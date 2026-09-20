package com.mojing.novel.embedding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class EmbeddingDtos {
    private EmbeddingDtos() {}
    public record EmbeddingRequest(
            @NotBlank(message = "向量化文本不能为空")
            @Size(max = 20_000, message = "向量化文本不能超过20000个字符")
            String text) {}
    public record EmbeddingResponse(String model, int dimensions, List<Double> vector,
                                    int promptTokens, int totalTokens) {}
    public record BatchEmbeddingResponse(String model, int dimensions, List<List<Double>> vectors,
                                         int promptTokens, int totalTokens) {}
}
