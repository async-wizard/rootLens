package com.rootlens.dashboard.dto;

import java.io.Serializable;

public class IncidentEventDto implements Serializable {

    private String service;
    private String severity;
    private String traceId;
    private String message;
    private Long originalTimestamp;
    private Long receivedAt;

    public IncidentEventDto() {}

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getOriginalTimestamp() { return originalTimestamp; }
    public void setOriginalTimestamp(Long originalTimestamp) { this.originalTimestamp = originalTimestamp; }

    public Long getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Long receivedAt) { this.receivedAt = receivedAt; }
}
