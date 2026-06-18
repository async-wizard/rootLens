package com.rootlens.incidentengine.model;

public class EnrichedEvent {

    private String eventType;
    private String service;
    private String severity;
    private String traceId;
    private String message;
    private Long originalTimestamp;
    private Long ingestionTimestamp;
    private String host;
    private String ingestionSource;
    private String topic;

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

    public Long getOriginalTimestamp() { return originalTimestamp; }
    public void setOriginalTimestamp(Long originalTimestamp) { this.originalTimestamp = originalTimestamp; }

    public Long getIngestionTimestamp() { return ingestionTimestamp; }
    public void setIngestionTimestamp(Long ingestionTimestamp) { this.ingestionTimestamp = ingestionTimestamp; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getIngestionSource() { return ingestionSource; }
    public void setIngestionSource(String ingestionSource) { this.ingestionSource = ingestionSource; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}
