package com.rootlens.incidentengine.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${rootlens.kafka.topics.incidents}")
    private String incidentsTopic;

    @Value("${rootlens.kafka.topics.alerts}")
    private String alertsTopic;

    @Bean
    public NewTopic incidentsTopic() {
        return TopicBuilder.name(incidentsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic alertsTopic() {
        return TopicBuilder.name(alertsTopic).partitions(3).replicas(1).build();
    }
}
