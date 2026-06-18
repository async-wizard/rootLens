package com.rootlens.ingestion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${rootlens.kafka.topics.logs}")
    private String logsTopic;

    @Value("${rootlens.kafka.topics.traces}")
    private String tracesTopic;

    @Value("${rootlens.kafka.topics.metrics}")
    private String metricsTopic;

    @Value("${rootlens.kafka.topics.events}")
    private String eventsTopic;

    @Bean
    public NewTopic logsTopic() {
        return TopicBuilder.name(logsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tracesTopic() {
        return TopicBuilder.name(tracesTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic metricsTopic() {
        return TopicBuilder.name(metricsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name(eventsTopic).partitions(3).replicas(1).build();
    }
}
