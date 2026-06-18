package com.rootlens.dashboard.entity;

import com.rootlens.dashboard.converter.StringListConverter;
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

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "services_impacted", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> servicesImpacted = new ArrayList<>();

    @Column(name = "trace_ids", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> traceIds = new ArrayList<>();

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getSeverity() { return severity; }
    public List<String> getServicesImpacted() { return servicesImpacted; }
    public List<String> getTraceIds() { return traceIds; }
    public Long getCreatedAt() { return createdAt; }
    public Long getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
