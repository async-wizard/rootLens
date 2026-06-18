package com.rootlens.aianalysis.listener;

import com.rootlens.aianalysis.model.IncidentEvent;
import com.rootlens.aianalysis.service.AiAnalysisOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentEventListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventListener.class);

    private final AiAnalysisOrchestrator orchestrator;

    public IncidentEventListener(AiAnalysisOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
        topics = "${rootlens.kafka.topics.incidents}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onIncident(IncidentEvent event) {
        log.debug("Received incident event: id={} severity={}", event.getIncidentId(), event.getSeverity());
        orchestrator.analyze(event);
    }
}
