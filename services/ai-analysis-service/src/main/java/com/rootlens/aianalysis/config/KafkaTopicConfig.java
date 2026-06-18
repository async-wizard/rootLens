package com.rootlens.aianalysis.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${rootlens.kafka.topics.ai-analysis}")
    private String aiAnalysisTopic;

    @Bean
    public NewTopic aiAnalysisTopic() {
        return TopicBuilder.name(aiAnalysisTopic).partitions(3).replicas(1).build();
    }
}
