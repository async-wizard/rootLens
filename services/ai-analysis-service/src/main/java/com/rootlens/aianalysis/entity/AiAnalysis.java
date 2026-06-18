package com.rootlens.aianalysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_analysis")
public class AiAnalysis {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "incident_id", nullable = false, length = 50)
    private String incidentId;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "probable_cause", columnDefinition = "TEXT")
    private String probableCause;

    @Column(name = "remediation", columnDefinition = "TEXT")
    private String remediation;

    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "analysis_timestamp")
    private Long analysisTimestamp;

    @Column(name = "success")
    private boolean success;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    public AiAnalysis() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getProbableCause() { return probableCause; }
    public void setProbableCause(String probableCause) { this.probableCause = probableCause; }

    public String getRemediation() { return remediation; }
    public void setRemediation(String remediation) { this.remediation = remediation; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Long getAnalysisTimestamp() { return analysisTimestamp; }
    public void setAnalysisTimestamp(Long analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
}
