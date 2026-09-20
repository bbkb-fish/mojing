package com.mojing.novel.completion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletionRequest(
        Long novelId,
        Long chapterId,

        @NotBlank(message = "上下文不能为空")
        @Size(max = 10_000, message = "上下文不能超过10000个字符")
        String cursorContext,

        @Min(value = 50, message = "续写长度不能少于50字")
        @Max(value = 300, message = "续写长度不能超过300字")
        Integer maxLength,

        @Size(max = 2_000, message = "续写要求不能超过2000个字符")
        String instruction,

        @Size(max = 80, message = "文风选项格式不正确")
        String styleId,

        @Size(max = 2_000, message = "光标后文不能超过2000个字符")
        String afterCursor,

        Boolean inlineCompletion
) {
    public int normalizedMaxLength() {
        return maxLength == null ? (isInlineCompletion() ? 100 : 120) : maxLength;
    }

    public String normalizedInstruction() {
        return instruction == null ? "" : instruction.trim();
    }

    public String normalizedAfterCursor() {
        return afterCursor == null ? "" : afterCursor;
    }

    public boolean isInlineCompletion() {
        return Boolean.TRUE.equals(inlineCompletion);
    }
}
