package com.mojing.novel.writing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class WritingDtos {
    private WritingDtos() {
    }

    public record CreateNovelRequest(
            @NotBlank(message = "小说名称不能为空")
            @Size(max = 200, message = "小说名称不能超过200字")
            String title,
            @Size(max = 1000, message = "小说简介不能超过1000字")
            String description,
            String outline
    ) {
    }

    public record UpdateNovelRequest(
            @NotBlank(message = "小说名称不能为空")
            @Size(max = 200, message = "小说名称不能超过200字")
            String title,
            @Size(max = 1000, message = "小说简介不能超过1000字")
            String description,
            String outline,
            @NotNull(message = "小说版本不能为空") Long version
    ) {
    }

    public record CreateChapterRequest(
            @NotBlank(message = "章节名称不能为空")
            @Size(max = 200, message = "章节名称不能超过200字")
            String title,
            String content,
            Integer chapterNo
    ) {
    }

    public record UpdateChapterRequest(
            @NotBlank(message = "章节名称不能为空")
            @Size(max = 200, message = "章节名称不能超过200字")
            String title,
            String content,
            @NotNull(message = "章节版本不能为空") Long version
    ) {
    }

    public record NovelResponse(
            Long id,
            String title,
            String description,
            String outline,
            String coverUrl,
            String status,
            Integer totalWords,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ChapterResponse(
            Long id,
            Long novelId,
            Integer chapterNo,
            String title,
            String content,
            Integer wordCount,
            String status,
            Long contentVersion,
            LocalDateTime completedAt,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
