package com.rootlens.dashboard.entity;

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

    @Column(name = "incident_id", length = 50)
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

    public String getId() { return id; }
    public String getIncidentId() { return incidentId; }
    public String getSummary() { return summary; }
    public String getProbableCause() { return probableCause; }
    public String getRemediation() { return remediation; }
    public String getModel() { return model; }
    public Long getAnalysisTimestamp() { return analysisTimestamp; }
    public boolean isSuccess() { return success; }
}
