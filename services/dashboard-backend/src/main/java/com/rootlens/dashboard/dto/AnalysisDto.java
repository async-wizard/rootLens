package com.rootlens.dashboard.dto;

import java.io.Serializable;

public class AnalysisDto implements Serializable {

    private String summary;
    private String probableCause;
    private String remediation;
    private String model;
    private Long analysisTimestamp;
    private boolean success;

    public AnalysisDto() {}

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
