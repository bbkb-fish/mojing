package com.mojing.novel.writing;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.mojing.novel.writing.AgentDtos.DirectorChatRequest;
import static com.mojing.novel.writing.AgentDtos.DirectorConversationResponse;

@Service
public class DirectorStreamingService {
    private static final Logger log = LoggerFactory.getLogger(DirectorStreamingService.class);
    private static final long STREAM_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10);
    private final NovelAgentService agentService;
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("director-sse-heartbeat").daemon(true).factory());

    public DirectorStreamingService(NovelAgentService agentService) {
        this.agentService = agentService;
    }

    public SseEmitter stream(long novelId, long chapterId, DirectorChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<Future<?>> taskReference = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> heartbeatReference = new AtomicReference<>();

        emitter.onCompletion(() -> close(closed, taskReference, heartbeatReference));
        emitter.onTimeout(() -> close(closed, taskReference, heartbeatReference));
        emitter.onError(error -> close(closed, taskReference, heartbeatReference));

        send(emitter, "connected", Map.of(
                "chapterId", chapterId,
                "generatePlan", request.shouldGeneratePlan()));
        send(emitter, "status", Map.of("message",
                request.shouldGeneratePlan() ? "正在生成本章主线" : "章节导演正在思考"));

        heartbeatReference.set(heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (closed.get()) return;
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(Map.of("at", Instant.now().toString())));
            } catch (Exception error) {
                close(closed, taskReference, heartbeatReference);
            }
        }, 15, 15, TimeUnit.SECONDS));

        Future<?> task = streamExecutor.submit(() -> {
            try {
                DirectorConversationResponse conversation = agentService.chatWithDirectorStreamed(
                        novelId, chapterId, request,
                        delta -> send(emitter, "delta", Map.of("text", delta)));
                send(emitter, "complete", conversation);
                closed.set(true);
                cancelHeartbeat(heartbeatReference);
                emitter.complete();
            } catch (Exception error) {
                log.warn("章节导演SSE失败 novelId={} chapterId={} reason={}", novelId, chapterId, error.getMessage());
                if (!closed.get()) {
                    try { send(emitter, "error", Map.of("message", safeMessage(error))); }
                    catch (RuntimeException ignored) { }
                }
                closed.set(true);
                cancelHeartbeat(heartbeatReference);
                emitter.complete();
            }
        });
        taskReference.set(task);
        if (closed.get()) task.cancel(true);
        return emitter;
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException error) {
            throw new StreamDisconnectedException(error);
        }
    }

    private void close(AtomicBoolean closed, AtomicReference<Future<?>> taskReference,
                       AtomicReference<ScheduledFuture<?>> heartbeatReference) {
        if (!closed.compareAndSet(false, true)) return;
        Future<?> task = taskReference.get();
        if (task != null) task.cancel(true);
        cancelHeartbeat(heartbeatReference);
    }

    private void cancelHeartbeat(AtomicReference<ScheduledFuture<?>> heartbeatReference) {
        ScheduledFuture<?> heartbeat = heartbeatReference.get();
        if (heartbeat != null) heartbeat.cancel(false);
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? "章节导演流式响应失败，请重试" : message;
    }

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdownNow();
        heartbeatExecutor.shutdownNow();
    }

    private static class StreamDisconnectedException extends RuntimeException {
        StreamDisconnectedException(Throwable cause) { super("浏览器已断开章节导演流式连接", cause); }
    }
}
