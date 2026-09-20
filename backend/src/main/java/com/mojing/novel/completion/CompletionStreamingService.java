package com.mojing.novel.completion;

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

@Service
public class CompletionStreamingService {
    private static final Logger log = LoggerFactory.getLogger(CompletionStreamingService.class);
    private static final long STREAM_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(2);

    private final NovelCompletionService completionService;
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("completion-sse-heartbeat").daemon(true).factory());

    public CompletionStreamingService(NovelCompletionService completionService) {
        this.completionService = completionService;
    }

    public SseEmitter stream(CompletionRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<Future<?>> taskReference = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> heartbeatReference = new AtomicReference<>();

        emitter.onCompletion(() -> close(closed, taskReference, heartbeatReference));
        emitter.onTimeout(() -> close(closed, taskReference, heartbeatReference));
        emitter.onError(error -> close(closed, taskReference, heartbeatReference));
        send(emitter, "connected", Map.of("maxLength", 100));

        heartbeatReference.set(heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (closed.get()) return;
            try {
                emitter.send(SseEmitter.event().name("heartbeat")
                        .data(Map.of("at", Instant.now().toString())));
            } catch (Exception error) {
                close(closed, taskReference, heartbeatReference);
            }
        }, 15, 15, TimeUnit.SECONDS));

        Future<?> task = streamExecutor.submit(() -> {
            try {
                CompletionResponse response = completionService.completeStreaming(request,
                        delta -> send(emitter, "delta", Map.of("text", delta)));
                send(emitter, "complete", response);
                closed.set(true);
                cancelHeartbeat(heartbeatReference);
                emitter.complete();
            } catch (Exception error) {
                log.warn("AI自动补全SSE失败 novelId={} chapterId={} reason={}",
                        request.novelId(), request.chapterId(), error.getMessage());
                if (!closed.get()) {
                    try {
                        send(emitter, "error", Map.of("message", safeMessage(error)));
                    } catch (RuntimeException ignored) {
                    }
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
        return message == null || message.isBlank() ? "AI自动补全流式响应失败，请重试" : message;
    }

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdownNow();
        heartbeatExecutor.shutdownNow();
    }

    private static class StreamDisconnectedException extends RuntimeException {
        StreamDisconnectedException(Throwable cause) {
            super("浏览器已断开自动补全流式连接", cause);
        }
    }
}
