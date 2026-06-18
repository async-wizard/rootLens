package com.rootlens.aianalysis.service;

import com.rootlens.aianalysis.entity.AiAnalysis;
import com.rootlens.aianalysis.entity.IncidentEventRecord;
import com.rootlens.aianalysis.model.IncidentEvent;
import com.rootlens.aianalysis.repository.AiAnalysisRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAnalysisOrchestratorTest {

    @Mock AiAnalysisRepository aiAnalysisRepository;
    @Mock ContextAggregationService contextAggregationService;
    @Mock PromptBuilderService promptBuilderService;
    @Mock LlmAnalysisProvider llmProvider;
    @Mock AiAnalysisPublisherService publisherService;

    AiAnalysisOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        lenient().when(llmProvider.getProviderName()).thenReturn("test-llm");
        orchestrator = new AiAnalysisOrchestrator(
                aiAnalysisRepository,
                contextAggregationService,
                promptBuilderService,
                llmProvider,
                publisherService,
                CircuitBreakerRegistry.ofDefaults(),
                new SimpleMeterRegistry()
        );
    }

    private IncidentEvent incident() {
        IncidentEvent e = new IncidentEvent();
        e.setIncidentId("INCIDENT-abc");
        e.setSeverity("HIGH");
        e.setStatus("OPEN");
        e.setServicesImpacted(List.of("payment-service", "checkout-service"));
        e.setTraceIds(List.of("trace-123"));
        e.setCreatedAt(System.currentTimeMillis());
        return e;
    }

    @Test
    void analysisAlreadyExists_skipped() {
        when(aiAnalysisRepository.findByIncidentId("INCIDENT-abc"))
                .thenReturn(Optional.of(new AiAnalysis()));

        orchestrator.analyze(incident());

        verifyNoInteractions(contextAggregationService, publisherService);
        verify(llmProvider, never()).analyze(any(), any());
    }

    @Test
    void llmSuccess_persistsSuccessAndPublishes() {
        when(aiAnalysisRepository.findByIncidentId(any())).thenReturn(Optional.empty());
        when(contextAggregationService.aggregate(any())).thenReturn(List.of());
        when(promptBuilderService.buildUserMessage(any(), any())).thenReturn("user-prompt");
        when(llmProvider.analyze(any(), any()))
                .thenReturn(AnalysisResult.success("summary", "cause", "fix", "raw"));
        when(aiAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.analyze(incident());

        ArgumentCaptor<AiAnalysis> captor = ArgumentCaptor.forClass(AiAnalysis.class);
        verify(aiAnalysisRepository).save(captor.capture());
        assertThat(captor.getValue().isSuccess()).isTrue();
        assertThat(captor.getValue().getIncidentId()).isEqualTo("INCIDENT-abc");
        assertThat(captor.getValue().getSummary()).isEqualTo("summary");
        assertThat(captor.getValue().getProbableCause()).isEqualTo("cause");

        verify(publisherService).publish(any());
    }

    @Test
    void llmThrows_persistsFailedAnalysisWithoutPropagating() {
        when(aiAnalysisRepository.findByIncidentId(any())).thenReturn(Optional.empty());
        when(contextAggregationService.aggregate(any())).thenReturn(List.of());
        when(promptBuilderService.buildUserMessage(any(), any())).thenReturn("user-prompt");
        when(llmProvider.analyze(any(), any())).thenThrow(new RuntimeException("API key invalid"));
        when(aiAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> orchestrator.analyze(incident()));

        ArgumentCaptor<AiAnalysis> captor = ArgumentCaptor.forClass(AiAnalysis.class);
        verify(aiAnalysisRepository).save(captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getRawResponse()).contains("API key invalid");

        verify(publisherService).publish(any());
    }

    @Test
    void noEventRecords_proceedsWithMetadataOnly() {
        List<IncidentEventRecord> empty = Collections.emptyList();
        when(aiAnalysisRepository.findByIncidentId(any())).thenReturn(Optional.empty());
        when(contextAggregationService.aggregate(any())).thenReturn(empty);
        when(promptBuilderService.buildUserMessage(any(), eq(empty))).thenReturn("minimal-prompt");
        when(llmProvider.analyze(any(), any())).thenReturn(AnalysisResult.failed("no context"));
        when(aiAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.analyze(incident());

        verify(promptBuilderService).buildUserMessage(any(), eq(empty));
        verify(aiAnalysisRepository).save(any());
    }
}
