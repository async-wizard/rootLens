package com.rootlens.incidentengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "incident_events")
public class IncidentEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, length = 50)
    private String incidentId;

    @Column(name = "service", nullable = false, length = 100)
    private String service;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "original_timestamp")
    private Long originalTimestamp;

    @Column(name = "received_at", nullable = false)
    private Long receivedAt;

    public IncidentEventRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

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
