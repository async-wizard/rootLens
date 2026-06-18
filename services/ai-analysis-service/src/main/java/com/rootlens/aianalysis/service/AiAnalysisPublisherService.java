package com.rootlens.aianalysis.service;

import com.rootlens.aianalysis.dto.AiAnalysisEvent;
import com.rootlens.aianalysis.entity.AiAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiAnalysisPublisherService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisPublisherService.class);

    private final KafkaTemplate<String, AiAnalysisEvent> kafkaTemplate;

    @Value("${rootlens.kafka.topics.ai-analysis}")
    private String aiAnalysisTopic;

    public AiAnalysisPublisherService(KafkaTemplate<String, AiAnalysisEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(AiAnalysis analysis) {
        AiAnalysisEvent event = toEvent(analysis);

        kafkaTemplate.send(aiAnalysisTopic, analysis.getIncidentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish AI analysis: incidentId={} error={}",
                                analysis.getIncidentId(), ex.getMessage());
                    } else {
                        log.debug("AI analysis published: incidentId={} partition={} offset={}",
                                analysis.getIncidentId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    private AiAnalysisEvent toEvent(AiAnalysis analysis) {
        AiAnalysisEvent event = new AiAnalysisEvent();
        event.setIncidentId(analysis.getIncidentId());
        event.setSummary(analysis.getSummary());
        event.setProbableCause(analysis.getProbableCause());
        event.setRemediation(analysis.getRemediation());
        event.setModel(analysis.getModel());
        event.setAnalysisTimestamp(analysis.getAnalysisTimestamp());
        event.setSuccess(analysis.isSuccess());
        return event;
    }
}
