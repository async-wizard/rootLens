package com.rootlens.aianalysis.service;

public interface LlmAnalysisProvider {

    AnalysisResult analyze(String systemPrompt, String userMessage);

    String getProviderName();
}
