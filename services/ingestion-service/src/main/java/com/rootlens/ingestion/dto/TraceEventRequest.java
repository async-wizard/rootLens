package com.rootlens.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TraceEventRequest {

    @NotBlank(message = "service is required")
    private String service;

    @NotBlank(message = "traceId is required")
    private String traceId;

    private String spanId;
    private String parentSpanId;
    private String operationName;
    private Long durationMs;

    @NotNull(message = "timestamp is required")
    private Long timestamp;

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }

    public String getParentSpanId() { return parentSpanId; }
    public void setParentSpanId(String parentSpanId) { this.parentSpanId = parentSpanId; }

    public String getOperationName() { return operationName; }
    public void setOperationName(String operationName) { this.operationName = operationName; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
