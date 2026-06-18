package com.rootlens.ingestion.service;

import com.rootlens.ingestion.dto.LogEventRequest;
import com.rootlens.ingestion.dto.MetricEventRequest;
import com.rootlens.ingestion.model.EnrichedEvent;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    @SuppressWarnings("unchecked")
    KafkaTemplate<String, EnrichedEvent> kafkaTemplate;

    IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(
                RateLimiterConfig.custom()
                        .limitForPeriod(Integer.MAX_VALUE)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ZERO)
                        .build()
        );

        ingestionService = new IngestionService(kafkaTemplate, rateLimiterRegistry, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(ingestionService, "logsTopic", "logs-topic");
        ReflectionTestUtils.setField(ingestionService, "tracesTopic", "traces-topic");
        ReflectionTestUtils.setField(ingestionService, "metricsTopic", "metrics-topic");
        ReflectionTestUtils.setField(ingestionService, "eventsTopic", "events-topic");
        ingestionService.init();

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void ingestLog_publishesToLogsTopic() {
        ingestionService.ingestLog(logRequest("checkout", "trace-123"));

        verify(kafkaTemplate).send(eq("logs-topic"), eq("checkout"), any(EnrichedEvent.class));
    }

    @Test
    void ingestLog_setsEnrichmentFields() {
        ArgumentCaptor<EnrichedEvent> captor = ArgumentCaptor.forClass(EnrichedEvent.class);
        when(kafkaTemplate.send(any(), any(), captor.capture()))
                .thenReturn(CompletableFuture.completedFuture(null));

        ingestionService.ingestLog(logRequest("payment", "trace-abc"));

        EnrichedEvent published = captor.getValue();
        assertThat(published.getEventType()).isEqualTo("LOG_EVENT");
        assertThat(published.getService()).isEqualTo("payment");
        assertThat(published.getTraceId()).isEqualTo("trace-abc");
        assertThat(published.getIngestionTimestamp()).isGreaterThan(0);
        assertThat(published.getHost()).isNotBlank();
        assertThat(published.getIngestionSource()).isEqualTo("http-api");
    }

    @Test
    void ingestMetric_publishesToMetricsTopic() {
        MetricEventRequest req = new MetricEventRequest();
        req.setService("payment");
        req.setMetricName("db.query.time");
        req.setValue(1500.0);
        req.setTimestamp(System.currentTimeMillis());

        ingestionService.ingestMetric(req);

        verify(kafkaTemplate).send(eq("metrics-topic"), eq("payment"), any(EnrichedEvent.class));
    }

    private LogEventRequest logRequest(String service, String traceId) {
        LogEventRequest req = new LogEventRequest();
        req.setService(service);
        req.setSeverity("ERROR");
        req.setTraceId(traceId);
        req.setMessage("Something went wrong");
        req.setTimestamp(System.currentTimeMillis());
        return req;
    }
}
