package com.mojing.novel.writing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AgentDtos {
    private AgentDtos() {}

    public record CharacterRequest(
            @NotBlank(message = "人物姓名不能为空") @Size(max = 100) String name,
            @Size(max = 500) String aliases,
            @Size(max = 100) String role,
            @Size(max = 500) String affiliations,
            @Size(max = 5000) String description,
            @Size(max = 5000) String personality,
            @Size(max = 5000) String goal,
            @Size(max = 5000) String currentState,
            @Size(max = 5000) String relationships,
            Long version) {}

    public record CharacterResponse(Long id, Long novelId, String name, String aliases, String role, String affiliations, String description,
                                    String personality, String goal, String currentState, String relationships,
                                    List<CharacterIdentityResponse> identities, Long version) {}
    public record CharacterIdentityResponse(Long id, String identityName, String identityType,
                                            Integer firstAppearanceChapterNo, Integer revealChapterNo,
                                            String description) {}

    public record OrganizationRequest(
            @NotBlank(message = "组织名称不能为空") @Size(max = 100) String name,
            @Size(max = 500) String aliases,
            @Size(max = 100) String type,
            @Size(max = 5000) String description,
            @Size(max = 5000) String goal,
            @Size(max = 5000) String structure,
            @Size(max = 5000) String relationships,
            Long version) {}

    public record OrganizationResponse(Long id, Long novelId, String name, String aliases, String type,
                                       String description, String goal, String structure,
                                       String relationships, Long version) {}

    public record WorldSettingRequest(
            @NotBlank(message = "设定分类不能为空") @Size(max = 50) String category,
            @NotBlank(message = "设定标题不能为空") @Size(max = 200) String title,
            @NotBlank(message = "设定内容不能为空") @Size(max = 10000) String content,
            Long version) {}

    public record WorldSettingResponse(Long id, Long novelId, String category, String title,
                                       String content, Long version) {}

    public record StoryVolumeRequest(
            @NotNull @Min(1) Integer volumeNo,
            @NotBlank(message = "分卷标题不能为空") @Size(max = 200) String title,
            @NotNull @Min(1) Integer chapterStart,
            @Min(1) Integer chapterEnd,
            @Size(max = 5000) String objective,
            @Size(max = 30000) String retrospective,
            @Size(max = 30000) String futurePlan,
            @Size(max = 10000) String keyTurningPoints,
            @Size(max = 5000) String climax,
            @Size(max = 5000) String endingHook,
            @Size(max = 10000) String foreshadows,
            @Size(max = 10000) String lockedBeats,
            @Min(1) Integer analyzedThroughChapterNo,
            @NotBlank @Pattern(regexp = "GENERATED|CONFIRMED", message = "不支持的分卷状态") String status,
            Long version) {}

    public record StoryVolumeResponse(
            Long id, Long novelId, Integer volumeNo, String title, Integer chapterStart, Integer chapterEnd,
            String objective, String retrospective, String chapterFacts, String futurePlan, String keyTurningPoints,
            String climax, String endingHook, String foreshadows, String lockedBeats,
            Integer analyzedThroughChapterNo, Long sourceContentVersionSum, String status,
            boolean stale, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record StoryPartRequest(
            @NotNull @Min(1) Integer partNo,
            @NotBlank(message = "部分标题不能为空") @Size(max = 200) String title,
            @NotNull @Min(1) Integer chapterStart,
            @Min(1) Integer chapterEnd,
            @Size(max = 5000) String objective,
            @Size(max = 30000) String plan,
            @Size(max = 30000) String retrospective,
            @NotBlank @Pattern(regexp = "GENERATED|CONFIRMED", message = "不支持的部分状态") String status,
            Long version) {}

    public record StoryPartResponse(
            Long id, Long novelId, Long volumeId, Integer partNo, String title,
            Integer chapterStart, Integer chapterEnd, String objective, String plan,
            String retrospective, String status, Long version,
            LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record StoryVolumeContent(
            @Size(max = 200) String title,
            @Size(max = 5000) String objective,
            @Size(max = 30000) String retrospective,
            @Size(max = 30000) String futurePlan,
            @Size(max = 10000) String keyTurningPoints,
            @Size(max = 5000) String climax,
            @Size(max = 5000) String endingHook,
            @Size(max = 10000) String foreshadows,
            @Size(max = 10000) String lockedBeats) {}

    public record StoryVolumeGenerateRequest(
            @NotNull @Min(1) Integer volumeNo,
            @Size(max = 200) String title,
            @NotNull @Min(1) Integer chapterStart,
            @Min(1) Integer chapterEnd,
            @Size(max = 5000) String instruction,
            @Valid StoryVolumeContent currentContent) {
        String normalizedInstruction() { return instruction == null ? "" : instruction.trim(); }
    }

    public record StoryVolumeGenerateResponse(StoryVolumeContent content, Integer analyzedThroughChapterNo,
                                              List<String> changeSummary, String providerMode) {}

    public record StoryBibleAssistRequest(
            @NotBlank @Pattern(regexp = "CHARACTER|ORGANIZATION|WORLD_SETTING", message = "不支持的资料类型")
            String type,
            @NotBlank @Pattern(regexp = "GENERATE|EXPAND|POLISH", message = "不支持的 AI 操作")
            String mode,
            @Size(max = 5000, message = "关键信息不能超过5000字") String instruction,
            @NotNull @Size(max = 10, message = "资料字段过多") Map<String, String> currentFields,
            @Size(max = 10, message = "目标字段过多") List<@Size(max = 50) String> targetFields) {
        String normalizedInstruction() { return instruction == null ? "" : instruction.trim(); }
        List<String> normalizedTargetFields() { return targetFields == null ? List.of() : targetFields; }
    }

    public record StoryBibleAssistResponse(String type, String mode, Map<String, String> fields,
                                           List<String> changeSummary, String providerMode) {}

    public record StoryBibleChatRequest(
            @NotBlank(message = "请输入想和资料库顾问讨论的内容")
            @Size(max = 4000, message = "单次对话不能超过4000字") String message,
            @Pattern(regexp = "VOLUME|CHARACTER|ORGANIZATION|WORLD_SETTING", message = "不支持的关注资料类型")
            String focusType,
            Long focusId) {
        String normalizedMessage() { return message.trim(); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StoryBibleCharacterSuggestion(
            @NotBlank @Pattern(regexp = "CREATE|UPDATE") String action,
            Long targetId,
            @NotBlank String name,
            String aliases,
            String role,
            String affiliations,
            String description,
            String personality,
            String goal,
            String currentState,
            String relationships,
            String rationale) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StoryBibleSuggestion(
            String type, String action, Long targetId,
            String name, String aliases, String role, String affiliations, String description,
            String personality, String goal, String currentState, String relationships, String structure,
            String category, String title, String content,
            Integer volumeNo, Integer chapterStart, Integer chapterEnd,
            String objective, String futurePlan, String keyTurningPoints, String climax,
            String endingHook, String foreshadows, String rationale) {}

    public record StoryBibleChatTurnOutput(
            @NotBlank String reply,
            @NotNull List<StoryBibleSuggestion> suggestions) {}

    public record StoryBibleChatMessageResponse(
            Long id, String role, String content,
            List<StoryBibleSuggestion> suggestions,
            List<Integer> appliedSuggestionIndexes,
            List<Long> recalledMessageIds,
            LocalDateTime createdAt) {}

    public record StoryBibleChatConversationResponse(Long novelId, List<StoryBibleChatMessageResponse> messages) {}

    public record StoryBibleApplySuggestionResponse(Integer suggestionIndex, String type, Object result,
                                                     StoryBibleChatConversationResponse conversation) {}

    public record NovelProjectContent(
            @Size(max = 5000) String inspiration,
            @Size(max = 100) String genre,
            @Size(max = 100) String channel,
            @Size(max = 500) String targetAudience,
            @Size(max = 100) String platform,
            @Size(max = 5000) String coreSellingPoint,
            @Size(max = 5000) String protagonistHook,
            @Size(max = 10000) String growthRoute,
            @Size(max = 5000) String readerExpectations,
            @Size(max = 10000) String openingThreeChapters,
            @Min(10000) @Max(10000000) Integer expectedWords,
            @Min(1) @Max(100) Integer expectedVolumes,
            @Min(300) @Max(10000) Integer chapterWordTarget,
            @Size(max = 1000) String description,
            @Size(max = 100000) String outline) {}

    public record NovelProjectSaveRequest(
            @NotNull @Valid NovelProjectContent content,
            @NotNull Long novelVersion,
            Long profileVersion) {}

    public record NovelProjectResponse(Long novelId, NovelProjectContent content,
                                       Long novelVersion, Long profileVersion,
                                       Integer writtenThroughChapterNo, LocalDateTime updatedAt) {}

    public record NovelProjectGenerateRequest(
            @NotBlank @Pattern(regexp = "FOUNDATION|DESCRIPTION|OUTLINE|ALL", message = "不支持的立项生成范围")
            String target,
            @Size(max = 5000) String instruction,
            @NotNull @Valid NovelProjectContent current) {
        String normalizedInstruction() { return instruction == null ? "" : instruction.trim(); }
    }

    public record NovelProjectGenerateResponse(NovelProjectContent content,
                                               Integer writtenThroughChapterNo,
                                               List<String> changeSummary,
                                               String providerMode) {}

    public record NovelProjectChatRequest(
            @NotBlank(message = "请输入想和立项顾问讨论的内容")
            @Size(max = 4000, message = "单次对话不能超过4000字") String message,
            @NotBlank @Pattern(regexp = "DISCUSS|FOUNDATION|DESCRIPTION|OUTLINE|ALL", message = "不支持的立项对话操作") String target,
            boolean importWrittenStory,
            @NotNull @Valid NovelProjectContent current) {
        String normalizedMessage() { return message.trim(); }
    }

    public record NovelProjectMemory(List<String> confirmedFacts, List<String> characterPremises,
                                     List<String> lockedConstraints, List<String> rejectedIdeas,
                                     List<String> answeredQuestions, List<String> openQuestions,
                                     List<String> authorPreferences, List<Long> sourceMessageIds) {}

    public record NovelProjectChatTurnOutput(@NotBlank String reply, NovelProjectContent suggestion,
                                             NovelProjectMemory memory,
                                             Integer analyzedThroughChapterNo, Long sourceContentVersionSum) {}

    public record NovelProjectChatMessageResponse(Long id, String role, String content,
                                                  NovelProjectContent suggestion, boolean applied,
                                                  Integer analyzedThroughChapterNo, LocalDateTime createdAt) {}

    public record NovelProjectChatConversationResponse(Long novelId,
                                                       List<NovelProjectChatMessageResponse> messages,
                                                       NovelProjectMemory memory) {}

    public record NovelProjectChatApplyResponse(NovelProjectResponse project,
                                                NovelProjectChatConversationResponse conversation) {}

    public record PlanRequest(@Size(max = 2000, message = "创作要求不能超过2000字") String guidance) {
        String normalizedGuidance() { return guidance == null ? "" : guidance.trim(); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChapterPlan(
            @NotBlank String title,
            @NotBlank String objective,
            @NotBlank String opening,
            @NotNull List<@NotBlank String> developments,
            @NotBlank String endingHook,
            @NotNull List<@NotBlank String> characters,
            @NotNull List<@NotBlank String> continuityNotes) {}

    public record DirectorChatRequest(
            @NotBlank(message = "请输入想如何调整本章主线")
            @Size(max = 2000, message = "单次对话不能超过2000字") String message,
            @Valid ChapterPlan currentPlan,
            Boolean thinkingEnabled,
            Boolean generatePlan) {
        String normalizedMessage() { return message.trim(); }
        boolean useThinking() { return Boolean.TRUE.equals(thinkingEnabled); }
        boolean shouldGeneratePlan() { return Boolean.TRUE.equals(generatePlan); }
    }

    public record SaveDirectorPlanRequest(@NotNull @Valid ChapterPlan plan) {}

    public record DirectorTurnOutput(
            @NotBlank String reply,
            @Valid ChapterPlan plan,
            @NotNull List<@NotBlank String> changeSummary) {}

    public record DirectorMessageResponse(Long id, String role, String content, Long runId,
                                          ChapterPlan plan, List<String> changeSummary,
                                          LocalDateTime createdAt) {}

    public record DirectorConversationResponse(Long chapterId, List<DirectorMessageResponse> messages,
                                               ChapterPlan currentPlan, Long currentRunId) {}

    public record DirectorRagPreviewRequest(
            @Size(max = 2000, message = "检索提示不能超过2000字") String message) {
        String normalizedMessage() { return message == null ? "" : message.trim(); }
    }

    public record DirectorRagMemory(String pointId, double score, Long chapterId, Integer chapterOrder,
                                    String memoryType, String text, boolean used) {}

    public record DirectorRagPreviewResponse(boolean enabled, String status, int retrievedCount,
                                             int usedCount, long durationMs,
                                             List<DirectorRagMemory> memories) {}

    public record DraftRequest(
            @NotNull @Valid ChapterPlan plan,
            @Min(value = 300, message = "草稿长度不能少于300字")
            @Max(value = 5000, message = "草稿长度不能超过5000字") Integer targetLength,
            @Size(max = 80, message = "文风选项格式不正确") String styleId) {
        int normalizedTargetLength() { return targetLength == null ? 1000 : targetLength; }
    }

    public record AgentStepResponse(Long id, Integer stepNo, String type, String status,
                                    String summary, Long durationMs, LocalDateTime createdAt) {}

    public record AiCallResponse(Long id, String stepType, String model, String status,
                                 Integer promptTokens, Integer completionTokens, Integer totalTokens,
                                 Long durationMs, Integer retryCount, String errorMessage,
                                 LocalDateTime createdAt) {}

    public record AgentRunResponse(Long id, Long novelId, Long chapterId, String status, String currentStep,
                                   String operation, String providerMode, String model,
                                   Integer promptTokens, Integer completionTokens, Integer totalTokens,
                                   Long totalDurationMs, Integer retryCount, String errorMessage,
                                   ChapterPlan plan, String draft, List<AgentStepResponse> steps,
                                   List<AiCallResponse> calls, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record AgentRunSummaryResponse(Long id, Long novelId, Long chapterId, String chapterTitle,
                                          String operation, String status, String currentStep,
                                          String providerMode, String model, Integer promptTokens,
                                          Integer completionTokens, Integer totalTokens, Long totalDurationMs,
                                          Integer retryCount, String errorMessage, LocalDateTime createdAt) {}

    public record RewriteRequest(
            @NotBlank(message = "请选择需要修改的正文") @Size(max = 10000, message = "单次修改不能超过10000字") String selectedText,
            @Size(max = 6000, message = "前文不能超过6000字") String beforeContext,
            @Size(max = 6000, message = "后文不能超过6000字") String afterContext,
            @NotBlank @Pattern(regexp = "POLISH|REWRITE|EXPAND|SHORTEN|TONE|CUSTOM", message = "不支持的修改方式") String mode,
            @Size(max = 2000, message = "修改要求不能超过2000字") String instruction,
            @NotNull @Min(0) Integer startOffset,
            @NotNull @Min(0) Integer endOffset,
            Boolean thinkingEnabled,
            @Size(max = 80, message = "文风选项格式不正确") String styleId) {
        String normalizedInstruction() { return instruction == null ? "" : instruction.trim(); }
        String normalizedBeforeContext() { return beforeContext == null ? "" : beforeContext; }
        String normalizedAfterContext() { return afterContext == null ? "" : afterContext; }
        boolean useThinking() { return Boolean.TRUE.equals(thinkingEnabled); }
    }

    public record RewriteOutput(
            @NotBlank String rewrittenText,
            @NotNull List<String> changeSummary,
            @NotNull List<String> warnings) {}

    public record RevisionDecisionRequest(
            @NotBlank @Pattern(regexp = "ACCEPTED|REJECTED|ROLLED_BACK", message = "不支持的修改状态") String status,
            @NotNull Long version) {}

    public record ChapterRevisionResponse(Long id, Long novelId, Long chapterId, Long agentRunId,
                                          String revisionType, String instruction, String originalText,
                                          String revisedText, Integer startOffset, Integer endOffset,
                                          String status, List<String> changeSummary, List<String> warnings,
                                          Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record ConsistencyCheckRequest(
            @Size(max = 1000, message = "检查重点不能超过1000字") String focus) {
        String normalizedFocus() { return focus == null ? "" : focus.trim(); }
    }

    public record ConsistencyIssueOutput(
            @NotBlank String type,
            @NotBlank @Pattern(regexp = "HIGH|MEDIUM|LOW") String severity,
            @NotBlank String quote,
            @NotBlank String message,
            @NotBlank String suggestion) {}

    public record ConsistencyOutput(
            @NotNull @Min(0) @Max(100) Integer score,
            @NotBlank String summary,
            @NotNull List<@Valid ConsistencyIssueOutput> issues) {}

    public record IssueDecisionRequest(
            @NotBlank @Pattern(regexp = "IGNORED|OPEN") String status,
            @NotNull Long version) {}

    public record IssueFixRequest(@Size(max = 1000) String instruction) {
        String normalizedInstruction() { return instruction == null ? "" : instruction.trim(); }
    }

    public record ConsistencyIssueResponse(Long id, Long reportId, String type, String severity,
                                           String quote, String message, String suggestion, String status,
                                           Long revisionId, Long version, LocalDateTime createdAt,
                                           LocalDateTime updatedAt, ChapterRevisionResponse revision) {}

    public record ConsistencyReportResponse(Long id, Long novelId, Long chapterId, Long agentRunId,
                                            Long sourceContentVersion, boolean stale,
                                            Integer score, String summary, String status, String providerMode,
                                            List<ConsistencyIssueResponse> issues, LocalDateTime createdAt) {}

    public record CharacterProfileChange(
            @NotBlank @Pattern(regexp = "DESCRIPTION|PERSONALITY|GOAL|AFFILIATIONS|CURRENT_STATE|RELATIONSHIPS") String field,
            @NotBlank @Pattern(regexp = "ADD|REPLACE|REMOVE") String operation,
            String oldValue,
            @NotBlank String newValue,
            String evidence,
            @NotBlank @Pattern(regexp = "HIGH|MEDIUM|LOW") String confidence,
            Boolean applyToProfile) {
        public CharacterProfileChange {
            confidence = confidence == null || confidence.isBlank() ? "MEDIUM" : confidence;
            applyToProfile = Boolean.TRUE.equals(applyToProfile);
        }
    }
    public record MemoryCharacter(@NotBlank String name, String role, String actions,
                                  String stateChange, String newKnowledge, Boolean firstAppearance,
                                  @NotNull List<@NotBlank String> foreshadowings,
                                  @NotNull List<@Valid CharacterProfileChange> profileChanges) {
        public MemoryCharacter {
            foreshadowings = foreshadowings == null ? List.of() : foreshadowings;
            profileChanges = profileChanges == null ? List.of() : profileChanges;
        }
        public MemoryCharacter(String name, String role, String actions, String stateChange,
                               String newKnowledge, Boolean firstAppearance) {
            this(name, role, actions, stateChange, newKnowledge, firstAppearance, List.of(), List.of());
        }
    }
    public record MemoryEvent(@NotBlank String event, String cause, String result, String location) {}
    public record MemoryForeshadowing(@NotBlank String content,
                                      @Pattern(regexp = "PLANTED|RESOLVED") String status,
                                      @Pattern(regexp = "HIGH|MEDIUM|LOW") String importance) {}
    public record MemorySceneEvent(String time, String location, @NotBlank String event) {}
    public record MemoryScene(
            @NotNull @Min(1) Integer sceneIndex,
            @NotBlank String title,
            @NotBlank @Pattern(regexp = "HARD|SOFT") String boundaryType,
            @NotBlank String continuityKey,
            String timeSpan,
            @NotNull List<@NotBlank String> locations,
            @NotNull List<@NotBlank String> characters,
            String goal,
            String conflict,
            @NotNull List<@Valid MemorySceneEvent> subEvents,
            String result) {}
    public record MemoryImportantFact(
            @NotBlank @Pattern(regexp = "FORESHADOWING|UNRESOLVED_THREAD|IMPORTANT_ITEM|WORLD_RULE|LOCATION_STATE|RELATIONSHIP|TIME_MARKER|REVEAL|OTHER") String type,
            @NotBlank String content,
            @NotBlank @Pattern(regexp = "HIGH|MEDIUM|LOW") String importance) {}
    public record IdentityReveal(@NotBlank String identityName, @NotBlank String realCharacterName,
                                 String evidence,
                                 @NotBlank @Pattern(regexp = "HIGH|MEDIUM|LOW") String confidence,
                                 Boolean applyMerge) {
        public IdentityReveal { applyMerge = Boolean.TRUE.equals(applyMerge); }
    }

    public record ChapterMemoryContent(
            String skillVersion,
            @NotBlank String summary,
            String timeInfo,
            @NotNull List<@NotBlank String> locations,
            @NotNull List<@Valid MemoryCharacter> characters,
            @NotNull List<@Valid MemoryScene> scenes,
            @NotNull List<@Valid MemoryImportantFact> importantFacts,
            @NotNull List<@Valid MemoryEvent> keyEvents,
            @NotNull List<@Valid MemoryForeshadowing> foreshadowings,
            @NotNull List<@NotBlank String> unresolvedQuestions,
            String plotProgress,
            @NotBlank @Pattern(regexp = "HIGH|MEDIUM|LOW") String importance,
            @NotNull List<@Valid IdentityReveal> identityReveals) {
        public ChapterMemoryContent {
            skillVersion = skillVersion == null || skillVersion.isBlank() ? "legacy" : skillVersion;
            locations = locations == null ? List.of() : locations;
            characters = characters == null ? List.of() : characters;
            scenes = scenes == null ? List.of() : scenes;
            importantFacts = importantFacts == null ? List.of() : importantFacts;
            keyEvents = keyEvents == null ? List.of() : keyEvents;
            foreshadowings = foreshadowings == null ? List.of() : foreshadowings;
            unresolvedQuestions = unresolvedQuestions == null ? List.of() : unresolvedQuestions;
            identityReveals = identityReveals == null ? List.of() : identityReveals;
            importance = importance == null || importance.isBlank() ? "MEDIUM" : importance;
        }
        public ChapterMemoryContent(String skillVersion, String summary, String timeInfo, List<String> locations,
                                    List<MemoryCharacter> characters, List<MemoryScene> scenes,
                                    List<MemoryImportantFact> importantFacts, List<MemoryEvent> keyEvents,
                                    List<MemoryForeshadowing> foreshadowings, List<String> unresolvedQuestions,
                                    String plotProgress, String importance) {
            this(skillVersion, summary, timeInfo, locations, characters, scenes, importantFacts, keyEvents,
                    foreshadowings, unresolvedQuestions, plotProgress, importance, List.of());
        }
    }

    public record ExtractChapterMemoryRequest(@Size(max = 1000) String instruction, Boolean thinkingEnabled) {
        String normalizedInstruction() { return instruction == null ? "" : instruction.trim(); }
        boolean normalizedThinkingEnabled() { return Boolean.TRUE.equals(thinkingEnabled); }
    }

    public record ConfirmChapterMemoryRequest(@NotNull @Valid ChapterMemoryContent content,
                                              @NotNull Long version) {}

    public record ChapterMemoryResponse(Long id, Long novelId, Long chapterId, Integer chapterNo,
                                        Long sourceChapterVersion, String status, ChapterMemoryContent content,
                                        String providerMode, boolean stale, Long version,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record CharacterHistoryResponse(Long id, Long novelId, Long characterId, String characterName,
                                           Long chapterId, Integer chapterNo, Long sourceChapterVersion,
                                           String actions, String stateChange, String newKnowledge,
                                           List<String> foreshadowings, List<CharacterProfileChange> profileChanges,
                                           String status, LocalDateTime createdAt) {}
}
