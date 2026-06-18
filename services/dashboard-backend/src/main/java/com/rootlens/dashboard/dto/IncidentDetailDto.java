package com.rootlens.dashboard.dto;

import java.io.Serializable;
import java.util.List;

public class IncidentDetailDto implements Serializable {

    private String incidentId;
    private String status;
    private String severity;
    private List<String> servicesImpacted;
    private List<String> traceIds;
    private Long createdAt;
    private Long updatedAt;
    private List<IncidentEventDto> events;
    private AnalysisDto analysis;

    public IncidentDetailDto() {}

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

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

    public List<IncidentEventDto> getEvents() { return events; }
    public void setEvents(List<IncidentEventDto> events) { this.events = events; }

    public AnalysisDto getAnalysis() { return analysis; }
    public void setAnalysis(AnalysisDto analysis) { this.analysis = analysis; }
}
