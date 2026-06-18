package com.rootlens.ingestion.service;

import com.rootlens.ingestion.dto.GenericEventRequest;
import com.rootlens.ingestion.dto.LogEventRequest;
import com.rootlens.ingestion.dto.MetricEventRequest;
import com.rootlens.ingestion.dto.TraceEventRequest;
import com.rootlens.ingestion.model.EnrichedEvent;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final KafkaTemplate<String, EnrichedEvent> kafkaTemplate;
    private final RateLimiter rateLimiter;
    private final Counter logsIngested;
    private final Counter tracesIngested;
    private final Counter metricsIngested;
    private final Counter eventsIngested;
    private String hostname;

    @Value("${rootlens.kafka.topics.logs}")
    private String logsTopic;

    @Value("${rootlens.kafka.topics.traces}")
    private String tracesTopic;

    @Value("${rootlens.kafka.topics.metrics}")
    private String metricsTopic;

    @Value("${rootlens.kafka.topics.events}")
    private String eventsTopic;

    public IngestionService(KafkaTemplate<String, EnrichedEvent> kafkaTemplate,
                            RateLimiterRegistry rateLimiterRegistry,
                            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.rateLimiter = rateLimiterRegistry.rateLimiter("ingestion");

        this.logsIngested    = counter(meterRegistry, "log");
        this.tracesIngested  = counter(meterRegistry, "trace");
        this.metricsIngested = counter(meterRegistry, "metric");
        this.eventsIngested  = counter(meterRegistry, "event");
    }

    private Counter counter(MeterRegistry registry, String type) {
        return Counter.builder("rootlens.events.ingested")
                .description("Events accepted for ingestion")
                .tag("type", type)
                .register(registry);
    }

    @PostConstruct
    public void init() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "unknown-host";
        }
    }

    public void ingestLog(LogEventRequest request) {
        rateLimiter.acquirePermission();
        EnrichedEvent event = new EnrichedEvent();
        event.setEventType("LOG_EVENT");
        event.setService(request.getService());
        event.setSeverity(request.getSeverity());
        event.setTraceId(request.getTraceId());
        event.setMessage(request.getMessage());
        event.setOriginalTimestamp(request.getTimestamp());
        event.setIngestionTimestamp(System.currentTimeMillis());
        event.setHost(hostname);
        event.setIngestionSource("http-api");
        event.setTopic(logsTopic);
        publish(logsTopic, request.getService(), event);
        logsIngested.increment();
    }

    public void ingestTrace(TraceEventRequest request) {
        rateLimiter.acquirePermission();
        EnrichedEvent event = new EnrichedEvent();
        event.setEventType("TRACE_EVENT");
        event.setService(request.getService());
        event.setTraceId(request.getTraceId());
        event.setMessage(request.getOperationName() != null ? request.getOperationName() : "trace-span");
        event.setOriginalTimestamp(request.getTimestamp());
        event.setIngestionTimestamp(System.currentTimeMillis());
        event.setHost(hostname);
        event.setIngestionSource("http-api");
        event.setTopic(tracesTopic);
        publish(tracesTopic, request.getService(), event);
        tracesIngested.increment();
    }

    public void ingestMetric(MetricEventRequest request) {
        rateLimiter.acquirePermission();
        EnrichedEvent event = new EnrichedEvent();
        event.setEventType("METRIC_EVENT");
        event.setService(request.getService());
        event.setMessage(request.getMetricName() + "=" + request.getValue());
        event.setOriginalTimestamp(request.getTimestamp());
        event.setIngestionTimestamp(System.currentTimeMillis());
        event.setHost(hostname);
        event.setIngestionSource("http-api");
        event.setTopic(metricsTopic);
        publish(metricsTopic, request.getService(), event);
        metricsIngested.increment();
    }

    public void ingestEvent(GenericEventRequest request) {
        rateLimiter.acquirePermission();
        EnrichedEvent event = new EnrichedEvent();
        event.setEventType(request.getEventType());
        event.setService(request.getService());
        event.setMessage(request.getEventType());
        event.setIngestionTimestamp(System.currentTimeMillis());
        event.setHost(hostname);
        event.setIngestionSource("http-api");
        event.setTopic(eventsTopic);
        publish(eventsTopic, request.getService(), event);
        eventsIngested.increment();
    }

    private void publish(String topic, String partitionKey, EnrichedEvent event) {
        kafkaTemplate.send(topic, partitionKey, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to topic={} key={}: {}", topic, partitionKey, ex.getMessage());
                    } else {
                        log.debug("Published to topic={} partition={} offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
