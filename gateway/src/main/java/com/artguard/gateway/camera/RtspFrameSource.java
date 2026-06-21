package com.artguard.gateway.camera;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A real RTSP camera source backed by FFmpeg (via JavaCV). Connects to an
 * {@code rtsp://} URL, decodes frames, re-encodes each as JPEG, and pushes it
 * downstream. Reconnects with backoff if the stream drops.
 */
public class RtspFrameSource implements FrameSource {

    private static final Logger log = LoggerFactory.getLogger(RtspFrameSource.class);

    private final String cameraId;
    private final String cameraName;
    private final String url;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public RtspFrameSource(String cameraId, String cameraName, String url) {
        this.cameraId = cameraId;
        this.cameraName = cameraName;
        this.url = url;
    }

    @Override public String cameraId()   { return cameraId; }
    @Override public String cameraName() { return cameraName; }

    @Override
    public void start(Consumer<Frame> sink) {
        if (!running.compareAndSet(false, true)) return;
        thread = Thread.ofVirtual().name("rtsp-" + cameraId).start(() -> runLoop(sink));
        log.info("RTSP camera {} connecting to {}", cameraId, url);
    }

    private void runLoop(Consumer<Frame> sink) {
        var converter = new Java2DFrameConverter();
        long frameId = 0;
        while (running.get()) {
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(url)) {
                grabber.setOption("rtsp_transport", "tcp"); // reliable over TCP
                grabber.setOption("stimeout", "5000000");   // 5s connect timeout (µs)
                grabber.start();
                org.bytedeco.javacv.Frame img;
                while (running.get() && (img = grabber.grabImage()) != null) {
                    var buffered = converter.getBufferedImage(img);
                    if (buffered == null) continue;
                    var baos = new ByteArrayOutputStream();
                    ImageIO.write(buffered, "jpg", baos);
                    sink.accept(new Frame(cameraId, frameId++, System.currentTimeMillis(), baos.toByteArray()));
                }
            } catch (Exception e) {
                if (!running.get()) break;
                log.warn("RTSP {} error ({}); reconnecting in 2s", cameraId, e.getMessage());
                sleep(2000);
            }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Override
    public void stop() {
        running.set(false);
        if (thread != null) thread.interrupt();
    }
}
