package com.mojing.novel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "mojing.embedding")
public record EmbeddingProperties(String apiKey, String baseUrl, String model,
                                  int dimensions, Duration timeout) {
    public EmbeddingProperties {
        baseUrl = hasText(baseUrl) ? trimSlash(baseUrl.trim()) : "https://dashscope.aliyuncs.com/compatible-mode/v1";
        model = hasText(model) ? model.trim() : "text-embedding-v4";
        dimensions = dimensions <= 0 ? 1024 : dimensions;
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }

    public boolean configured() { return hasText(apiKey); }
    public String embeddingsUrl() { return baseUrl.endsWith("/embeddings") ? baseUrl : baseUrl + "/embeddings"; }
    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static String trimSlash(String value) { return value.endsWith("/") ? value.substring(0, value.length() - 1) : value; }
}
