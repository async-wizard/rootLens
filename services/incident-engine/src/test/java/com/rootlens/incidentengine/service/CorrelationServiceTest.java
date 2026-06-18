package com.rootlens.incidentengine.service;

import com.rootlens.incidentengine.entity.Incident;
import com.rootlens.incidentengine.entity.IncidentEventRecord;
import com.rootlens.incidentengine.model.EnrichedEvent;
import com.rootlens.incidentengine.repository.IncidentEventRecordRepository;
import com.rootlens.incidentengine.repository.IncidentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationServiceTest {

    @Mock DeduplicationService deduplicationService;
    @Mock FingerprintService fingerprintService;
    @Mock IncidentRepository incidentRepository;
    @Mock IncidentEventRecordRepository incidentEventRecordRepository;
    @Mock IncidentPublisherService incidentPublisherService;
    @Mock WebhookNotificationService webhookNotificationService;

    CorrelationService correlationService;

    @BeforeEach
    void setUp() {
        correlationService = new CorrelationService(
                deduplicationService,
                fingerprintService,
                incidentRepository,
                incidentEventRecordRepository,
                incidentPublisherService,
                webhookNotificationService,
                new SimpleMeterRegistry()
        );
    }

    private EnrichedEvent errorEvent(String service, String traceId) {
        EnrichedEvent e = new EnrichedEvent();
        e.setSeverity("ERROR");
        e.setService(service);
        e.setTraceId(traceId);
        e.setMessage("Something failed");
        e.setOriginalTimestamp(System.currentTimeMillis());
        return e;
    }

    @Test
    void nonErrorSeverity_isDropped() {
        EnrichedEvent e = errorEvent("checkout", "trace-abc");
        e.setSeverity("INFO");

        correlationService.process(e);

        verifyNoInteractions(deduplicationService, incidentRepository, incidentPublisherService);
    }

    @Test
    void newTraceId_createsIncidentAndPublishes() {
        when(fingerprintService.fingerprint(any())).thenReturn("fp-123");
        when(deduplicationService.getTraceIncidentId("trace-abc")).thenReturn(Optional.empty());
        when(deduplicationService.getDedupIncidentId("payment", "fp-123")).thenReturn(Optional.empty());
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        correlationService.process(errorEvent("payment", "trace-abc"));

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("OPEN");
        assertThat(captor.getValue().getSeverity()).isEqualTo("MEDIUM");
        assertThat(captor.getValue().getServicesImpacted()).contains("payment");
        assertThat(captor.getValue().getTraceIds()).contains("trace-abc");

        verify(incidentPublisherService).publish(any());
        verify(deduplicationService).storeTrace(eq("trace-abc"), anyString());
        verify(deduplicationService).storeDedup(eq("payment"), eq("fp-123"), anyString());
        verify(incidentEventRecordRepository).save(any());
    }

    @Test
    void knownTraceId_attachesToExistingIncident() {
        Incident existing = new Incident("INCIDENT-aaa", "OPEN", "MEDIUM", System.currentTimeMillis());
        existing.setServicesImpacted(new ArrayList<>(List.of("payment")));
        existing.setTraceIds(new ArrayList<>(List.of("trace-abc")));

        when(deduplicationService.getTraceIncidentId("trace-abc")).thenReturn(Optional.of("INCIDENT-aaa"));
        when(incidentRepository.findById("INCIDENT-aaa")).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        correlationService.process(errorEvent("checkout", "trace-abc"));

        ArgumentCaptor<IncidentEventRecord> recordCaptor = ArgumentCaptor.forClass(IncidentEventRecord.class);
        verify(incidentEventRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getService()).isEqualTo("checkout");
    }

    @Test
    void knownTraceId_escalatesToHighWithTwoServices() {
        Incident existing = new Incident("INCIDENT-aaa", "OPEN", "MEDIUM", System.currentTimeMillis());
        existing.setServicesImpacted(new ArrayList<>(List.of("payment")));
        existing.setTraceIds(new ArrayList<>());

        when(deduplicationService.getTraceIncidentId("trace-abc")).thenReturn(Optional.of("INCIDENT-aaa"));
        when(incidentRepository.findById("INCIDENT-aaa")).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        correlationService.process(errorEvent("checkout", "trace-abc"));

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo("HIGH");
        assertThat(captor.getValue().getServicesImpacted()).containsExactlyInAnyOrder("payment", "checkout");
    }

    @Test
    void dedupMatch_suppressesDuplicate() {
        when(fingerprintService.fingerprint(any())).thenReturn("fp-dup");
        when(deduplicationService.getTraceIncidentId(any())).thenReturn(Optional.empty());
        when(deduplicationService.getDedupIncidentId("payment", "fp-dup")).thenReturn(Optional.of("INCIDENT-existing"));

        correlationService.process(errorEvent("payment", "trace-abc"));

        verify(incidentRepository, never()).save(any());
        verify(incidentPublisherService, never()).publish(any());
    }

    @Test
    void sameServiceOnExistingIncident_noReSave() {
        Incident existing = new Incident("INCIDENT-aaa", "OPEN", "MEDIUM", System.currentTimeMillis());
        existing.setServicesImpacted(new ArrayList<>(List.of("checkout")));
        existing.setTraceIds(new ArrayList<>());

        when(deduplicationService.getTraceIncidentId("trace-abc")).thenReturn(Optional.of("INCIDENT-aaa"));
        when(incidentRepository.findById("INCIDENT-aaa")).thenReturn(Optional.of(existing));

        correlationService.process(errorEvent("checkout", "trace-abc"));

        verify(incidentRepository, never()).save(any());
        verify(incidentPublisherService, never()).publish(any());
        verify(incidentEventRecordRepository).save(any());
    }
}
