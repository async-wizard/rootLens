package com.rootlens.aianalysis.service;

import com.rootlens.aianalysis.entity.AiAnalysis;
import com.rootlens.aianalysis.entity.IncidentEventRecord;
import com.rootlens.aianalysis.model.IncidentEvent;
import com.rootlens.aianalysis.repository.AiAnalysisRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAnalysisOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisOrchestrator.class);

    private final AiAnalysisRepository aiAnalysisRepository;
    private final ContextAggregationService contextAggregationService;
    private final PromptBuilderService promptBuilderService;
    private final LlmAnalysisProvider llmProvider;
    private final AiAnalysisPublisherService publisherService;

    private final CircuitBreaker circuitBreaker;
    private final Timer analysisTimer;
    private final Counter analysisSuccess;
    private final Counter analysisFailure;

    public AiAnalysisOrchestrator(
            AiAnalysisRepository aiAnalysisRepository,
            ContextAggregationService contextAggregationService,
            PromptBuilderService promptBuilderService,
            LlmAnalysisProvider llmProvider,
            AiAnalysisPublisherService publisherService,
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry) {
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.contextAggregationService = contextAggregationService;
        this.promptBuilderService = promptBuilderService;
        this.llmProvider = llmProvider;
        this.publisherService = publisherService;

        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("llm-provider");

        this.analysisTimer = Timer.builder("rootlens.ai.analysis.duration")
                .description("End-to-end AI analysis latency")
                .tag("provider", llmProvider.getProviderName())
                .register(meterRegistry);
        this.analysisSuccess = Counter.builder("rootlens.ai.analysis.result")
                .tag("success", "true").register(meterRegistry);
        this.analysisFailure = Counter.builder("rootlens.ai.analysis.result")
                .tag("success", "false").register(meterRegistry);
    }

    public void analyze(IncidentEvent incident) {
        String incidentId = incident.getIncidentId();

        // Step 1: idempotency guard
        if (aiAnalysisRepository.findByIncidentId(incidentId).isPresent()) {
            log.info("Analysis already exists for incident {}, skipping", incidentId);
            return;
        }

        log.info("Starting AI analysis for incident={} severity={} provider={}",
                incidentId, incident.getSeverity(), llmProvider.getProviderName());

        // Step 2: aggregate context from incident_events table
        List<IncidentEventRecord> eventRecords = contextAggregationService.aggregate(incidentId);

        if (eventRecords.isEmpty()) {
            log.warn("No event records found for incident {}. Proceeding with incident metadata only.", incidentId);
        }

        // Step 3: build prompt
        String userMessage = promptBuilderService.buildUserMessage(incident, eventRecords);

        // Step 4: call LLM via circuit breaker, timed manually
        AnalysisResult result;
        long start = System.nanoTime();
        try {
            result = circuitBreaker.executeSupplier(
                    () -> llmProvider.analyze(PromptBuilderService.SYSTEM_PROMPT, userMessage));
        } catch (CallNotPermittedException e) {
            log.warn("LLM circuit breaker OPEN for incident={}: {}", incidentId, e.getMessage());
            result = AnalysisResult.failed("Circuit open: " + e.getMessage());
        } catch (Exception e) {
            log.error("LLM provider '{}' failed for incident={}: {}",
                    llmProvider.getProviderName(), incidentId, e.getMessage(), e);
            result = AnalysisResult.failed("Provider error: " + e.getMessage());
        } finally {
            analysisTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }

        if (result.isSuccess()) { analysisSuccess.increment(); } else { analysisFailure.increment(); }

        // Step 5: persist result
        AiAnalysis analysis = buildEntity(incidentId, result);
        aiAnalysisRepository.save(analysis);

        // Step 6: publish to ai-analysis-topic
        publisherService.publish(analysis);

        log.info("AI analysis complete: incidentId={} provider={} success={}",
                incidentId, llmProvider.getProviderName(), result.isSuccess());
    }

    private AiAnalysis buildEntity(String incidentId, AnalysisResult result) {
        AiAnalysis analysis = new AiAnalysis();
        analysis.setId("ANALYSIS-" + incidentId);
        analysis.setIncidentId(incidentId);
        analysis.setSummary(result.getSummary());
        analysis.setProbableCause(result.getProbableCause());
        analysis.setRemediation(result.getRemediation());
        analysis.setModel(llmProvider.getProviderName());
        analysis.setAnalysisTimestamp(System.currentTimeMillis());
        analysis.setSuccess(result.isSuccess());
        analysis.setRawResponse(result.getRawResponse());
        return analysis;
    }
}
