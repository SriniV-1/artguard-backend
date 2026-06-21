package com.artguard.gateway.camera;

import java.util.function.Consumer;

/**
 * A running source of frames for one camera. Implementations push frames to the
 * supplied sink on their own thread until {@link #stop()} is called.
 */
public interface FrameSource extends AutoCloseable {

    String cameraId();

    String cameraName();

    /** Begin producing frames, delivering each to {@code sink}. Non-blocking. */
    void start(Consumer<Frame> sink);

    /** Stop producing and release any resources (camera handle, threads). */
    void stop();

    @Override
    default void close() { stop(); }
}
