package com.rootlens.dashboard.dto;

import java.io.Serializable;
import java.util.List;

public class IncidentSummaryDto implements Serializable {

    private String incidentId;
    private String status;
    private String severity;
    private List<String> servicesImpacted;
    private Long createdAt;
    private Long updatedAt;
    private boolean hasAnalysis;

    public IncidentSummaryDto() {}

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public List<String> getServicesImpacted() { return servicesImpacted; }
    public void setServicesImpacted(List<String> servicesImpacted) { this.servicesImpacted = servicesImpacted; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isHasAnalysis() { return hasAnalysis; }
    public void setHasAnalysis(boolean hasAnalysis) { this.hasAnalysis = hasAnalysis; }
}
