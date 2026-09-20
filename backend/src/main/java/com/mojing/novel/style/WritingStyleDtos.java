package com.mojing.novel.style;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public final class WritingStyleDtos {
    private WritingStyleDtos() {}

    public record SaveStyleRequest(
            Long novelId,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "CUSTOM|REFERENCE|NOVEL") String sourceType,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 10000) String rulesText,
            @Size(max = 4000) String forbiddenWords,
            @Size(max = 20000) String referenceExcerpt,
            Long version) {}

    public record StyleResponse(String id, Long databaseId, Long novelId, String name, String sourceType,
                                String description, String rulesText, String forbiddenWords,
                                String referenceExcerpt, boolean system, boolean editable,
                                Long version, LocalDateTime updatedAt) {}

    public record AssistStyleRequest(
            @NotBlank @Pattern(regexp = "ORGANIZE|REFERENCE|NOVEL") String mode,
            Long novelId,
            @Size(max = 5000) String instruction,
            @Size(max = 5) List<@NotBlank @Size(max = 10000) String> referenceTexts) {
        String normalizedInstruction() { return instruction == null ? "" : instruction.trim(); }
        List<String> normalizedReferences() { return referenceTexts == null ? List.of() : referenceTexts; }
    }

    public record AssistStyleResponse(String name, String description, String rulesText,
                                      String forbiddenWords, List<String> changeSummary,
                                      String providerMode) {}
}
