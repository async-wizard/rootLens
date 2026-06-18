package com.rootlens.incidentengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class IncidentEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentEngineApplication.class, args);
    }
}
