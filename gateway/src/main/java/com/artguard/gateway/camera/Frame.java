package com.artguard.gateway.camera;

/**
 * A single captured video frame on its way through the pipeline.
 *
 * @param cameraId    source camera
 * @param frameId     monotonic per-camera counter
 * @param captureTsMs epoch millis at capture (used for end-to-end latency)
 * @param jpeg        JPEG-encoded bytes
 */
public record Frame(String cameraId, long frameId, long captureTsMs, byte[] jpeg) {}
