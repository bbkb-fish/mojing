package com.mojing.novel.completion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.config.AiProperties;
import com.mojing.novel.config.AiHttpClientFactory;
import com.mojing.novel.writing.AgentTelemetryService;
import com.mojing.novel.writing.CharacterContextService;
import com.mojing.novel.writing.OrganizationContextService;
import com.mojing.novel.writing.StoryVolumeContextService;
import com.mojing.novel.style.WritingStyleService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class NovelCompletionService {

    private static final int MODEL_CONTEXT_LIMIT = 6_000;

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AgentTelemetryService telemetry;
    private final RagContextService ragContextService;
    private final CharacterContextService characterContextService;
    private final OrganizationContextService organizationContextService;
    private final WritingStyleService writingStyleService;
    private final StoryVolumeContextService storyVolumeContextService;

    public NovelCompletionService(AiProperties properties, ObjectMapper objectMapper,
                                  AgentTelemetryService telemetry, RagContextService ragContextService,
                                  CharacterContextService characterContextService,
                                  OrganizationContextService organizationContextService,
                                  WritingStyleService writingStyleService,
                                  StoryVolumeContextService storyVolumeContextService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.telemetry = telemetry;
        this.ragContextService = ragContextService;
        this.characterContextService = characterContextService;
        this.organizationContextService = organizationContextService;
        this.writingStyleService = writingStyleService;
        this.storyVolumeContextService = storyVolumeContextService;
        this.httpClient = AiHttpClientFactory.create(properties);
    }

    public CompletionResponse complete(CompletionRequest request) {
        long started = System.nanoTime();
        String context = keepTail(request.cursorContext().trim(), MODEL_CONTEXT_LIMIT);
        String afterContext = request.normalizedAfterCursor();
        int maxLength = request.normalizedMaxLength();
        String instruction = request.normalizedInstruction();
        String stylePrompt = writingStyleService.resolvePrompt(request.styleId(), request.novelId());
        String selectionContext = request.isInlineCompletion() && !afterContext.isBlank()
                ? context + "\n<光标后文>\n" + afterContext : context;
        RagContextService.RagContext rag = ragContextService.retrieve(
                request.novelId(), request.chapterId(), selectionContext, instruction);
        CharacterContextService.Selection characterContext = characterContextService.select(
                request.novelId(), selectionContext, instruction, rag.promptContext());
        OrganizationContextService.Selection organizationContext = organizationContextService.select(
                request.novelId(), selectionContext, instruction, rag.promptContext());
        String memoryContext = joinContext(storyVolumeContextService.currentConfirmedVolume(
                request.novelId(), request.chapterId()), rag.promptContext());
        String stepType = request.isInlineCompletion() ? "INLINE_COMPLETION" : "QUICK_COMPLETION";
        Long runId = request.novelId() != null && request.chapterId() != null
                ? telemetry.startRun(request.novelId(), request.chapterId(),
                stepType, instruction,
                properties.configured() ? "ai" : "demo", properties.configured() ? properties.model() : "demo")
                : null;

        try {
            if (!properties.configured()) {
                String content = createDemoCompletion(context);
                if (runId != null) {
                    telemetry.recordCall(runId, stepType, "demo", "COMPLETED",
                            0, 0, 0, elapsed(started), 0, null);
                    telemetry.finishRun(runId, request.isInlineCompletion() ? "INLINE_COMPLETION_READY" : "COMPLETION_READY", elapsed(started));
                }
                return new CompletionResponse(content, "demo", "demo", 0, 0, 0, elapsed(started), rag.debugInfo());
            }

            ProviderResult result = callProvider(context, afterContext, maxLength, instruction, memoryContext,
                    characterContext.promptContext(), organizationContext.promptContext(), stylePrompt,
                    request.isInlineCompletion());
            if (runId != null) {
                telemetry.recordCall(runId, stepType, properties.model(), "COMPLETED",
                        result.promptTokens(), result.completionTokens(), result.totalTokens(),
                        result.durationMs(), 0, null);
                telemetry.finishRun(runId, request.isInlineCompletion() ? "INLINE_COMPLETION_READY" : "COMPLETION_READY", elapsed(started));
            }
            return new CompletionResponse(result.content(), "ai", properties.model(), result.promptTokens(),
                    result.completionTokens(), result.totalTokens(), elapsed(started), rag.debugInfo());
        } catch (AiProviderException error) {
            if (runId != null) {
                telemetry.recordCall(runId, stepType, properties.model(), "FAILED",
                        0, 0, 0, elapsed(started), 0, error.getMessage());
                telemetry.failRun(runId, stepType, elapsed(started), error.getMessage());
            }
            throw error;
        }
    }

    public CompletionResponse completeStreaming(CompletionRequest request, Consumer<String> onTextDelta) {
        long started = System.nanoTime();
        String context = keepTail(request.cursorContext().trim(), MODEL_CONTEXT_LIMIT);
        String afterContext = request.normalizedAfterCursor();
        int maxLength = Math.min(100, request.normalizedMaxLength());
        String instruction = request.normalizedInstruction();
        String stylePrompt = writingStyleService.resolvePrompt(request.styleId(), request.novelId());
        String selectionContext = afterContext.isBlank()
                ? context : context + "\n<光标后文>\n" + afterContext;
        RagContextService.RagContext rag = ragContextService.retrieve(
                request.novelId(), request.chapterId(), selectionContext, instruction);
        CharacterContextService.Selection characterContext = characterContextService.select(
                request.novelId(), selectionContext, instruction, rag.promptContext());
        OrganizationContextService.Selection organizationContext = organizationContextService.select(
                request.novelId(), selectionContext, instruction, rag.promptContext());
        String memoryContext = joinContext(storyVolumeContextService.currentConfirmedVolume(
                request.novelId(), request.chapterId()), rag.promptContext());
        Long runId = request.novelId() != null && request.chapterId() != null
                ? telemetry.startRun(request.novelId(), request.chapterId(), "INLINE_COMPLETION", instruction,
                properties.configured() ? "ai" : "demo", properties.configured() ? properties.model() : "demo")
                : null;
        try {
            if (!properties.configured()) {
                String content = limitText(createDemoCompletion(context), maxLength);
                onTextDelta.accept(content);
                if (runId != null) {
                    telemetry.recordCall(runId, "INLINE_COMPLETION", "demo", "COMPLETED",
                            0, 0, 0, elapsed(started), 0, null);
                    telemetry.finishRun(runId, "INLINE_COMPLETION_READY", elapsed(started));
                }
                return new CompletionResponse(content, "demo", "demo", 0, 0, 0,
                        elapsed(started), rag.debugInfo());
            }

            ProviderResult result = callProviderStreaming(context, afterContext, maxLength, instruction,
                    memoryContext, characterContext.promptContext(), organizationContext.promptContext(),
                    stylePrompt, onTextDelta);
            if (runId != null) {
                telemetry.recordCall(runId, "INLINE_COMPLETION", properties.model(), "COMPLETED",
                        result.promptTokens(), result.completionTokens(), result.totalTokens(),
                        result.durationMs(), 0, null);
                telemetry.finishRun(runId, "INLINE_COMPLETION_READY", elapsed(started));
            }
            return new CompletionResponse(result.content(), "ai", properties.model(), result.promptTokens(),
                    result.completionTokens(), result.totalTokens(), elapsed(started), rag.debugInfo());
        } catch (RuntimeException error) {
            if (runId != null) {
                telemetry.recordCall(runId, "INLINE_COMPLETION", properties.model(), "FAILED",
                        0, 0, 0, elapsed(started), 0, error.getMessage());
                telemetry.failRun(runId, "INLINE_COMPLETION", elapsed(started), error.getMessage());
            }
            throw error;
        }
    }

    private ProviderResult callProvider(String context, String afterContext, int maxLength, String instruction,
                                        String memoryContext, String characterContext, String organizationContext,
                                        String stylePrompt, boolean inlineCompletion) {
        long started = System.nanoTime();
        try {
            Map<String, Object> payload = Map.of(
                    "model", properties.model(),
                    "temperature", inlineCompletion ? Math.min(0.55, properties.temperature()) : properties.temperature(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt(maxLength, stylePrompt, inlineCompletion)),
                            Map.of("role", "user", "content", userPrompt(
                                    context, afterContext, instruction, memoryContext, characterContext,
                                    organizationContext, inlineCompletion))
                    )
            );

            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.chatCompletionsUrl()))
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException("AI服务调用失败，状态码：" + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractMessageContent(root.path("choices").path(0).path("message").path("content")).strip();
            if (content.isBlank()) {
                throw new AiProviderException("AI服务没有返回续写内容");
            }
            JsonNode usage = root.path("usage");
            return new ProviderResult(content, usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0), usage.path("total_tokens").asInt(0), elapsed(started));
        } catch (AiProviderException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI续写请求已取消", exception);
        } catch (Exception exception) {
            throw new AiProviderException("暂时无法连接AI服务", exception);
        }
    }

    private ProviderResult callProviderStreaming(String context, String afterContext, int maxLength,
                                                 String instruction, String memoryContext,
                                                 String characterContext, String organizationContext,
                                                 String stylePrompt, Consumer<String> onTextDelta) {
        long started = System.nanoTime();
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        StringBuilder content = new StringBuilder();
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", properties.model());
            payload.put("temperature", Math.min(0.55, properties.temperature()));
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt(maxLength, stylePrompt, true)),
                    Map.of("role", "user", "content", userPrompt(context, afterContext, instruction,
                            memoryContext, characterContext, organizationContext, true))));
            payload.put("thinking", Map.of("type", "disabled"));
            payload.put("max_tokens", Math.max(160, maxLength * 2));
            payload.put("stream", true);
            payload.put("stream_options", Map.of("include_usage", true));

            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.chatCompletionsUrl()))
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new AiProviderException("AI服务调用失败，状态码：" + response.statusCode());
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while (content.length() < maxLength && (line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if (data.isBlank() || "[DONE]".equals(data)) continue;
                    JsonNode chunk = objectMapper.readTree(data);
                    JsonNode usage = chunk.path("usage");
                    if (!usage.isMissingNode() && !usage.isNull()) {
                        promptTokens = usage.path("prompt_tokens").asInt(promptTokens);
                        completionTokens = usage.path("completion_tokens").asInt(completionTokens);
                        totalTokens = usage.path("total_tokens").asInt(totalTokens);
                    }
                    String delta = extractMessageContent(
                            chunk.path("choices").path(0).path("delta").path("content"));
                    if (delta.isEmpty()) continue;
                    String accepted = limitText(delta, maxLength - content.length());
                    if (accepted.isEmpty()) break;
                    content.append(accepted);
                    onTextDelta.accept(accepted);
                }
            }
            if (content.toString().isBlank()) throw new AiProviderException("AI服务没有返回续写内容");
            return new ProviderResult(content.toString(), promptTokens, completionTokens, totalTokens,
                    elapsed(started));
        } catch (AiProviderException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI自动补全请求已取消", exception);
        } catch (Exception exception) {
            throw new AiProviderException("AI自动补全流式连接中断，请重试", exception);
        }
    }

    private String extractMessageContent(JsonNode contentNode) {
        if (contentNode.isTextual()) return contentNode.asText();
        if (!contentNode.isArray()) return "";
        StringBuilder content = new StringBuilder();
        for (JsonNode part : contentNode) {
            if (part.isTextual()) content.append(part.asText());
            else if (part.path("text").isTextual()) content.append(part.path("text").asText());
            else if (part.path("content").isTextual()) content.append(part.path("content").asText());
        }
        return content.toString();
    }

    private long elapsed(long started) { return (System.nanoTime() - started) / 1_000_000; }

    private record ProviderResult(String content, int promptTokens, int completionTokens,
                                  int totalTokens, long durationMs) {}

    private String systemPrompt(int maxLength, String stylePrompt, boolean inlineCompletion) {
        String task = inlineCompletion ? """
                你是小说正文编辑器的中间填充（Fill-in-the-Middle）模型。输入中会用 <AI_INSERT_HERE> 标出唯一需要补写的位置。
                你只能生成插入 <AI_INSERT_HERE> 的文字，不是在整段输入之后继续写作。
                补全应短小克制，优先完成当前句子或自然延伸一个短段落，不要一次推进过多剧情。
                如果标记后存在正文，那些内容是已经写好的固定后文：补全必须自然衔接它，不得复述它，也不得写后文之后才发生的情节。
                """ : "你是一名网络小说续写助手。用户会提供光标前的小说正文。\n";
        return stylePrompt + "\n\n" + task + """
                请直接续写紧接在后面的内容，并严格遵守：
                1. 只输出小说正文，不解释、不加标题、不使用Markdown。
                2. 延续原文人物、视角、语气、时态和情节。
                3. 不重复或总结原文，不突然结束故事。
                4. 内容自然形成一个小段落，长度不超过%d个汉字。
                5. 避免用“接下来”“未完待续”等元叙事措辞。
                6. 不沿用其他故事的人名；上下文和作者要求没有提供姓名时，不要擅自给主角添加姓名。
                7. 如果提供“历史记忆”，它只用于保持人物、伏笔和世界观连续性；若与当前正文冲突，以当前正文为准。
                8. 不要照抄历史记忆，也不要在正文中提及“记忆”“资料”或章节编号。
                """.formatted(maxLength);
    }

    private String userPrompt(String context, String afterContext, String instruction, String memoryContext,
                              String characterContext, String organizationContext, boolean inlineCompletion) {
        StringBuilder prompt = new StringBuilder();
        if (characterContext != null && !characterContext.isBlank()) {
            prompt.append("当前片段涉及的人物档案：\n").append(characterContext).append("\n\n");
        }
        if (organizationContext != null && !organizationContext.isBlank()) {
            prompt.append("当前片段涉及的组织档案：\n").append(organizationContext).append("\n\n");
        }
        if (memoryContext != null && !memoryContext.isBlank()) {
            prompt.append("与当前情节相关的历史记忆：\n").append(memoryContext).append("\n\n");
        }
        if (inlineCompletion) {
            prompt.append("待补全的正文：\n<BEFORE_CURSOR>\n")
                    .append(context)
                    .append("\n</BEFORE_CURSOR>\n<AI_INSERT_HERE>\n<AFTER_CURSOR>\n")
                    .append(afterContext == null || afterContext.isBlank() ? "（光标位于正文末尾）" : afterContext)
                    .append("\n</AFTER_CURSOR>");
            if (!instruction.isBlank()) prompt.append("\n\n作者的补全要求：\n").append(instruction);
            prompt.append("\n\n现在只输出应当替换 <AI_INSERT_HERE> 的小说正文，最多100字；"
                    + "绝对不要输出 BEFORE_CURSOR、AFTER_CURSOR 中已有的文字，也不要续写 AFTER_CURSOR 之后的情节。");
        } else {
            prompt.append("当前小说正文：\n").append(context);
            if (!instruction.isBlank()) prompt.append("\n\n作者的续写要求：\n").append(instruction);
        }
        return prompt.toString();
    }

    private String keepTail(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(text.length() - maxLength);
    }

    private String joinContext(String first, String second) {
        if (first == null || first.isBlank()) return second == null ? "" : second;
        if (second == null || second.isBlank()) return first;
        return first + "\n" + second;
    }

    private String limitText(String text, int maxLength) {
        if (maxLength <= 0 || text == null || text.isEmpty()) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String createDemoCompletion(String context) {
        if (context.endsWith("。") || context.endsWith("！") || context.endsWith("？")) {
            return "片刻之后，远处传来一阵细碎的脚步声。那声音越来越近，却在门外忽然停住，仿佛来人也在等待屋里的人先开口。";
        }
        return "，可话到嘴边，又忽然停了下来。窗外的风掠过檐角，带来一种此前从未出现过的、近乎不祥的安静。";
    }
}
