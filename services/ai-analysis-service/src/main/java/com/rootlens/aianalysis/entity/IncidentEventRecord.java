package com.rootlens.aianalysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "incident_events")
public class IncidentEventRecord {

    @Id
    private Long id;

    @Column(name = "incident_id")
    private String incidentId;

    @Column(name = "service")
    private String service;

    @Column(name = "severity")
    private String severity;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "original_timestamp")
    private Long originalTimestamp;

    @Column(name = "received_at")
    private Long receivedAt;

    public Long getId() { return id; }
    public String getIncidentId() { return incidentId; }
    public String getService() { return service; }
    public String getSeverity() { return severity; }
    public String getTraceId() { return traceId; }
    public String getMessage() { return message; }
    public Long getOriginalTimestamp() { return originalTimestamp; }
    public Long getReceivedAt() { return receivedAt; }
}
