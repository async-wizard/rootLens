package com.rootlens.aianalysis.dto;

public class AiAnalysisEvent {

    private String incidentId;
    private String summary;
    private String probableCause;
    private String remediation;
    private String model;
    private Long analysisTimestamp;
    private boolean success;

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
}
