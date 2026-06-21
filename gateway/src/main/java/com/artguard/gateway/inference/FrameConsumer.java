package com.artguard.gateway.inference;

import com.artguard.gateway.config.ArtGuardProperties;
import com.artguard.gateway.incident.IncidentService;
import com.artguard.inference.grpc.Detection;
import com.artguard.inference.grpc.DetectResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes frames from Kafka and fans each one out to the YOLOv8 inference
 * service on its own <b>virtual thread</b> — so thousands of frames can be
 * in-flight concurrently without a thread-per-frame platform-thread cost. Any
 * alerting detection is handed to the incident layer.
 */
@Component
public class FrameConsumer {

    private static final Logger log = LoggerFactory.getLogger(FrameConsumer.class);

    private final InferenceClient inference;
    private final IncidentService incidents;
    private final Set<String> alertLabels;
    private final float threshold;
    private final ExecutorService vthreads = Executors.newVirtualThreadPerTaskExecutor();

    public FrameConsumer(InferenceClient inference, IncidentService incidents, ArtGuardProperties props) {
        this.inference = inference;
        this.incidents = incidents;
        this.alertLabels = Set.copyOf(props.alerting().alertLabels());
        this.threshold = props.alerting().threshold();
    }

    @KafkaListener(topics = "${artguard.kafka.frames-topic}")
    public void onFrame(ConsumerRecord<String, byte[]> rec) {
        String cameraId = rec.key();
        long frameId    = headerLong(rec, "frameId", 0);
        long captureTs  = headerLong(rec, "captureTs", System.currentTimeMillis());
        String camName  = headerStr(rec, "cameraName", cameraId);
        byte[] jpeg     = rec.value();

        // One virtual thread per frame: blocking gRPC call parks cheaply.
        vthreads.submit(() -> {
            try {
                DetectResponse resp = inference.detect(cameraId, frameId, captureTs, jpeg);
                for (Detection d : resp.getDetectionsList()) {
                    if (d.getConfidence() >= threshold && alertLabels.contains(d.getLabel())) {
                        incidents.onAlertingDetection(
                                cameraId, camName, d.getLabel(), d.getConfidence(), frameId, captureTs);
                    }
                }
            } catch (Exception e) {
                log.debug("inference failed for {}#{}: {}", cameraId, frameId, e.getMessage());
            }
        });
    }

    private static long headerLong(ConsumerRecord<?, ?> rec, String key, long dflt) {
        Header h = rec.headers().lastHeader(key);
        if (h == null) return dflt;
        try { return Long.parseLong(new String(h.value(), StandardCharsets.UTF_8)); }
        catch (NumberFormatException e) { return dflt; }
    }

    private static String headerStr(ConsumerRecord<?, ?> rec, String key, String dflt) {
        Header h = rec.headers().lastHeader(key);
        return h == null ? dflt : new String(h.value(), StandardCharsets.UTF_8);
    }
}
