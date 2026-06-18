package com.rootlens.incidentengine.listener;

import com.rootlens.incidentengine.model.EnrichedEvent;
import com.rootlens.incidentengine.service.CorrelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EnrichedEventListener {

    private static final Logger log = LoggerFactory.getLogger(EnrichedEventListener.class);

    private final CorrelationService correlationService;

    public EnrichedEventListener(CorrelationService correlationService) {
        this.correlationService = correlationService;
    }

    @KafkaListener(
        topics = {
            "${rootlens.kafka.topics.logs}",
            "${rootlens.kafka.topics.traces}",
            "${rootlens.kafka.topics.metrics}"
        },
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onEvent(EnrichedEvent event) {
        log.debug("Received event: service={} severity={} traceId={}",
                event.getService(), event.getSeverity(), event.getTraceId());
        correlationService.process(event);
    }
}
