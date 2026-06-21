package com.artguard.gateway.kafka;

import com.artguard.gateway.config.ArtGuardProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declares the frames topic so it's auto-created on startup. */
@Configuration
public class KafkaTopics {

    @Bean
    public NewTopic framesTopic(ArtGuardProperties props) {
        // Partitions = parallelism for the inference consumers. Short retention:
        // frames are a live signal, not a log to keep.
        return TopicBuilder.name(props.kafka().framesTopic())
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "60000")
                .config("max.message.bytes", "10485760")
                .build();
    }
}
