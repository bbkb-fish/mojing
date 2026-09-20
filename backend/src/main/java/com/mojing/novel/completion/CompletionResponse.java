package com.mojing.novel.completion;

import java.util.List;

public record CompletionResponse(
        String completion,
        String source,
        String model,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        long durationMs,
        RagDebugInfo rag
) {
    public record RagDebugInfo(boolean enabled, String status, int retrievedCount, int usedCount,
                               long durationMs, List<RagMemoryHit> memories) {}

    public record RagMemoryHit(String pointId, double score, Long chapterId, Integer chapterOrder,
                               String memoryType, String text, boolean used) {}
}
