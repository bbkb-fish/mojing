package com.mojing.novel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "mojing.ai")
public record AiProperties(
        String apiKey,
        String baseUrl,
        String model,
        Duration timeout,
        double temperature,
        int draftMaxTokens,
        boolean useSystemProxy
) {
    public AiProperties {
        baseUrl = normalizeBaseUrl(baseUrl);
        model = hasText(model) ? model : "gpt-4.1-mini";
        timeout = timeout == null ? Duration.ofSeconds(180) : timeout;
        temperature = temperature <= 0 ? 0.8 : Math.min(temperature, 1.5);
        draftMaxTokens = draftMaxTokens <= 0 ? 16_000 : Math.min(draftMaxTokens, 32_768);
    }

    public boolean configured() {
        return hasText(apiKey);
    }

    public String chatCompletionsUrl() {
        return baseUrl + "/chat/completions";
    }

    private static String normalizeBaseUrl(String value) {
        String url = hasText(value) ? value.trim() : "https://api.openai.com/v1";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
