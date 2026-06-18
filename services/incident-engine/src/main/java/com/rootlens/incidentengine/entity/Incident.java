package com.rootlens.incidentengine.entity;

import com.rootlens.incidentengine.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "services_impacted", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> servicesImpacted = new ArrayList<>();

    @Column(name = "trace_ids", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> traceIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    public Incident() {}

    public Incident(String id, String status, String severity, Long now) {
        this.id = id;
        this.status = status;
        this.severity = severity;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public List<String> getServicesImpacted() { return servicesImpacted; }
    public void setServicesImpacted(List<String> servicesImpacted) { this.servicesImpacted = servicesImpacted; }

    public List<String> getTraceIds() { return traceIds; }
    public void setTraceIds(List<String> traceIds) { this.traceIds = traceIds; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
