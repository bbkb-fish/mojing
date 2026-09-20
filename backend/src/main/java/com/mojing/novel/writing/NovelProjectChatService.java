package com.mojing.novel.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.mojing.novel.writing.AgentDtos.*;

@Service
public class NovelProjectChatService {
    private static final int HISTORY_LIMIT = 20;
    private final NovelRepository novelRepository;
    private final NovelProjectChatSessionRepository sessionRepository;
    private final NovelProjectChatMessageRepository messageRepository;
    private final ChapterRepository chapterRepository;
    private final NovelProjectService projectService;
    private final NovelAgentService agentService;
    private final ObjectMapper objectMapper;

    public NovelProjectChatService(NovelRepository novelRepository,
                                   NovelProjectChatSessionRepository sessionRepository,
                                   NovelProjectChatMessageRepository messageRepository,
                                   ChapterRepository chapterRepository,
                                   NovelProjectService projectService, NovelAgentService agentService,
                                   ObjectMapper objectMapper) {
        this.novelRepository = novelRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.chapterRepository = chapterRepository;
        this.projectService = projectService;
        this.agentService = agentService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public NovelProjectChatConversationResponse conversation(long novelId) {
        requireNovel(novelId);
        return sessionRepository.findByNovelId(novelId).map(this::response)
                .orElseGet(() -> new NovelProjectChatConversationResponse(novelId, List.of(), readMemory(null)));
    }

    @Transactional
    public NovelProjectChatConversationResponse chat(long novelId, NovelProjectChatRequest request) {
        NovelEntity novel = requireNovel(novelId);
        NovelProjectChatSessionEntity session = sessionRepository.findByNovelId(novelId).orElseGet(() -> {
            NovelProjectChatSessionEntity created = new NovelProjectChatSessionEntity();
            created.setNovelId(novelId);
            return sessionRepository.saveAndFlush(created);
        });
        List<NovelProjectChatMessageEntity> previous = messageRepository.findBySessionIdOrderByIdAsc(session.getId());
        NovelProjectResponse source = projectService.get(novelId);
        NovelProjectChatMessageEntity user = new NovelProjectChatMessageEntity();
        user.setSessionId(session.getId()); user.setRole("USER"); user.setContent(request.normalizedMessage());
        user = messageRepository.saveAndFlush(user);

        NovelProjectMemory memory = readMemory(session.getMemoryJson());
        String backfillHistory = session.getMemoryJson() == null ? backfillHistory(previous) : "";

        NovelProjectChatTurnOutput turn = agentService.novelProjectChatTurn(novel, request.current(),
                history(previous), memory, backfillHistory, user.getId(), request.normalizedMessage(),
                request.target(), request.importWrittenStory());
        NovelProjectChatMessageEntity assistant = new NovelProjectChatMessageEntity();
        assistant.setSessionId(session.getId()); assistant.setRole("ASSISTANT"); assistant.setContent(turn.reply().trim());
        assistant.setSourceNovelVersion(source.novelVersion());
        assistant.setSourceProfileVersion(source.profileVersion());
        assistant.setAnalyzedThroughChapterNo(turn.analyzedThroughChapterNo());
        assistant.setSourceContentVersionSum(turn.sourceContentVersionSum());
        assistant.setApplied(false);
        try {
            if (turn.suggestion() != null) assistant.setSuggestionJson(objectMapper.writeValueAsString(turn.suggestion()));
            session.setMemoryJson(objectMapper.writeValueAsString(turn.memory()));
            session.setMemoryUpdatedThroughMessageId(user.getId());
            sessionRepository.saveAndFlush(session);
        } catch (Exception error) {
            throw new WritingConflictException("立项顾问方案保存失败，请重试");
        }
        messageRepository.saveAndFlush(assistant);
        return response(session);
    }

    @Transactional
    public NovelProjectChatApplyResponse apply(long novelId, long messageId) {
        NovelProjectChatMessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new WritingNotFoundException("立项顾问消息不存在：" + messageId));
        if (!"ASSISTANT".equals(message.getRole()) || message.getSuggestionJson() == null)
            throw new WritingConflictException("这条消息没有可应用的立项方案");
        if (Boolean.TRUE.equals(message.getApplied())) throw new WritingConflictException("这份立项方案已经应用");
        NovelProjectChatSessionEntity session = sessionRepository.findById(message.getSessionId())
                .orElseThrow(() -> new WritingNotFoundException("立项顾问会话不存在"));
        if (!Objects.equals(session.getNovelId(), novelId))
            throw new WritingNotFoundException("立项顾问消息不属于当前小说");
        NovelProjectResponse current = projectService.get(session.getNovelId());
        if (!Objects.equals(current.novelVersion(), message.getSourceNovelVersion())
                || !Objects.equals(current.profileVersion(), message.getSourceProfileVersion()))
            throw new WritingConflictException("正式立项资料在方案生成后已经变化，请让 AI 基于最新版重新生成");
        if (message.getSourceContentVersionSum() != null) {
            List<ChapterEntity> writtenChapters = chapterRepository.findByNovelIdOrderByChapterNoAsc(session.getNovelId())
                    .stream()
                    .filter(chapter -> chapter.getContent() != null && !chapter.getContent().isBlank())
                    .toList();
            long currentContentVersionSum = writtenChapters.stream().map(ChapterEntity::getContentVersion)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();
            Integer currentLastChapterNo = writtenChapters.isEmpty() ? null
                    : writtenChapters.get(writtenChapters.size() - 1).getChapterNo();
            if (!Objects.equals(currentContentVersionSum, message.getSourceContentVersionSum())
                    || !Objects.equals(currentLastChapterNo, message.getAnalyzedThroughChapterNo()))
                throw new WritingConflictException("正文在方案生成后已经变化，请让 AI 重新导入已写章节后生成方案");
        }
        try {
            NovelProjectContent suggestion = objectMapper.readValue(message.getSuggestionJson(), NovelProjectContent.class);
            NovelProjectResponse saved = projectService.save(session.getNovelId(),
                    new NovelProjectSaveRequest(suggestion, current.novelVersion(), current.profileVersion()));
            message.setApplied(true);
            messageRepository.saveAndFlush(message);
            return new NovelProjectChatApplyResponse(saved, response(session));
        } catch (WritingConflictException error) {
            throw error;
        } catch (Exception error) {
            throw new WritingConflictException("立项方案格式已经失效，请重新生成");
        }
    }

    private NovelProjectChatConversationResponse response(NovelProjectChatSessionEntity session) {
        return new NovelProjectChatConversationResponse(session.getNovelId(),
                messageRepository.findBySessionIdOrderByIdAsc(session.getId()).stream().map(this::message).toList(),
                readMemory(session.getMemoryJson()));
    }

    private NovelProjectChatMessageResponse message(NovelProjectChatMessageEntity entity) {
        NovelProjectContent suggestion = null;
        if (entity.getSuggestionJson() != null) try {
            suggestion = objectMapper.readValue(entity.getSuggestionJson(), NovelProjectContent.class);
        } catch (Exception ignored) {}
        return new NovelProjectChatMessageResponse(entity.getId(), entity.getRole(), entity.getContent(), suggestion,
                Boolean.TRUE.equals(entity.getApplied()), entity.getAnalyzedThroughChapterNo(), entity.getCreatedAt());
    }

    private String history(List<NovelProjectChatMessageEntity> messages) { return history(messages, 16_000); }

    private String history(List<NovelProjectChatMessageEntity> messages, int budget) {
        int from = budget > 16_000 ? 0 : Math.max(0, messages.size() - HISTORY_LIMIT);
        StringBuilder text = new StringBuilder();
        for (int index = messages.size() - 1; index >= from; index--) {
            NovelProjectChatMessageEntity message = messages.get(index);
            StringBuilder entry = new StringBuilder()
                    .append("USER".equals(message.getRole()) ? "作者：" : "立项顾问：")
                    .append(excerpt(message.getContent(), 2_000)).append('\n');
            if ("ASSISTANT".equals(message.getRole()) && message.getSuggestionJson() != null) {
                entry.append("该轮提出的立项方案（")
                        .append(Boolean.TRUE.equals(message.getApplied()) ? "已应用" : "未应用")
                        .append("）：").append(excerpt(message.getSuggestionJson(), 8_000)).append('\n');
            }
            if (entry.length() + text.length() > budget && !text.isEmpty()) break;
            text.insert(0, entry.substring(0, Math.min(entry.length(), budget - text.length())));
        }
        return text.toString();
    }

    private NovelProjectMemory readMemory(String json) {
        if (json != null && !json.isBlank()) try {
            return objectMapper.readValue(json, NovelProjectMemory.class);
        } catch (Exception ignored) {}
        return new NovelProjectMemory(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private String backfillHistory(List<NovelProjectChatMessageEntity> messages) {
        StringBuilder text = new StringBuilder();
        for (NovelProjectChatMessageEntity message : messages) {
            String entry = "[消息" + message.getId() + "]"
                    + ("USER".equals(message.getRole()) ? "作者：" : "立项顾问：")
                    + excerpt(message.getContent(), 3_000) + '\n';
            if (text.length() + entry.length() > 40_000) break;
            text.append(entry);
        }
        return text.toString();
    }

    private String excerpt(String value, int limit) {
        if (value == null || value.length() <= limit) return value == null ? "" : value;
        int half = limit / 2;
        return value.substring(0, half) + "\n…中间内容已省略…\n" + value.substring(value.length() - half);
    }

    private NovelEntity requireNovel(long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new WritingNotFoundException("小说不存在：" + novelId));
    }
}
