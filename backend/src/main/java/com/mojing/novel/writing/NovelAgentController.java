package com.mojing.novel.writing;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static com.mojing.novel.writing.AgentDtos.*;

@RestController
@RequestMapping("/api/agent")
public class NovelAgentController {
    private final NovelAgentService service;
    private final DirectorStreamingService streamingService;
    private final NovelAccessService novelAccessService;
    public NovelAgentController(NovelAgentService service, DirectorStreamingService streamingService,
                                NovelAccessService novelAccessService) {
        this.service = service;
        this.streamingService = streamingService;
        this.novelAccessService = novelAccessService;
    }

    @PostMapping("/novels/{novelId}/chapters/{chapterId}/plan")
    public AgentRunResponse createPlan(@PathVariable long novelId, @PathVariable long chapterId,
                                       @Valid @RequestBody PlanRequest request) {
        novelAccessService.requireNovelChapter(novelId, chapterId); return service.createPlan(novelId, chapterId, request);
    }

    @GetMapping("/chapters/{chapterId}/director")
    public DirectorConversationResponse getDirectorConversation(@PathVariable long chapterId) {
        novelAccessService.requireChapter(chapterId); return service.getDirectorConversation(chapterId);
    }

    @PutMapping("/chapters/{chapterId}/director/plan")
    public ChapterPlan saveDirectorPlan(@PathVariable long chapterId,
                                        @Valid @RequestBody SaveDirectorPlanRequest request) {
        novelAccessService.requireChapter(chapterId); return service.saveDirectorPlan(chapterId, request);
    }

    @PostMapping("/novels/{novelId}/chapters/{chapterId}/director/messages")
    public DirectorConversationResponse chatWithDirector(@PathVariable long novelId,
                                                         @PathVariable long chapterId,
                                                         @Valid @RequestBody DirectorChatRequest request) {
        novelAccessService.requireNovelChapter(novelId, chapterId); return service.chatWithDirector(novelId, chapterId, request);
    }

    @PostMapping(value = "/novels/{novelId}/chapters/{chapterId}/director/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatWithDirector(@PathVariable long novelId,
                                             @PathVariable long chapterId,
                                             @Valid @RequestBody DirectorChatRequest request,
                                             HttpServletResponse response) {
        novelAccessService.requireNovelChapter(novelId, chapterId);
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        return streamingService.stream(novelId, chapterId, request);
    }

    @DeleteMapping("/director/messages/{messageId}")
    public DirectorConversationResponse deleteDirectorTurn(@PathVariable long messageId) {
        novelAccessService.requireDirectorMessage(messageId); return service.deleteDirectorTurn(messageId);
    }

    @PostMapping("/novels/{novelId}/chapters/{chapterId}/director/rag-preview")
    public DirectorRagPreviewResponse previewDirectorRag(@PathVariable long novelId,
                                                        @PathVariable long chapterId,
                                                        @Valid @RequestBody DirectorRagPreviewRequest request) {
        novelAccessService.requireNovelChapter(novelId, chapterId); return service.previewDirectorRag(novelId, chapterId, request);
    }

    @PostMapping("/runs/{runId}/draft")
    public AgentRunResponse createDraft(@PathVariable long runId, @Valid @RequestBody DraftRequest request) {
        novelAccessService.requireRun(runId); return service.createDraft(runId, request);
    }

    @GetMapping("/runs/{runId}")
    public AgentRunResponse getRun(@PathVariable long runId) { novelAccessService.requireRun(runId); return service.getRun(runId); }

    @GetMapping("/novels/{novelId}/runs")
    public List<AgentRunSummaryResponse> listRuns(@PathVariable long novelId,
                                                  @RequestParam(defaultValue = "30") int limit) {
        novelAccessService.requireNovel(novelId); return service.listRuns(novelId, limit);
    }

    @PostMapping("/novels/{novelId}/chapters/{chapterId}/rewrite")
    public ChapterRevisionResponse rewrite(@PathVariable long novelId, @PathVariable long chapterId,
                                           @Valid @RequestBody RewriteRequest request) {
        novelAccessService.requireNovelChapter(novelId, chapterId); return service.rewrite(novelId, chapterId, request);
    }

    @GetMapping("/chapters/{chapterId}/revisions")
    public List<ChapterRevisionResponse> listRevisions(@PathVariable long chapterId) {
        novelAccessService.requireChapter(chapterId); return service.listRevisions(chapterId);
    }

    @PatchMapping("/revisions/{revisionId}")
    public ChapterRevisionResponse decideRevision(@PathVariable long revisionId,
                                                  @Valid @RequestBody RevisionDecisionRequest request) {
        novelAccessService.requireRevision(revisionId); return service.decideRevision(revisionId, request);
    }

    @PostMapping("/novels/{novelId}/chapters/{chapterId}/consistency-check")
    public ConsistencyReportResponse checkConsistency(@PathVariable long novelId, @PathVariable long chapterId,
                                                      @Valid @RequestBody ConsistencyCheckRequest request) {
        novelAccessService.requireNovelChapter(novelId, chapterId); return service.checkConsistency(novelId, chapterId, request);
    }

    @GetMapping("/chapters/{chapterId}/consistency-reports")
    public List<ConsistencyReportResponse> listConsistencyReports(@PathVariable long chapterId) {
        novelAccessService.requireChapter(chapterId); return service.listConsistencyReports(chapterId);
    }

    @PatchMapping("/consistency-issues/{issueId}")
    public ConsistencyIssueResponse decideIssue(@PathVariable long issueId,
                                                @Valid @RequestBody IssueDecisionRequest request) {
        novelAccessService.requireConsistencyIssue(issueId); return service.decideIssue(issueId, request);
    }

    @PostMapping("/consistency-issues/{issueId}/fix")
    public ConsistencyIssueResponse fixIssue(@PathVariable long issueId,
                                             @Valid @RequestBody IssueFixRequest request) {
        novelAccessService.requireConsistencyIssue(issueId); return service.fixIssue(issueId, request);
    }

    @PostMapping("/chapters/{chapterId}/memory/extract")
    public ChapterMemoryResponse extractChapterMemory(@PathVariable long chapterId,
                                                       @Valid @RequestBody ExtractChapterMemoryRequest request) {
        novelAccessService.requireChapter(chapterId); return service.extractChapterMemory(chapterId, request);
    }

    @GetMapping("/chapters/{chapterId}/memory")
    public ChapterMemoryResponse getChapterMemory(@PathVariable long chapterId) {
        novelAccessService.requireChapter(chapterId); return service.getChapterMemory(chapterId);
    }

    @GetMapping("/novels/{novelId}/chapter-memories")
    public List<ChapterMemoryResponse> listChapterMemories(@PathVariable long novelId) {
        novelAccessService.requireNovel(novelId); return service.listChapterMemories(novelId);
    }

    @PutMapping("/chapter-memories/{memoryId}/confirm")
    public ChapterMemoryResponse confirmChapterMemory(@PathVariable long memoryId,
                                                       @Valid @RequestBody ConfirmChapterMemoryRequest request) {
        novelAccessService.requireChapterMemory(memoryId); return service.confirmChapterMemory(memoryId, request);
    }
}
