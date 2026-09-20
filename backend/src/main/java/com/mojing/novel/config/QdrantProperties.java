package com.mojing.novel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "mojing.qdrant")
public record QdrantProperties(String baseUrl, String apiKey, String collection, String storyBibleCollection,
                               int vectorSize, Duration timeout) {
    public QdrantProperties {
        baseUrl = hasText(baseUrl) ? trimSlash(baseUrl.trim()) : "http://127.0.0.1:6333";
        apiKey = apiKey == null ? "" : apiKey.trim();
        collection = hasText(collection) ? collection.trim() : "novel_memory";
        storyBibleCollection = hasText(storyBibleCollection) ? storyBibleCollection.trim() : "novel_story_bible_chat";
        vectorSize = vectorSize <= 0 ? 1024 : vectorSize;
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
    }

    public boolean hasApiKey() { return hasText(apiKey); }
    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static String trimSlash(String value) { return value.endsWith("/") ? value.substring(0, value.length() - 1) : value; }
}
