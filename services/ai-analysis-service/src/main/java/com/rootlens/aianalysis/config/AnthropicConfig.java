package com.rootlens.aianalysis.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "rootlens.ai.provider", havingValue = "claude", matchIfMissing = true)
public class AnthropicConfig {

    // Only instantiated when provider=claude (the default).
    // When using DeepSeek, Groq, or OpenRouter, ANTHROPIC_API_KEY does not need to be set.
    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }
}
