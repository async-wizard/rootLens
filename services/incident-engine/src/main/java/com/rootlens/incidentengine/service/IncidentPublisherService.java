package com.rootlens.incidentengine.service;

import com.rootlens.incidentengine.dto.IncidentEvent;
import com.rootlens.incidentengine.entity.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IncidentPublisherService {

    private static final Logger log = LoggerFactory.getLogger(IncidentPublisherService.class);

    private final KafkaTemplate<String, IncidentEvent> kafkaTemplate;

    @Value("${rootlens.kafka.topics.incidents}")
    private String incidentsTopic;

    @Value("${rootlens.kafka.topics.alerts}")
    private String alertsTopic;

    public IncidentPublisherService(KafkaTemplate<String, IncidentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Incident incident) {
        IncidentEvent event = toEvent(incident);

        kafkaTemplate.send(incidentsTopic, incident.getId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish incident to {}: id={} error={}",
                                incidentsTopic, incident.getId(), ex.getMessage());
                    } else {
                        log.debug("Incident published: id={} partition={} offset={}",
                                incident.getId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

        if ("HIGH".equals(incident.getSeverity())) {
            kafkaTemplate.send(alertsTopic, incident.getId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish alert: id={} error={}",
                                    incident.getId(), ex.getMessage());
                        }
                    });
        }
    }

    private IncidentEvent toEvent(Incident incident) {
        IncidentEvent event = new IncidentEvent();
        event.setIncidentId(incident.getId());
        event.setStatus(incident.getStatus());
        event.setSeverity(incident.getSeverity());
        event.setServicesImpacted(incident.getServicesImpacted());
        event.setTraceIds(incident.getTraceIds());
        event.setCreatedAt(incident.getCreatedAt());
        event.setUpdatedAt(incident.getUpdatedAt());
        return event;
    }
}
