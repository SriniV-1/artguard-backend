package com.artguard.gateway.camera;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * A camera that needs no hardware: it loops over the JPEG sample images bundled
 * at {@code classpath:/samples/} at a fixed FPS, so the rest of the pipeline
 * (Kafka → YOLOv8 → incidents → WebSocket) runs end-to-end on any machine.
 * Because the samples are real photos, YOLOv8 returns real detections.
 */
public class SimulatedFrameSource implements FrameSource {

    private static final Logger log = LoggerFactory.getLogger(SimulatedFrameSource.class);
    private static final int FPS = 5;

    private final String cameraId;
    private final String cameraName;
    private final List<byte[]> samples = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public SimulatedFrameSource(String cameraId, String cameraName) {
        this.cameraId = cameraId;
        this.cameraName = cameraName;
        loadSamples();
    }

    private void loadSamples() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] found = resolver.getResources("classpath:/samples/*.jpg");
            for (Resource r : found) {
                try (InputStream in = r.getInputStream()) {
                    samples.add(in.readAllBytes());
                }
            }
        } catch (IOException e) {
            log.warn("No sample frames found for {} ({})", cameraId, e.getMessage());
        }
        if (samples.isEmpty()) {
            // 1x1 black JPEG fallback so the source still emits frames.
            samples.add(BLACK_JPEG);
        }
    }

    @Override public String cameraId()   { return cameraId; }
    @Override public String cameraName() { return cameraName; }

    @Override
    public void start(Consumer<Frame> sink) {
        if (!running.compareAndSet(false, true)) return;
        thread = Thread.ofVirtual().name("cam-" + cameraId).start(() -> {
            long frameId = 0;
            int idx = 0;
            final long periodMs = 1000L / FPS;
            while (running.get()) {
                byte[] jpeg = samples.get(idx % samples.size());
                idx++;
                sink.accept(new Frame(cameraId, frameId++, System.currentTimeMillis(), jpeg));
                try {
                    Thread.sleep(periodMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        log.info("Simulated camera {} started at {} fps ({} sample frames)", cameraId, FPS, samples.size());
    }

    @Override
    public void stop() {
        running.set(false);
        if (thread != null) thread.interrupt();
    }

    // Minimal valid 1x1 JPEG (used only if no sample images are bundled).
    private static final byte[] BLACK_JPEG = java.util.Base64.getDecoder().decode(
        "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAP//////////////////////////////////////////"
      + "////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBAB"
      + "AAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=");
}
