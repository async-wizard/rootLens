package com.rootlens.aianalysis.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClaudeAnalysisService implements LlmAnalysisProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAnalysisService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AnthropicClient anthropicClient;
    private final String model;
    private final long maxTokens;

    public ClaudeAnalysisService(AnthropicClient anthropicClient, String model, long maxTokens) {
        this.anthropicClient = anthropicClient;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public String getProviderName() {
        return model;
    }

    @Override
    public AnalysisResult analyze(String systemPrompt, String userMessage) {
        Message response = anthropicClient.messages().create(
                MessageCreateParams.builder()
                        .model(Model.CLAUDE_SONNET_4_6)
                        .maxTokens(maxTokens)
                        .system(systemPrompt)
                        .addUserMessage(userMessage)
                        .build()
        );

        String rawText = response.content().stream()
                .filter(block -> block.isText())
                .findFirst()
                .map(block -> block.asText().text())
                .orElse("");

        return parse(rawText);
    }

    private AnalysisResult parse(String rawText) {
        try {
            ParsedAnalysis parsed = objectMapper.readValue(rawText, ParsedAnalysis.class);
            return AnalysisResult.success(parsed.summary, parsed.probableCause, parsed.remediation, rawText);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Claude response as JSON: {}", e.getMessage());
            return AnalysisResult.failed(rawText);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ParsedAnalysis {
        public String summary;
        public String probableCause;
        public String remediation;
    }
}
