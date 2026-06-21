package com.artguard.gateway.inference;

import com.artguard.gateway.config.ArtGuardProperties;
import com.artguard.inference.grpc.DetectRequest;
import com.artguard.inference.grpc.DetectResponse;
import com.artguard.inference.grpc.InferenceServiceGrpc;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** gRPC client to the Python/YOLOv8 inference service. */
@Component
public class InferenceClient {

    private static final Logger log = LoggerFactory.getLogger(InferenceClient.class);

    private final ManagedChannel channel;
    private final InferenceServiceGrpc.InferenceServiceBlockingStub stub;
    private final long deadlineMs;

    public InferenceClient(ArtGuardProperties props) {
        var cfg = props.inference();
        this.channel = NettyChannelBuilder.forAddress(cfg.host(), cfg.port())
                .usePlaintext()
                .maxInboundMessageSize(16 * 1024 * 1024)
                .build();
        this.stub = InferenceServiceGrpc.newBlockingStub(channel);
        this.deadlineMs = cfg.deadlineMs();
        log.info("Inference gRPC client -> {}:{}", cfg.host(), cfg.port());
    }

    /**
     * Detect objects in one frame. Blocking — intended to be called on a virtual
     * thread, so blocking here parks the carrier-free virtual thread cheaply.
     */
    public DetectResponse detect(String cameraId, long frameId, long captureTsMs, byte[] jpeg) {
        DetectRequest req = DetectRequest.newBuilder()
                .setCameraId(cameraId)
                .setFrameId(frameId)
                .setCaptureTsMs(captureTsMs)
                .setJpeg(ByteString.copyFrom(jpeg))
                .build();
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).detect(req);
    }

    @PreDestroy
    void shutdown() {
        channel.shutdown();
        try {
            channel.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
