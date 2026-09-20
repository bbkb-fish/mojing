package com.mojing.novel.writing;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class EvalDtos {
    private EvalDtos() {}

    public record SaveCaseRequest(
            Long chapterId,
            @NotBlank @Size(max=200) String name,
            @NotBlank @Size(max=10000) String inputContext,
            @Size(max=2000) String instruction,
            @Size(max=2000) String expectedCharacters,
            @Size(max=2000) String requiredTerms,
            @Size(max=2000) String forbiddenTerms,
            @Min(20) @Max(300) Integer minLength,
            @Min(50) @Max(300) Integer maxLength,
            Boolean enabled,
            Long version) {
        int normalizedMinLength(){return minLength==null?50:minLength;}
        int normalizedMaxLength(){return maxLength==null?160:maxLength;}
        boolean normalizedEnabled(){return enabled==null||enabled;}
        String normalized(String value){return value==null?"":value.trim();}
    }

    public record RunCaseRequest(Boolean useLlmJudge) { boolean normalizedUseJudge(){return useLlmJudge==null||useLlmJudge;} }

    public record EvalCaseResponse(Long id,Long novelId,Long chapterId,String name,String inputContext,
                                   String instruction,String expectedCharacters,String requiredTerms,String forbiddenTerms,
                                   Integer minLength,Integer maxLength,Boolean enabled,Long version,
                                   LocalDateTime createdAt,LocalDateTime updatedAt,EvalRunResponse latestRun) {}

    public record EvalRunResponse(Long id,Long caseId,String status,String generatorModel,
                                  String generatorPromptVersion,String judgePromptVersion,String generatedText,
                                  Integer ruleScore,Integer judgeScore,Integer overallScore,Boolean passed,
                                  List<String> violations,String judgeFeedback,Integer promptTokens,
                                  Integer completionTokens,Integer totalTokens,Long durationMs,
                                  String errorMessage,LocalDateTime createdAt) {}

    record JudgeOutput(@Min(0) @Max(100) Integer score,@NotBlank String feedback,List<String> issues) {}
}
