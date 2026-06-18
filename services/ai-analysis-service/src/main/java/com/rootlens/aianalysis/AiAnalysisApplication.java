package com.rootlens.aianalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class AiAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAnalysisApplication.class, args);
    }
}
