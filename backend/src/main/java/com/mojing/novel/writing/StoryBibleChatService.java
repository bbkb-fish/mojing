package com.mojing.novel.writing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.qdrant.QdrantMemoryService;
import com.mojing.novel.qdrant.VectorMemoryDtos.MemorySearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.mojing.novel.writing.AgentDtos.*;

@Service
public class StoryBibleChatService {
    private static final int HISTORY_LIMIT = 16;
    private static final Logger log = LoggerFactory.getLogger(StoryBibleChatService.class);
    private final NovelRepository novelRepository;
    private final StoryCharacterRepository characterRepository;
    private final StoryOrganizationRepository organizationRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final StoryVolumeRepository volumeRepository;
    private final NovelProjectProfileRepository projectProfileRepository;
    private final StoryBibleChatSessionRepository sessionRepository;
    private final StoryBibleChatMessageRepository messageRepository;
    private final StoryBibleMessageReferenceRepository referenceRepository;
    private final StoryBibleService storyBibleService;
    private final NovelAgentService agentService;
    private final ObjectMapper objectMapper;
    private final QdrantMemoryService qdrantMemoryService;

    public StoryBibleChatService(NovelRepository novelRepository, StoryCharacterRepository characterRepository,
                                 StoryOrganizationRepository organizationRepository,
                                 WorldSettingRepository worldSettingRepository, StoryVolumeRepository volumeRepository,
                                 NovelProjectProfileRepository projectProfileRepository,
                                 StoryBibleChatSessionRepository sessionRepository,
                                 StoryBibleChatMessageRepository messageRepository,
                                 StoryBibleMessageReferenceRepository referenceRepository,
                                 StoryBibleService storyBibleService, NovelAgentService agentService,
                                 ObjectMapper objectMapper, QdrantMemoryService qdrantMemoryService) {
        this.novelRepository = novelRepository;
        this.characterRepository = characterRepository;
        this.organizationRepository = organizationRepository;
        this.worldSettingRepository = worldSettingRepository;
        this.volumeRepository = volumeRepository;
        this.projectProfileRepository = projectProfileRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.referenceRepository = referenceRepository;
        this.storyBibleService = storyBibleService;
        this.agentService = agentService;
        this.objectMapper = objectMapper;
        this.qdrantMemoryService = qdrantMemoryService;
    }

    @Transactional(readOnly = true)
    public StoryBibleChatConversationResponse conversation(long novelId) {
        requireNovel(novelId);
        return sessionRepository.findByNovelId(novelId)
                .map(this::toConversation)
                .orElseGet(() -> new StoryBibleChatConversationResponse(novelId, List.of()));
    }

    @Transactional
    public StoryBibleChatConversationResponse chat(long novelId, StoryBibleChatRequest request) {
        NovelEntity novel = requireNovel(novelId);
        StoryBibleChatSessionEntity session = sessionRepository.findByNovelId(novelId).orElseGet(() -> {
            StoryBibleChatSessionEntity created = new StoryBibleChatSessionEntity();
            created.setNovelId(novelId);
            return sessionRepository.saveAndFlush(created);
        });
        List<StoryBibleChatMessageEntity> previous = messageRepository.findBySessionIdOrderByIdAsc(session.getId());
        backfillReferences(novelId, previous);
        List<EntityReference> currentReferences = detectReferences(novelId, request.normalizedMessage(),
                request.focusType(), request.focusId());
        RecallResult recalled = recall(novelId, previous, currentReferences, request.normalizedMessage());

        StoryBibleChatMessageEntity userMessage = new StoryBibleChatMessageEntity();
        userMessage.setSessionId(session.getId());
        userMessage.setRole("USER");
        userMessage.setContent(request.normalizedMessage());
        userMessage = messageRepository.saveAndFlush(userMessage);
        saveReferences(userMessage.getId(), currentReferences);

        StoryBibleChatTurnOutput turn = agentService.storyBibleChatTurn(novel, buildContext(novelId, request),
                recalled.prompt() + buildHistory(previous), request.normalizedMessage());
        StoryBibleChatMessageEntity assistant = new StoryBibleChatMessageEntity();
        assistant.setSessionId(session.getId());
        assistant.setRole("ASSISTANT");
        assistant.setContent(turn.reply().trim());
        try {
            assistant.setSuggestionsJson(objectMapper.writeValueAsString(turn.suggestions()));
            assistant.setAppliedIndexesJson("[]");
            assistant.setRecalledMessageIdsJson(objectMapper.writeValueAsString(recalled.messageIds()));
        } catch (Exception error) {
            throw new WritingConflictException("资料库顾问建议保存失败，请重试");
        }
        assistant = messageRepository.saveAndFlush(assistant);
        List<EntityReference> assistantReferences = new ArrayList<>(currentReferences);
        assistantReferences.addAll(detectReferences(novelId, turn.reply(), null, null));
        for (StoryBibleSuggestion suggestion : turn.suggestions()) {
            if (suggestion.targetId() != null)
                assistantReferences.add(new EntityReference(suggestion.type(), suggestion.targetId(), "SUGGESTION"));
        }
        saveReferences(assistant.getId(), assistantReferences);
        vectorize(novelId, userMessage);
        vectorize(novelId, assistant);
        return toConversation(session);
    }

    @Transactional
    public StoryBibleApplySuggestionResponse apply(long messageId, int suggestionIndex) {
        StoryBibleChatMessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new WritingNotFoundException("资料库顾问消息不存在：" + messageId));
        if (!"ASSISTANT".equals(message.getRole())) throw new WritingConflictException("只能应用 AI 提出的资料建议");
        StoryBibleChatSessionEntity session = sessionRepository.findById(message.getSessionId())
                .orElseThrow(() -> new WritingNotFoundException("资料库顾问会话不存在"));
        List<StoryBibleSuggestion> suggestions = readSuggestions(message.getSuggestionsJson());
        if (suggestionIndex < 0 || suggestionIndex >= suggestions.size())
            throw new WritingConflictException("资料建议不存在或已经失效");
        Set<Integer> applied = new LinkedHashSet<>(readAppliedIndexes(message.getAppliedIndexesJson()));
        if (applied.contains(suggestionIndex)) throw new WritingConflictException("这条资料建议已经应用");

        StoryBibleSuggestion suggestion = suggestions.get(suggestionIndex);
        Object result = applySuggestion(session.getNovelId(), suggestion);
        Long resultId = resultId(result);
        if (resultId != null)
            saveReferences(message.getId(), List.of(new EntityReference(suggestion.type(), resultId, "APPLIED")));
        applied.add(suggestionIndex);
        try { message.setAppliedIndexesJson(objectMapper.writeValueAsString(applied)); }
        catch (Exception error) { throw new WritingConflictException("资料建议应用状态保存失败"); }
        messageRepository.saveAndFlush(message);
        return new StoryBibleApplySuggestionResponse(suggestionIndex, suggestion.type(), result, toConversation(session));
    }

    private Object applySuggestion(long novelId, StoryBibleSuggestion suggestion) {
        if (suggestion == null || suggestion.type() == null)
            throw new WritingConflictException("AI 返回的资料建议类型不完整");
        if (!Set.of("CREATE", "UPDATE").contains(suggestion.action()))
            throw new WritingConflictException("AI 返回的资料建议操作不受支持");
        return switch (suggestion.type()) {
            case "CHARACTER" -> "CREATE".equals(suggestion.action()) ? createCharacter(novelId, suggestion) : updateCharacter(novelId, suggestion);
            case "ORGANIZATION" -> applyOrganization(novelId, suggestion);
            case "WORLD_SETTING" -> applyWorldSetting(novelId, suggestion);
            case "VOLUME" -> applyVolume(novelId, suggestion);
            default -> throw new WritingConflictException("不支持的资料建议类型");
        };
    }

    private CharacterResponse createCharacter(long novelId, StoryBibleSuggestion suggestion) {
        String name = required(suggestion.name(), "人物姓名", 100);
        if (characterRepository.findFirstByNovelIdAndNameIgnoreCase(novelId, name).isPresent())
            throw new WritingConflictException("人物“" + name + "”已经存在，请让 AI 改为修改人物或更换姓名");
        return storyBibleService.createCharacter(novelId, request(suggestion, null));
    }

    private CharacterResponse updateCharacter(long novelId, StoryBibleSuggestion suggestion) {
        if (suggestion.targetId() == null) throw new WritingConflictException("AI 没有指出需要修改的人物");
        StoryCharacterEntity current = characterRepository.findById(suggestion.targetId())
                .filter(item -> Objects.equals(item.getNovelId(), novelId) && "ACTIVE".equals(item.getStatus()))
                .orElseThrow(() -> new WritingNotFoundException("需要修改的人物不存在或不属于当前作品"));
        return storyBibleService.updateCharacter(current.getId(), new CharacterRequest(
                fit(value(suggestion.name(), current.getName()), 100), fit(value(suggestion.aliases(), current.getAliases()), 500),
                fit(value(suggestion.role(), current.getRole()), 100), fit(value(suggestion.affiliations(), current.getAffiliations()), 500),
                fit(value(suggestion.description(), current.getDescription()), 5_000),
                fit(value(suggestion.personality(), current.getPersonality()), 5_000), fit(value(suggestion.goal(), current.getGoal()), 5_000),
                fit(value(suggestion.currentState(), current.getCurrentState()), 5_000),
                fit(value(suggestion.relationships(), current.getRelationships()), 5_000), current.getVersion()));
    }

    private CharacterRequest request(StoryBibleSuggestion suggestion, Long version) {
        return new CharacterRequest(fit(suggestion.name(), 100), fit(suggestion.aliases(), 500),
                fit(suggestion.role(), 100), fit(suggestion.affiliations(), 500),
                fit(suggestion.description(), 5_000), fit(suggestion.personality(), 5_000),
                fit(suggestion.goal(), 5_000), fit(suggestion.currentState(), 5_000),
                fit(suggestion.relationships(), 5_000), version);
    }

    private OrganizationResponse applyOrganization(long novelId, StoryBibleSuggestion s) {
        if ("CREATE".equals(s.action())) return storyBibleService.createOrganization(novelId,
                new OrganizationRequest(required(s.name(),"组织名称",100), fit(s.aliases(),500), fit(s.role(),100), fit(s.description(),5000),
                        fit(s.goal(),5000), fit(s.structure(),5000), fit(s.relationships(),5000), null));
        requireTargetId(s, "组织");
        StoryOrganizationEntity current = organizationRepository.findById(s.targetId())
                .filter(item -> Objects.equals(item.getNovelId(), novelId))
                .orElseThrow(() -> new WritingNotFoundException("需要修改的组织不存在"));
        return storyBibleService.updateOrganization(current.getId(), new OrganizationRequest(
                fit(value(s.name(),current.getName()),100), fit(value(s.aliases(),current.getAliases()),500),
                fit(value(s.role(),current.getType()),100), fit(value(s.description(),current.getDescription()),5000),
                fit(value(s.goal(),current.getGoal()),5000), fit(value(s.structure(),current.getStructure()),5000),
                fit(value(s.relationships(),current.getRelationships()),5000), current.getVersion()));
    }

    private WorldSettingResponse applyWorldSetting(long novelId, StoryBibleSuggestion s) {
        if ("CREATE".equals(s.action())) return storyBibleService.createWorldSetting(novelId,
                new WorldSettingRequest(worldSettingCategory(s.category()), required(s.title(),"设定标题",200),
                        required(s.content(),"设定内容",10000), null));
        requireTargetId(s, "世界观设定");
        WorldSettingEntity current = worldSettingRepository.findById(s.targetId())
                .filter(item -> Objects.equals(item.getNovelId(), novelId))
                .orElseThrow(() -> new WritingNotFoundException("需要修改的世界设定不存在"));
        return storyBibleService.updateWorldSetting(current.getId(), new WorldSettingRequest(
                fit(value(s.category(),current.getCategory()),50), fit(value(s.title(),current.getTitle()),200),
                fit(value(s.content(),current.getContent()),10000), current.getVersion()));
    }

    private StoryVolumeResponse applyVolume(long novelId, StoryBibleSuggestion s) {
        if ("CREATE".equals(s.action())) {
            if (s.volumeNo() == null || s.volumeNo() < 1) throw new WritingConflictException("AI 生成的分卷缺少有效卷号");
            if (s.chapterStart() == null || s.chapterStart() < 1) throw new WritingConflictException("AI 生成的分卷缺少有效起始章节");
            return storyBibleService.createStoryVolume(novelId, new StoryVolumeRequest(
                    s.volumeNo(), required(s.title(),"分卷标题",200), s.chapterStart(), s.chapterEnd(), fit(s.objective(),5000), null,
                    fit(s.futurePlan(),30000), fit(s.keyTurningPoints(),10000), fit(s.climax(),5000),
                    fit(s.endingHook(),5000), fit(s.foreshadows(),10000), null, null, "GENERATED", null));
        }
        requireTargetId(s, "分卷");
        StoryVolumeEntity c = volumeRepository.findById(s.targetId()).filter(item -> Objects.equals(item.getNovelId(),novelId))
                .orElseThrow(() -> new WritingNotFoundException("需要修改的分卷不存在"));
        return storyBibleService.updateStoryVolume(c.getId(), new StoryVolumeRequest(
                s.volumeNo()==null?c.getVolumeNo():s.volumeNo(), fit(value(s.title(),c.getTitle()),200),
                s.chapterStart()==null?c.getChapterStart():s.chapterStart(), s.chapterEnd()==null?c.getChapterEnd():s.chapterEnd(),
                fit(value(s.objective(),c.getObjective()),5000), c.getRetrospective(), fit(value(s.futurePlan(),c.getFuturePlan()),30000),
                fit(value(s.keyTurningPoints(),c.getKeyTurningPoints()),10000), fit(value(s.climax(),c.getClimax()),5000),
                fit(value(s.endingHook(),c.getEndingHook()),5000), fit(value(s.foreshadows(),c.getForeshadows()),10000),
                c.getLockedBeats(), c.getAnalyzedThroughChapterNo(), "GENERATED", c.getVersion()));
    }

    private String value(String suggested, String current) { return suggested == null ? current : suggested; }
    private void requireTargetId(StoryBibleSuggestion suggestion, String label) {
        if (suggestion.targetId() == null) throw new WritingConflictException("AI 没有指出需要修改的" + label);
    }
    private String required(String value, String label, int max) {
        String normalized = fit(value, max);
        if (normalized == null || normalized.isBlank()) throw new WritingConflictException("AI 生成的资料缺少" + label);
        return normalized;
    }
    private String worldSettingCategory(String category) {
        String normalized = fit(category, 50);
        return normalized == null || normalized.isBlank() ? "其他" : normalized;
    }
    private Long resultId(Object result) {
        if (result instanceof CharacterResponse item) return item.id();
        if (result instanceof OrganizationResponse item) return item.id();
        if (result instanceof WorldSettingResponse item) return item.id();
        if (result instanceof StoryVolumeResponse item) return item.id();
        return null;
    }
    private String fit(String value, int max) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private String buildContext(long novelId, StoryBibleChatRequest request) {
        StringBuilder text = new StringBuilder();
        appendFocus(text, novelId, request.focusType(), request.focusId());
        projectProfileRepository.findByNovelId(novelId).ifPresent(profile -> text.append("\n【立项约束】\n")
                .append("题材：").append(orEmpty(profile.getGenre())).append("｜频道：").append(orEmpty(profile.getChannel()))
                .append("｜平台：").append(orEmpty(profile.getPlatform())).append('\n')
                .append("核心卖点：").append(orEmpty(profile.getCoreSellingPoint())).append('\n')
                .append("主角金手指与限制：").append(orEmpty(profile.getProtagonistHook())).append('\n')
                .append("升级路线：").append(orEmpty(profile.getGrowthRoute())).append('\n')
                .append("读者期待：").append(orEmpty(profile.getReaderExpectations())).append('\n'));
        text.append("\n【人物档案】\n");
        for (StoryCharacterEntity item : characterRepository.findByNovelIdAndStatusOrderByIdAsc(novelId, "ACTIVE")) {
            text.append("- ID ").append(item.getId()).append("｜").append(item.getName())
                    .append("｜定位：").append(orEmpty(item.getRole()))
                    .append("｜组织：").append(orEmpty(item.getAffiliations()))
                    .append("｜经历：").append(orEmpty(item.getDescription()))
                    .append("｜性格：").append(orEmpty(item.getPersonality()))
                    .append("｜目标：").append(orEmpty(item.getGoal()))
                    .append("｜状态：").append(orEmpty(item.getCurrentState()))
                    .append("｜关系：").append(orEmpty(item.getRelationships())).append('\n');
            if (text.length() > 18_000) break;
        }
        text.append("\n【组织档案】\n");
        for (StoryOrganizationEntity item : organizationRepository.findByNovelIdOrderByIdAsc(novelId)) {
            text.append("- ID ").append(item.getId()).append("｜").append(item.getName())
                    .append("｜类型：").append(orEmpty(item.getType())).append("｜目标：").append(orEmpty(item.getGoal()))
                    .append("｜结构：").append(orEmpty(item.getStructure()))
                    .append("｜关系：").append(orEmpty(item.getRelationships())).append('\n');
            if (text.length() > 24_000) break;
        }
        text.append("\n【分卷主线与已写事实】\n");
        for (StoryVolumeEntity item : volumeRepository.findByNovelIdOrderByVolumeNoAsc(novelId)) {
            text.append("- ID ").append(item.getId()).append("｜第").append(item.getVolumeNo()).append("卷《")
                    .append(item.getTitle()).append("》｜范围：第").append(item.getChapterStart()).append('-')
                    .append(item.getChapterEnd() == null ? "?" : item.getChapterEnd()).append("章")
                    .append("｜目标：").append(orEmpty(item.getObjective()))
                    .append("｜已写：").append(orEmpty(item.getRetrospective()))
                    .append("｜章节事实：").append(StoryVolumeContextService.readableChapterFacts(item.getChapterFactsJson()))
                    .append("｜后续：").append(orEmpty(item.getFuturePlan())).append('\n');
            if (text.length() > 34_000) break;
        }
        text.append("\n【世界观设定】\n");
        for (WorldSettingEntity item : worldSettingRepository.findByNovelIdOrderByIdAsc(novelId)) {
            text.append("- ID ").append(item.getId()).append("｜[").append(item.getCategory()).append("] ")
                    .append(item.getTitle()).append("：").append(item.getContent()).append('\n');
            if (text.length() > 40_000) break;
        }
        return limit(text.toString(), 40_000);
    }

    private void appendFocus(StringBuilder text, long novelId, String type, Long id) {
        if (type == null || id == null) { text.append("【当前关注】整本作品"); return; }
        String label = switch (type) {
            case "CHARACTER" -> characterRepository.findById(id).filter(item -> item.getNovelId() == novelId)
                    .map(StoryCharacterEntity::getName).orElse("人物已不存在");
            case "ORGANIZATION" -> organizationRepository.findById(id).filter(item -> item.getNovelId() == novelId)
                    .map(StoryOrganizationEntity::getName).orElse("组织已不存在");
            case "WORLD_SETTING" -> worldSettingRepository.findById(id).filter(item -> item.getNovelId() == novelId)
                    .map(WorldSettingEntity::getTitle).orElse("设定已不存在");
            case "VOLUME" -> volumeRepository.findById(id).filter(item -> item.getNovelId() == novelId)
                    .map(item -> "第" + item.getVolumeNo() + "卷《" + item.getTitle() + "》").orElse("分卷已不存在");
            default -> "整本作品";
        };
        text.append("【当前关注】").append(type).append("：").append(label).append("（ID ").append(id).append("）");
    }

    private String buildHistory(List<StoryBibleChatMessageEntity> messages) {
        int from = Math.max(0, messages.size() - HISTORY_LIMIT);
        StringBuilder text = new StringBuilder();
        for (StoryBibleChatMessageEntity item : messages.subList(from, messages.size())) {
            text.append("USER".equals(item.getRole()) ? "作者：" : "资料库顾问：")
                    .append(item.getContent()).append("\n");
        }
        return limit(text.toString(), 12_000);
    }

    private StoryBibleChatConversationResponse toConversation(StoryBibleChatSessionEntity session) {
        List<StoryBibleChatMessageResponse> messages = messageRepository.findBySessionIdOrderByIdAsc(session.getId())
                .stream().map(this::toMessage).toList();
        return new StoryBibleChatConversationResponse(session.getNovelId(), messages);
    }

    private StoryBibleChatMessageResponse toMessage(StoryBibleChatMessageEntity item) {
        return new StoryBibleChatMessageResponse(item.getId(), item.getRole(), item.getContent(),
                readSuggestions(item.getSuggestionsJson()), readAppliedIndexes(item.getAppliedIndexesJson()),
                readLongs(item.getRecalledMessageIdsJson()),
                item.getCreatedAt());
    }

    private List<StoryBibleSuggestion> readSuggestions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<StoryBibleSuggestion> values = objectMapper.readValue(json, new TypeReference<List<StoryBibleSuggestion>>() {});
            return values.stream().map(item -> item.type() == null ? new StoryBibleSuggestion("CHARACTER", item.action(), item.targetId(),
                    item.name(), item.aliases(), item.role(), item.affiliations(), item.description(), item.personality(), item.goal(),
                    item.currentState(), item.relationships(), item.structure(), item.category(), item.title(), item.content(),
                    item.volumeNo(), item.chapterStart(), item.chapterEnd(), item.objective(), item.futurePlan(), item.keyTurningPoints(),
                    item.climax(), item.endingHook(), item.foreshadows(), item.rationale()) : item).toList();
        }
        catch (Exception ignored) { return List.of(); }
    }

    private List<Integer> readAppliedIndexes(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<List<Integer>>() {}); }
        catch (Exception ignored) { return List.of(); }
    }

    private List<Long> readLongs(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<List<Long>>() {}); }
        catch (Exception ignored) { return List.of(); }
    }

    private List<EntityReference> detectReferences(long novelId, String text, String focusType, Long focusId) {
        Map<String, EntityReference> found = new LinkedHashMap<>();
        if (focusType != null && focusId != null)
            found.put(focusType + ':' + focusId, new EntityReference(focusType, focusId, "FOCUS"));
        String signal = text == null ? "" : text.toLowerCase();
        for (StoryCharacterEntity item : characterRepository.findByNovelIdAndStatusOrderByIdAsc(novelId, "ACTIVE")) {
            if (mentioned(signal, item.getName(), item.getAliases()))
                found.put("CHARACTER:" + item.getId(), new EntityReference("CHARACTER", item.getId(), "MENTION"));
        }
        for (StoryOrganizationEntity item : organizationRepository.findByNovelIdOrderByIdAsc(novelId)) {
            if (mentioned(signal, item.getName(), item.getAliases()))
                found.put("ORGANIZATION:" + item.getId(), new EntityReference("ORGANIZATION", item.getId(), "MENTION"));
        }
        for (WorldSettingEntity item : worldSettingRepository.findByNovelIdOrderByIdAsc(novelId)) {
            if (mentioned(signal, item.getTitle(), null))
                found.put("WORLD_SETTING:" + item.getId(), new EntityReference("WORLD_SETTING", item.getId(), "MENTION"));
        }
        for (StoryVolumeEntity item : volumeRepository.findByNovelIdOrderByVolumeNoAsc(novelId)) {
            if (mentioned(signal, item.getTitle(), "第" + item.getVolumeNo() + "卷"))
                found.put("VOLUME:" + item.getId(), new EntityReference("VOLUME", item.getId(), "MENTION"));
        }
        return new ArrayList<>(found.values());
    }

    private boolean mentioned(String signal, String primary, String aliases) {
        if (containsName(signal, primary)) return true;
        if (aliases == null || aliases.isBlank()) return false;
        for (String alias : aliases.split("[，,、/；;\\s]+")) {
            if (containsName(signal, alias)) return true;
        }
        return false;
    }

    private boolean containsName(String signal, String name) {
        return name != null && !name.isBlank() && signal.contains(name.trim().toLowerCase());
    }

    private void saveReferences(Long messageId, List<EntityReference> references) {
        if (messageId == null || references == null || references.isEmpty()) return;
        Set<String> existing = referenceRepository.findByMessageIdIn(List.of(messageId)).stream()
                .map(item -> item.getEntityType() + ':' + item.getEntityId()).collect(java.util.stream.Collectors.toSet());
        List<StoryBibleMessageReferenceEntity> entities = new ArrayList<>();
        for (EntityReference reference : references) {
            if (!existing.add(reference.type() + ':' + reference.id())) continue;
            StoryBibleMessageReferenceEntity entity = new StoryBibleMessageReferenceEntity();
            entity.setMessageId(messageId); entity.setEntityType(reference.type()); entity.setEntityId(reference.id());
            entity.setReferenceType(reference.referenceType()); entities.add(entity);
        }
        if (!entities.isEmpty()) referenceRepository.saveAllAndFlush(entities);
    }

    private void backfillReferences(long novelId, List<StoryBibleChatMessageEntity> messages) {
        if (messages.isEmpty()) return;
        List<Long> ids = messages.stream().map(StoryBibleChatMessageEntity::getId).filter(Objects::nonNull).toList();
        Set<Long> indexed = referenceRepository.findByMessageIdIn(ids).stream()
                .map(StoryBibleMessageReferenceEntity::getMessageId).collect(java.util.stream.Collectors.toSet());
        for (StoryBibleChatMessageEntity message : messages) {
            if (message.getId() == null || indexed.contains(message.getId())) continue;
            List<EntityReference> detected = new ArrayList<>(detectReferences(novelId, message.getContent(), null, null));
            for (StoryBibleSuggestion suggestion : readSuggestions(message.getSuggestionsJson())) {
                if (suggestion.targetId() != null)
                    detected.add(new EntityReference(suggestion.type(), suggestion.targetId(), "SUGGESTION"));
            }
            saveReferences(message.getId(), detected);
        }
    }

    private RecallResult recall(long novelId, List<StoryBibleChatMessageEntity> previous, List<EntityReference> current,
                                String query) {
        if (previous.size() <= HISTORY_LIMIT) return RecallResult.empty();
        Set<Long> recentIds = previous.subList(previous.size() - HISTORY_LIMIT, previous.size()).stream()
                .map(StoryBibleChatMessageEntity::getId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Set<String>> matchedKeys = new HashMap<>();
        for (EntityReference reference : current) {
            for (StoryBibleMessageReferenceEntity item : referenceRepository
                    .findByEntityTypeAndEntityIdIn(reference.type(), List.of(reference.id()))) {
                if (!recentIds.contains(item.getMessageId()))
                    matchedKeys.computeIfAbsent(item.getMessageId(), ignored -> new LinkedHashSet<>())
                            .add(reference.type() + ':' + reference.id());
            }
        }
        Map<Long, Double> semanticScores = new HashMap<>();
        try {
            for (MemorySearchHit hit : qdrantMemoryService.searchStoryBibleMessages(novelId, query, 8)) {
                if (hit.memoryId() != null && !recentIds.contains(hit.memoryId())) {
                    semanticScores.put(hit.memoryId(), hit.score());
                    matchedKeys.computeIfAbsent(hit.memoryId(), ignored -> new LinkedHashSet<>());
                }
            }
        } catch (RuntimeException error) {
            log.info("资料库语义召回不可用，继续使用本地关联召回 novelId={} reason={}", novelId, error.getMessage());
        }
        if (matchedKeys.isEmpty()) return RecallResult.empty();
        Map<Long, Integer> positions = new HashMap<>();
        for (int index = 0; index < previous.size(); index++) positions.put(previous.get(index).getId(), index);
        List<ScoredMessage> ranked = matchedKeys.entrySet().stream()
                .filter(entry -> positions.containsKey(entry.getKey()))
                .map(entry -> {
                    int position = positions.get(entry.getKey());
                    StoryBibleChatMessageEntity message = previous.get(position);
                    int score = entry.getValue().size() * 10;
                    if (current.size() > 1 && entry.getValue().size() == current.size()) score += 8;
                    score += (int) Math.round(semanticScores.getOrDefault(entry.getKey(), 0D) * 10);
                    score += topicOverlap(query, message.getContent());
                    if (!readAppliedIndexes(message.getAppliedIndexesJson()).isEmpty()) score += 3;
                    score += Math.min(2, position * 3 / Math.max(1, previous.size()));
                    return new ScoredMessage(position, score);
                })
                .sorted(Comparator.comparingInt(ScoredMessage::score).reversed())
                .limit(4).toList();
        LinkedHashSet<Integer> selectedPositions = new LinkedHashSet<>();
        for (ScoredMessage item : ranked) {
            int position = item.position();
            if ("ASSISTANT".equals(previous.get(position).getRole()) && position > 0) selectedPositions.add(position - 1);
            selectedPositions.add(position);
            if ("USER".equals(previous.get(position).getRole()) && position + 1 < previous.size())
                selectedPositions.add(position + 1);
            if (selectedPositions.size() >= 8) break;
        }
        StringBuilder prompt = new StringBuilder("【按人物/组织/分卷召回的早期原始对话】\n");
        List<Long> ids = new ArrayList<>();
        for (Integer position : selectedPositions) {
            StoryBibleChatMessageEntity item = previous.get(position);
            String line = "[消息" + item.getId() + "]" + ("USER".equals(item.getRole()) ? "作者：" : "资料库顾问：")
                    + item.getContent() + "\n";
            if (prompt.length() + line.length() > 8_000) break;
            prompt.append(line); ids.add(item.getId());
        }
        prompt.append("以上内容是原始记录，摘要或旧建议若与正式资料冲突，以正式资料为准。\n\n");
        return new RecallResult(prompt.toString(), ids);
    }

    private void vectorize(long novelId, StoryBibleChatMessageEntity message) {
        if (message.getId() == null || message.getContent() == null || message.getContent().isBlank()) return;
        try {
            qdrantMemoryService.upsertStoryBibleMessage(novelId, message.getId(), message.getContent());
        } catch (RuntimeException error) {
            log.info("资料库对话向量写入不可用，消息仍已保存在本地数据库 novelId={} messageId={} reason={}",
                    novelId, message.getId(), error.getMessage());
        }
    }

    private int topicOverlap(String query, String content) {
        String left = query == null ? "" : query.replaceAll("\\s+", "");
        String right = content == null ? "" : content.replaceAll("\\s+", "");
        int score = 0;
        Set<String> seen = new LinkedHashSet<>();
        for (int index = 0; index + 1 < left.length() && score < 6; index++) {
            String token = left.substring(index, index + 2);
            if (seen.add(token) && right.contains(token)) score++;
        }
        return score;
    }

    private NovelEntity requireNovel(long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new WritingNotFoundException("小说不存在：" + novelId));
    }

    private String orEmpty(String value) { return value == null ? "" : value.replace('\n', ' '); }
    private String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }

    private record EntityReference(String type, Long id, String referenceType) {}
    private record ScoredMessage(int position, int score) {}
    private record RecallResult(String prompt, List<Long> messageIds) {
        private static RecallResult empty() { return new RecallResult("", List.of()); }
    }
}
