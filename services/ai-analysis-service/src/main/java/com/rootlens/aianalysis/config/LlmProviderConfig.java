package com.rootlens.aianalysis.config;

import com.anthropic.client.AnthropicClient;
import com.rootlens.aianalysis.service.ClaudeAnalysisService;
import com.rootlens.aianalysis.service.LlmAnalysisProvider;
import com.rootlens.aianalysis.service.OpenAiCompatibleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class LlmProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderConfig.class);

    @Value("${rootlens.ai.provider:claude}")
    private String activeProvider;

    @Value("${anthropic.model:claude-sonnet-4-6}")
    private String claudeModel;

    @Value("${anthropic.max-tokens:1024}")
    private long claudeMaxTokens;

    @Value("${rootlens.ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${rootlens.ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${rootlens.ai.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${rootlens.ai.deepseek.max-tokens:1024}")
    private long deepseekMaxTokens;

    @Value("${rootlens.ai.groq.api-key:}")
    private String groqApiKey;

    @Value("${rootlens.ai.groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    @Value("${rootlens.ai.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${rootlens.ai.groq.max-tokens:1024}")
    private long groqMaxTokens;

    @Value("${rootlens.ai.openrouter.api-key:}")
    private String openrouterApiKey;

    @Value("${rootlens.ai.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openrouterBaseUrl;

    @Value("${rootlens.ai.openrouter.model:meta-llama/llama-3.1-8b-instruct:free}")
    private String openrouterModel;

    @Value("${rootlens.ai.openrouter.max-tokens:1024}")
    private long openrouterMaxTokens;

    @Bean
    public LlmAnalysisProvider llmAnalysisProvider(Optional<AnthropicClient> anthropicClient) {
        log.info("Initializing LLM provider: {}", activeProvider);

        return switch (activeProvider.toLowerCase()) {
            case "deepseek" -> new OpenAiCompatibleProvider(
                    "DeepSeek", deepseekApiKey, deepseekBaseUrl, deepseekModel, deepseekMaxTokens);

            case "groq" -> new OpenAiCompatibleProvider(
                    "Groq", groqApiKey, groqBaseUrl, groqModel, groqMaxTokens);

            case "openrouter" -> new OpenAiCompatibleProvider(
                    "OpenRouter", openrouterApiKey, openrouterBaseUrl, openrouterModel, openrouterMaxTokens);

            default -> {
                AnthropicClient client = anthropicClient.orElseThrow(() ->
                        new IllegalStateException(
                                "Provider is 'claude' but ANTHROPIC_API_KEY is not set. " +
                                "Set the env var or switch to a different provider via AI_PROVIDER."));
                yield new ClaudeAnalysisService(client, claudeModel, claudeMaxTokens);
            }
        };
    }
}
