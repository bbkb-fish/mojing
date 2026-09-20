package com.mojing.novel.completion;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
public class NovelCompletionController {

    private static final Logger log = LoggerFactory.getLogger(NovelCompletionController.class);

    private final NovelCompletionService completionService;
    private final CompletionStreamingService streamingService;

    public NovelCompletionController(NovelCompletionService completionService,
                                     CompletionStreamingService streamingService) {
        this.completionService = completionService;
        this.streamingService = streamingService;
    }

    @PostMapping("/completion")
    public CompletionResponse complete(@Valid @RequestBody CompletionRequest request) {
        log.info("收到AI续写请求：上下文={}字，后文={}字，目标长度={}字，内联补全={}，包含额外要求={}",
                request.cursorContext().length(), request.normalizedAfterCursor().length(), request.normalizedMaxLength(),
                request.isInlineCompletion(),
                !request.normalizedInstruction().isBlank());
        return completionService.complete(request);
    }

    @PostMapping(value = "/completion/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCompletion(@Valid @RequestBody CompletionRequest request,
                                       HttpServletResponse response) {
        log.info("收到流式自动补全请求：上下文={}字，后文={}字，目标长度=100字",
                request.cursorContext().length(), request.normalizedAfterCursor().length());
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        return streamingService.stream(request);
    }
}
