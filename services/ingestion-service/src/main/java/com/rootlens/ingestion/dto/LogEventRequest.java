package com.rootlens.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LogEventRequest {

    private String eventType;

    @NotBlank(message = "service is required")
    private String service;

    @NotBlank(message = "severity is required")
    private String severity;

    @NotBlank(message = "traceId is required")
    private String traceId;

    @NotBlank(message = "message is required")
    private String message;

    @NotNull(message = "timestamp is required")
    private Long timestamp;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
