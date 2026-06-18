package com.rootlens.aianalysis.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

public class OpenAiCompatibleProvider implements LlmAnalysisProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String providerName;
    private final String model;
    private final long maxTokens;
    private final RestClient restClient;

    public OpenAiCompatibleProvider(String providerName, String apiKey,
                                    String baseUrl, String model, long maxTokens) {
        this.providerName = providerName;
        this.model = model;
        this.maxTokens = maxTokens;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String getProviderName() {
        return providerName + "/" + model;
    }

    @Override
    public AnalysisResult analyze(String systemPrompt, String userMessage) {
        ChatRequest request = new ChatRequest(
                model,
                List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userMessage)
                ),
                maxTokens
        );

        try {
            ChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                log.warn("[{}] Empty response received", providerName);
                return AnalysisResult.failed("empty response");
            }

            String rawText = response.choices.get(0).message.content;
            return parse(rawText);

        } catch (Exception e) {
            log.error("[{}] API call failed: {}", providerName, e.getMessage());
            return AnalysisResult.failed("API error: " + e.getMessage());
        }
    }

    private AnalysisResult parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return AnalysisResult.failed("empty content");
        }
        // Strip markdown code fences if present (some models wrap JSON in ```json ... ```)
        String cleaned = rawText.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
        }
        try {
            ParsedAnalysis parsed = objectMapper.readValue(cleaned, ParsedAnalysis.class);
            return AnalysisResult.success(parsed.summary, parsed.probableCause, parsed.remediation, rawText);
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to parse response as JSON: {}", providerName, e.getMessage());
            return AnalysisResult.failed(rawText);
        }
    }

    // ── Request / Response DTOs ────────────────────────────────────────────────

    private record ChatRequest(
            String model,
            List<ChatMessage> messages,
            @JsonProperty("max_tokens") long maxTokens
    ) {}

    private record ChatMessage(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatResponse {
        public List<Choice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Choice {
        public ChatMessage message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ParsedAnalysis {
        public String summary;
        public String probableCause;
        public String remediation;
    }
}
