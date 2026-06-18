package com.rootlens.aianalysis.service;

public class AnalysisResult {

    private final String summary;
    private final String probableCause;
    private final String remediation;
    private final boolean success;
    private final String rawResponse;

    private AnalysisResult(String summary, String probableCause, String remediation,
                           boolean success, String rawResponse) {
        this.summary = summary;
        this.probableCause = probableCause;
        this.remediation = remediation;
        this.success = success;
        this.rawResponse = rawResponse;
    }

    public static AnalysisResult success(String summary, String probableCause,
                                          String remediation, String raw) {
        return new AnalysisResult(summary, probableCause, remediation, true, raw);
    }

    public static AnalysisResult failed(String raw) {
        return new AnalysisResult(null, null, null, false, raw);
    }

    public String getSummary() { return summary; }
    public String getProbableCause() { return probableCause; }
    public String getRemediation() { return remediation; }
    public boolean isSuccess() { return success; }
    public String getRawResponse() { return rawResponse; }
}
