package com.mojing.novel.qdrant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public final class VectorMemoryDtos {
    private VectorMemoryDtos() {}

    public record UpsertMemoryRequest(
            @Positive(message = "novelId必须大于0") long novelId,
            @Positive(message = "chapterId必须大于0") Long chapterId,
            @NotBlank(message = "记忆类型不能为空") @Size(max = 64, message = "记忆类型不能超过64个字符") String memoryType,
            @NotBlank(message = "记忆文本不能为空") @Size(max = 20000, message = "记忆文本不能超过20000个字符") String text,
            @PositiveOrZero(message = "正文版本不能小于0") Integer sourceRevision) {}

    public record SearchMemoryRequest(
            @Positive(message = "novelId必须大于0") long novelId,
            @NotBlank(message = "检索文本不能为空") @Size(max = 20000, message = "检索文本不能超过20000个字符") String text,
            @Min(value = 1, message = "limit不能小于1") @Max(value = 20, message = "limit不能大于20") Integer limit,
            @DecimalMin(value = "0.0", message = "相似度阈值不能小于0")
            @DecimalMax(value = "1.0", message = "相似度阈值不能大于1") Double scoreThreshold,
            @Positive(message = "excludeChapterId必须大于0") Long excludeChapterId,
            @Positive(message = "beforeChapterOrder必须大于0") Integer beforeChapterOrder) {}

    public record CollectionStatusResponse(String collection, int vectorSize, String distance, boolean ready) {}

    public record MemoryWriteResponse(String pointId, long novelId, Long chapterId,
                                      String memoryType, int dimensions) {}

    public record MemorySearchHit(String pointId, double score, long novelId, Long chapterId,
                                  Integer chapterOrder, Long memoryId, String memoryType, String text,
                                  String importance, Integer sourceRevision, String createdAt) {}

    public record MemorySearchResponse(String collection, int dimensions, int count,
                                       List<MemorySearchHit> hits) {}

    public record VectorMemoryItem(String memoryType, String text, Map<String, Object> metadata) {
        public VectorMemoryItem {
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }
}
