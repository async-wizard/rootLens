package com.rootlens.incidentengine.service;

import com.rootlens.incidentengine.entity.Incident;
import com.rootlens.incidentengine.entity.IncidentEventRecord;
import com.rootlens.incidentengine.model.EnrichedEvent;
import com.rootlens.incidentengine.repository.IncidentEventRecordRepository;
import com.rootlens.incidentengine.repository.IncidentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CorrelationService {

    private static final Logger log = LoggerFactory.getLogger(CorrelationService.class);
    private static final String SEVERITY_ERROR = "ERROR";

    private final DeduplicationService deduplicationService;
    private final FingerprintService fingerprintService;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRecordRepository incidentEventRecordRepository;
    private final IncidentPublisherService incidentPublisherService;
    private final WebhookNotificationService webhookNotificationService;

    private final Counter incidentsCreated;
    private final Counter eventsCorrelated;
    private final Counter dedupSuppressed;

    public CorrelationService(
            DeduplicationService deduplicationService,
            FingerprintService fingerprintService,
            IncidentRepository incidentRepository,
            IncidentEventRecordRepository incidentEventRecordRepository,
            IncidentPublisherService incidentPublisherService,
            WebhookNotificationService webhookNotificationService,
            MeterRegistry meterRegistry) {
        this.deduplicationService = deduplicationService;
        this.fingerprintService = fingerprintService;
        this.incidentRepository = incidentRepository;
        this.incidentEventRecordRepository = incidentEventRecordRepository;
        this.incidentPublisherService = incidentPublisherService;
        this.webhookNotificationService = webhookNotificationService;

        this.incidentsCreated  = Counter.builder("rootlens.incidents.created")
                .description("Number of new incidents created").register(meterRegistry);
        this.eventsCorrelated  = Counter.builder("rootlens.events.correlated")
                .description("Events attached to existing incidents").register(meterRegistry);
        this.dedupSuppressed   = Counter.builder("rootlens.incidents.dedup.suppressed")
                .description("Duplicate events suppressed by dedup cache").register(meterRegistry);
    }

    @Transactional
    public void process(EnrichedEvent event) {
        // Gate: only ERROR events trigger incident logic
        if (!SEVERITY_ERROR.equalsIgnoreCase(event.getSeverity())) {
            log.trace("Ignoring non-ERROR event: service={} severity={}", event.getService(), event.getSeverity());
            return;
        }

        String service     = event.getService();
        String traceId     = event.getTraceId();
        String fingerprint = fingerprintService.fingerprint(event.getMessage());

        // Step 1: trace-based correlation — attach to existing incident on same trace
        if (isValidTraceId(traceId)) {
            Optional<String> existingByTrace = deduplicationService.getTraceIncidentId(traceId);
            if (existingByTrace.isPresent()) {
                log.debug("TraceId {} maps to incident {}. Attaching service {}.",
                        traceId, existingByTrace.get(), service);
                addServiceToIncident(existingByTrace.get(), service, event);
                return;
            }
        }

        // Step 2: dedup check — same service + same error within 5-min window
        Optional<String> existingByDedup = deduplicationService.getDedupIncidentId(service, fingerprint);
        if (existingByDedup.isPresent()) {
            log.debug("Duplicate suppressed: service={} fingerprint={} → incident={}",
                    service, fingerprint, existingByDedup.get());
            dedupSuppressed.increment();
            return;
        }

        // Step 3: create new incident
        String incidentId = generateIncidentId();
        long now = System.currentTimeMillis();

        Incident incident = new Incident(incidentId, "OPEN", "MEDIUM", now);
        incident.setServicesImpacted(new ArrayList<>(List.of(service)));
        if (isValidTraceId(traceId)) {
            incident.setTraceIds(new ArrayList<>(List.of(traceId)));
        }

        incidentRepository.save(incident);

        // Redis keys written after successful JPA save
        deduplicationService.storeDedup(service, fingerprint, incidentId);
        if (isValidTraceId(traceId)) {
            deduplicationService.storeTrace(traceId, incidentId);
        }

        persistEventRecord(incidentId, event);
        incidentPublisherService.publish(incident);
        webhookNotificationService.notify(incident);
        incidentsCreated.increment();

        log.info("New incident created: id={} service={} traceId={}", incidentId, service, traceId);
    }

    private void addServiceToIncident(String incidentId, String service, EnrichedEvent event) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Incident " + incidentId + " found in Redis but missing from PostgreSQL"));

        boolean alreadyImpacted = incident.getServicesImpacted().contains(service);
        if (!alreadyImpacted) {
            incident.getServicesImpacted().add(service);

            if (incident.getServicesImpacted().size() >= 2) {
                incident.setSeverity("HIGH");
            }

            incident.setUpdatedAt(System.currentTimeMillis());
            incidentRepository.save(incident);
            incidentPublisherService.publish(incident);
            if ("HIGH".equals(incident.getSeverity())) {
                webhookNotificationService.notify(incident);
            }

            log.info("Incident {} updated: services={} severity={}",
                    incidentId, incident.getServicesImpacted(), incident.getSeverity());
        }

        eventsCorrelated.increment();
        persistEventRecord(incidentId, event);
    }

    private void persistEventRecord(String incidentId, EnrichedEvent event) {
        IncidentEventRecord record = new IncidentEventRecord();
        record.setIncidentId(incidentId);
        record.setService(event.getService());
        record.setSeverity(event.getSeverity());
        record.setTraceId(event.getTraceId());
        record.setMessage(event.getMessage());
        record.setOriginalTimestamp(event.getOriginalTimestamp());
        record.setReceivedAt(System.currentTimeMillis());
        incidentEventRecordRepository.save(record);
    }

    private boolean isValidTraceId(String traceId) {
        return traceId != null && !traceId.isBlank() && !"unknown".equals(traceId);
    }

    private String generateIncidentId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "INCIDENT-" + uuid.substring(0, 6);
    }
}
