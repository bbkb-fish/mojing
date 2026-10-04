package com.mojing.novel.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.completion.AiProviderException;
import com.mojing.novel.completion.RagContextService;
import com.mojing.novel.config.AiProperties;
import com.mojing.novel.config.AiHttpClientFactory;
import com.mojing.novel.skill.ChapterCompressionSkill;
import com.mojing.novel.style.WritingStyleService;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.mojing.novel.writing.AgentDtos.*;

@Service
public class NovelAgentService {
    private static final int WORLD_SETTING_CONTEXT_LIMIT = 10_000;
    private static final int RECENT_CHAPTER_CONTEXT_LIMIT = 10_000;
    private static final int REWRITE_STORY_CONTEXT_LIMIT = 30_000;
    private static final int DIRECTOR_RAG_DIALOGUE_LIMIT = 4_000;
    private static final int DIRECTOR_RAG_MESSAGE_LIMIT = 12;
    private static final Logger log = LoggerFactory.getLogger(NovelAgentService.class);
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final CharacterContextService characterContextService;
    private final OrganizationContextService organizationContextService;
    private final WorldSettingRepository worldSettingRepository;
    private final StoryVolumeContextService storyVolumeContextService;
    private final AgentRunRepository runRepository;
    private final AgentStepRepository stepRepository;
    private final AiCallLogRepository callRepository;
    private final AgentTelemetryService telemetry;
    private final ChapterRevisionRepository revisionRepository;
    private final ConsistencyReportRepository consistencyReportRepository;
    private final ConsistencyIssueRepository consistencyIssueRepository;
    private final ChapterMemoryRepository chapterMemoryRepository;
    private final NovelProjectProfileRepository projectProfileRepository;
    private final ChapterDirectorSessionRepository directorSessionRepository;
    private final ChapterDirectorMessageRepository directorMessageRepository;
    private final ChapterCompressionSkill chapterCompressionSkill;
    private final ChapterMemoryVectorizer chapterMemoryVectorizer;
    private final CharacterHistoryService characterHistoryService;
    private final CharacterNameResolver characterNameResolver;
    private final RagContextService ragContextService;
    private final WritingStyleService writingStyleService;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public NovelAgentService(NovelRepository novelRepository, ChapterRepository chapterRepository,
                             CharacterContextService characterContextService,
                             OrganizationContextService organizationContextService,
                             WorldSettingRepository worldSettingRepository,
                             StoryVolumeContextService storyVolumeContextService,
                             AgentRunRepository runRepository, AgentStepRepository stepRepository,
                             AiCallLogRepository callRepository, AgentTelemetryService telemetry,
                             ChapterRevisionRepository revisionRepository,
                             ConsistencyReportRepository consistencyReportRepository,
                             ConsistencyIssueRepository consistencyIssueRepository,
                             ChapterMemoryRepository chapterMemoryRepository,
                             NovelProjectProfileRepository projectProfileRepository,
                             ChapterDirectorSessionRepository directorSessionRepository,
                             ChapterDirectorMessageRepository directorMessageRepository,
                             ChapterCompressionSkill chapterCompressionSkill,
                             ChapterMemoryVectorizer chapterMemoryVectorizer,
                             CharacterHistoryService characterHistoryService,
                             CharacterNameResolver characterNameResolver,
                             RagContextService ragContextService,
                             WritingStyleService writingStyleService,
                             AiProperties properties, ObjectMapper objectMapper) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.characterContextService = characterContextService;
        this.organizationContextService = organizationContextService;
        this.worldSettingRepository = worldSettingRepository;
        this.storyVolumeContextService = storyVolumeContextService;
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.callRepository = callRepository;
        this.telemetry = telemetry;
        this.revisionRepository = revisionRepository;
        this.consistencyReportRepository = consistencyReportRepository;
        this.consistencyIssueRepository = consistencyIssueRepository;
        this.chapterMemoryRepository = chapterMemoryRepository;
        this.projectProfileRepository = projectProfileRepository;
        this.directorSessionRepository = directorSessionRepository;
        this.directorMessageRepository = directorMessageRepository;
        this.chapterCompressionSkill = chapterCompressionSkill;
        this.chapterMemoryVectorizer = chapterMemoryVectorizer;
        this.characterHistoryService = characterHistoryService;
        this.characterNameResolver = characterNameResolver;
        this.ragContextService = ragContextService;
        this.writingStyleService = writingStyleService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = AiHttpClientFactory.create(properties);
    }

    @Transactional(readOnly = true)
    public StoryBibleAssistResponse assistStoryBible(long novelId, StoryBibleAssistRequest request) {
        NovelEntity novel = requireNovel(novelId);
        List<String> fieldNames = storyBibleFieldNames(request.type());
        Map<String, String> currentFields = new LinkedHashMap<>();
        for (String fieldName : fieldNames) {
            String value = request.currentFields().get(fieldName);
            currentFields.put(fieldName, value == null ? "" : value.trim());
        }
        List<String> targetFields = request.normalizedTargetFields().stream()
                .filter(fieldNames::contains).distinct().toList();
        if (targetFields.isEmpty()) targetFields = detectStoryBibleTargets(request.type(), request.normalizedInstruction());
        if (targetFields.isEmpty()) targetFields = fieldNames;
        boolean hasCurrentContent = currentFields.values().stream().anyMatch(value -> !value.isBlank());
        if (request.normalizedInstruction().isBlank() && !hasCurrentContent) {
            throw new AiProviderException("请先输入一些关键信息，再让 AI 完善资料");
        }

        log.info("故事资料 AI 辅助开始 novelId={} type={} mode={} targetFields={} instructionChars={} currentChars={}",
                novelId, request.type(), request.mode(), targetFields, request.normalizedInstruction().length(),
                currentFields.values().stream().mapToInt(String::length).sum());
        if (!properties.configured()) {
            return demoStoryBibleAssist(request, currentFields, targetFields);
        }

        CharacterContextService.Selection relatedCharacters = CharacterContextService.Selection.empty();
        OrganizationContextService.Selection relatedOrganizations = OrganizationContextService.Selection.empty();
        if ("CHARACTER".equals(request.type())) {
            String routingSignal = String.join("\n", currentFields.values()) + "\n" + request.normalizedInstruction();
            relatedCharacters = characterContextService.selectRelated(
                    novelId, currentFields.get("name"), routingSignal);
            relatedOrganizations = organizationContextService.select(novelId, routingSignal);
            log.info("人物资料 AI 上下文按需装载 novelId={} character={} relatedCharacters={} relatedOrganizations={}",
                    novelId, currentFields.get("name"), relatedCharacters.characterNames(),
                    relatedOrganizations.organizationNames());
        }

        String schema = switch (request.type()) {
            case "CHARACTER" -> "name, aliases, role, affiliations, description, personality, goal, currentState, relationships";
            case "ORGANIZATION" -> "name, aliases, type, description, goal, structure, relationships";
            case "WORLD_SETTING" -> "category, title, content";
            default -> throw new AiProviderException("不支持的故事资料类型");
        };
        String system = """
                你是网络小说故事资料编辑助手。根据作者提供的关键信息，补全或优化一条故事资料。
                你必须遵守：
                1. 作者明确给出的姓名、身份、关系、能力、时间和世界规则属于不可擅改的事实。
                2. GENERATE 表示根据零散信息生成完整档案；EXPAND 表示保留已有内容并补充细节；POLISH 表示只改善表达，不新增关键设定。
                3. 不要编写小说正文，不要使用 Markdown，不要解释创作过程。
                4. 信息不足时保持克制；不要为了填满字段制造会影响剧情的重大事实。
                5. 只输出一个 JSON 对象，格式为：{"fields":{"字段":"内容"},"changeSummary":["修改摘要"]}。
                6. 当前资料的全部合法字段为：%s，但本次 fields 只能输出这些目标字段：%s。
                7. 禁止输出或改写目标字段以外的内容。所有字段值都必须是字符串。
                8. 相关人物和组织档案只用于保持一致性，不代表允许修改其中的事实，也不要把它们原样复制到当前档案。
                """.formatted(schema, String.join(", ", targetFields));
        String user = "【作品名称】\n" + novel.getTitle()
                + "\n\n【作品简介】\n" + safeText(novel.getDescription(), 1500)
                + "\n\n【故事大纲（仅用于保持一致）】\n" + safeText(novel.getOutline(), 4000)
                + promptSection("已确认立项约束", projectContext(novelId))
                + "\n\n【资料类型】\n" + request.type()
                + "\n\n【操作方式】\n" + request.mode()
                + "\n\n【本次只允许修改的字段】\n" + String.join(", ", targetFields)
                + "\n\n【当前表单】\n" + writeJson(currentFields)
                + promptSection("相关人物档案（按名称命中）", relatedCharacters.promptContext())
                + promptSection("相关组织档案（按名称或别名命中）", relatedOrganizations.promptContext())
                + "\n\n【作者提供的关键信息或额外要求】\n"
                + (request.normalizedInstruction().isBlank() ? "无，请根据当前表单处理" : request.normalizedInstruction());
        String raw = callProvider(null, "ASSIST_STORY_BIBLE", system, user, 0.35, 4096,
                new ProviderOptions(true, false, 4096));
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            JsonNode generatedFields = root.path("fields");
            Map<String, String> fields = new LinkedHashMap<>();
            for (String fieldName : fieldNames) {
                String generated = targetFields.contains(fieldName)
                        ? generatedFields.path(fieldName).asText("").trim() : "";
                String value = generated.isBlank() ? currentFields.get(fieldName) : generated;
                fields.put(fieldName, limitStoryBibleField(request.type(), fieldName, value));
            }
            List<String> summary = firstStringList(root, "changeSummary", "changes", "修改摘要");
            if (summary.isEmpty()) summary = List.of("已根据作者要求完善故事资料");
            log.info("故事资料 AI 辅助完成 novelId={} type={} mode={} targetFields={} outputChars={} changes={}",
                    novelId, request.type(), request.mode(), targetFields,
                    fields.values().stream().mapToInt(String::length).sum(), summary.size());
            return new StoryBibleAssistResponse(request.type(), request.mode(), fields, summary, "ai");
        } catch (AiProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiProviderException("AI 返回的故事资料格式不正确，请重试", exception);
        }
    }

    public StoryBibleChatTurnOutput storyBibleChatTurn(NovelEntity novel, String storyContext,
                                                        String conversationHistory, String message) {
        if (!properties.configured()) return demoStoryBibleChatTurn(message);
        String system = """
                你是长篇小说的“故事资料库顾问”。你要和作者进行多轮讨论，重点分析人物、组织、世界设定、分卷主线和已写剧情事实之间的关系。
                规则：
                1. 已写剧情事实不可推翻；发现冲突时明确指出，不要偷偷改写事实。
                2. 普通讨论只在 reply 中回答。只有作者明确要求新增或修改资料时，才输出 suggestions。
                3. type 只能是 CHARACTER、ORGANIZATION、WORLD_SETTING、VOLUME；action 只能是 CREATE 或 UPDATE。UPDATE 的 targetId 必须使用资料中真实 ID。
                4. UPDATE 必须给出修改后的完整档案；未讨论字段照抄原值，不能清空。分卷不得修改已写事实、章节自动归纳、已分析范围和作者锁定节点，只能提议标题、范围、目标和未来规划等字段。
                5. 设计反派时要说明其欲望、正当性、手段边界、与主角及组织的关系、卷内职责，避免只有“作恶”的扁平人物。
                6. 建议不会直接落库，因此可以提出多个候选，但不要声称已经创建或修改成功。
                7. CREATE 的必填字段：CHARACTER 必须有 name；ORGANIZATION 必须有 name；WORLD_SETTING 必须有 category、title、content，其中 category 应使用“地点、规则、力量体系、历史、种族、文化、物品、其他”等简短分类；VOLUME 必须有 volumeNo、title、chapterStart。
                8. ORGANIZATION 的组织类型写入 role 字段；WORLD_SETTING 不要把分类只写在 title 或 content 中。
                9. 只输出 JSON，不要 Markdown 或额外文本。
                JSON 格式：
                {"reply":"对作者的回复","suggestions":[{"type":"CHARACTER|ORGANIZATION|WORLD_SETTING|VOLUME","action":"CREATE|UPDATE","targetId":null,"name":"人物或组织名","aliases":"","role":"","affiliations":"","description":"","personality":"","goal":"","currentState":"","relationships":"","structure":"","category":"","title":"设定或分卷标题","content":"世界设定内容","volumeNo":null,"chapterStart":null,"chapterEnd":null,"objective":"","futurePlan":"","keyTurningPoints":"","climax":"","endingHook":"","foreshadows":"","rationale":"建议理由"}]}
                没有可应用建议时 suggestions 输出空数组。不属于当前类型的字段使用 null。
                """;
        String user = "【作品】\n" + novel.getTitle()
                + "\n【简介】\n" + safeText(novel.getDescription(), 1500)
                + "\n【全书大纲】\n" + safeText(novel.getOutline(), 5000)
                + promptSection("已确认立项约束", projectContext(novel.getId()))
                + "\n\n" + storyContext
                + "\n\n【最近对话】\n" + (conversationHistory.isBlank() ? "尚无历史对话" : conversationHistory)
                + "\n\n【作者本轮消息】\n" + message;
        String raw = callProvider(null, "STORY_BIBLE_CHAT", system, user, 0.65, 8192,
                new ProviderOptions(true, false, 8192));
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            String reply = root.path("reply").asText("").trim();
            if (reply.isBlank()) throw new AiProviderException("AI 没有返回资料库讨论内容，请重试");
            List<StoryBibleSuggestion> suggestions = new ArrayList<>();
            JsonNode nodes = root.path("suggestions");
            if (nodes.isArray()) {
                for (JsonNode node : nodes) {
                    StoryBibleSuggestion item = objectMapper.treeToValue(node, StoryBibleSuggestion.class);
                    if (item != null && Set.of("CHARACTER", "ORGANIZATION", "WORLD_SETTING", "VOLUME").contains(item.type())
                            && ("CREATE".equals(item.action()) || "UPDATE".equals(item.action())))
                        suggestions.add(item);
                    if (suggestions.size() >= 8) break;
                }
            }
            return new StoryBibleChatTurnOutput(reply, suggestions);
        } catch (AiProviderException error) {
            throw error;
        } catch (Exception error) {
            throw new AiProviderException("AI 返回的资料库建议格式不正确，请重试", error);
        }
    }

    private StoryBibleChatTurnOutput demoStoryBibleChatTurn(String message) {
        if (!message.contains("反派") && !message.contains("敌人") && !message.contains("对手")) {
            return new StoryBibleChatTurnOutput("我已经结合当前人物、组织、设定与分卷资料记下这个方向。你可以继续说明希望关注的关系，或者直接让我设计、修改人物。", List.of());
        }
        List<StoryBibleSuggestion> suggestions = List.of(new StoryBibleSuggestion(
                "CHARACTER", "CREATE", null, "闻烬", "闻先生", "本卷主要反派", "地方权力组织",
                "负责维持本地秩序的实际掌权者。", "克制、讲规则", "建立绝对可控的秩序",
                "正在寻找关键人物", "认可主角能力，但否定主角的自由选择", null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                "拥有可理解的正当目标，适合作为价值观对手"));
        return new StoryBibleChatTurnOutput("我先给出一组互相咬合的反派结构：一名价值观层面的主要反派、一名负责制造前中段压力的行动反派，以及一名后期揭露的隐藏反派。它们目前只是建议卡，确认后才会创建到人物库。", suggestions);
    }

    @Transactional(readOnly = true)
    public NovelProjectGenerateResponse generateNovelProject(long novelId, NovelProjectGenerateRequest request) {
        NovelEntity novel = requireNovel(novelId);
        List<ChapterEntity> writtenChapters = chapterRepository.findByNovelIdOrderByChapterNoAsc(novelId).stream()
                .filter(chapter -> chapter.getContent() != null && !chapter.getContent().isBlank()).toList();
        Integer writtenThrough = writtenChapters.isEmpty() ? null
                : writtenChapters.get(writtenChapters.size() - 1).getChapterNo();
        String writtenFacts = buildVolumeWrittenFacts(novelId, writtenChapters);
        NovelProjectContent current = request.current();
        boolean hasInput = hasText(request.normalizedInstruction()) || hasText(current.inspiration())
                || hasText(current.coreSellingPoint()) || hasText(current.description()) || hasText(current.outline())
                || !writtenChapters.isEmpty();
        if (!hasInput) throw new AiProviderException("请先写下一句灵感，再让 AI 帮你完成立项");

        if (!properties.configured()) {
            String description = hasText(current.description()) ? current.description()
                    : hasText(current.inspiration()) ? current.inspiration().trim() : "请配置 API Key 后生成作品简介";
            String outline = hasText(current.outline()) ? current.outline()
                    : writtenChapters.isEmpty() ? "请配置 API Key 后生成故事总纲"
                    : "已识别已有正文至第" + writtenThrough + "章；配置 API Key 后会在不推翻已写事实的前提下反推总纲。";
            NovelProjectContent demo = copyProject(current, description, outline);
            return new NovelProjectGenerateResponse(demo, writtenThrough,
                    List.of("当前为演示模式，生成结果尚未保存；配置 API Key 后可获得完整立项方案"), "demo");
        }

        String system = """
                你是一名擅长网络小说立项的策划编辑。请把作者的模糊灵感整理为能够长期连载的作品方案。
                必须遵守：
                1. 已写章节是不可推翻的事实。若作品已有正文，只能反推和补全方案，不得为了新大纲要求作者重写已发生剧情。
                2. 立项只需明确题材与基调、主角的独特处境、核心卖点和主线方向。基于现有材料直接给出能开始写作的简明草案，不要求作者提前想清所有设定。
                3. 简介用于吸引读者，突出主角处境、独特能力、核心冲突和悬念，控制在1000字内，不要写编辑说明。
                4. 故事总纲先交代起点、主线目标和大致推进方向；未定的转折与结局可暂留弹性，具体事件留到卷、部分和章纲阶段。
                5. 能力机制、限制、代价、成长阶段和开篇三章只有作者主动要求细化或已有明确材料时才展开；否则保持原值或留空。已有前三章时可概括实际作用，不得另造一套开篇。
                6. target=FOUNDATION 重点完善立项字段；DESCRIPTION 只重新创作简介；OUTLINE 只重新创作故事大纲；ALL 完善全部内容。未作为目标的字段保持原值。
                7. 只输出 JSON，不要 Markdown 或额外文字。格式：
                {"content":{"inspiration":"","genre":"","channel":"","targetAudience":"","platform":"","coreSellingPoint":"","protagonistHook":"","growthRoute":"","readerExpectations":"","openingThreeChapters":"","expectedWords":null,"expectedVolumes":null,"chapterWordTarget":null,"description":"","outline":""},"changeSummary":["本次调整"]}
                """;
        String user = "【作品名称】\n" + novel.getTitle()
                + "\n\n【生成范围】\n" + request.target()
                + "\n\n【当前立项草案】\n" + writeJson(current)
                + "\n\n【已有正文状态】\n" + (writtenThrough == null ? "尚无已写正文" : "已写至第" + writtenThrough + "章")
                + "\n\n【已写章节事实材料】\n" + (writtenFacts.isBlank() ? "无" : writtenFacts)
                + "\n\n【作者本次要求】\n" + (request.normalizedInstruction().isBlank() ? "请基于当前材料生成" : request.normalizedInstruction());
        String raw = callProvider(null, "GENERATE_NOVEL_PROJECT", system, user, 0.55, 10000,
                new ProviderOptions(true, false, 10000));
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            JsonNode node = root.path("content");
            boolean foundation = "FOUNDATION".equals(request.target()) || "ALL".equals(request.target());
            boolean description = "DESCRIPTION".equals(request.target()) || "ALL".equals(request.target());
            boolean outline = "OUTLINE".equals(request.target()) || "ALL".equals(request.target());
            NovelProjectContent generated = new NovelProjectContent(
                    projectText(node, "inspiration", current.inspiration(), 5_000, foundation),
                    projectText(node, "genre", current.genre(), 100, foundation),
                    projectText(node, "channel", current.channel(), 100, foundation),
                    projectText(node, "targetAudience", current.targetAudience(), 500, foundation),
                    projectText(node, "platform", current.platform(), 100, foundation),
                    projectText(node, "coreSellingPoint", current.coreSellingPoint(), 5_000, foundation),
                    projectText(node, "protagonistHook", current.protagonistHook(), 5_000, foundation),
                    projectText(node, "growthRoute", current.growthRoute(), 10_000, foundation),
                    projectText(node, "readerExpectations", current.readerExpectations(), 5_000, foundation),
                    projectText(node, "openingThreeChapters", current.openingThreeChapters(), 10_000, foundation),
                    projectInteger(node, "expectedWords", current.expectedWords(), foundation, 10_000, 10_000_000),
                    projectInteger(node, "expectedVolumes", current.expectedVolumes(), foundation, 1, 100),
                    projectInteger(node, "chapterWordTarget", current.chapterWordTarget(), foundation, 300, 10_000),
                    projectText(node, "description", current.description(), 1_000, description),
                    projectText(node, "outline", current.outline(), 100_000, outline));
            List<String> summary = firstStringList(root, "changeSummary", "changes", "修改摘要");
            if (summary.isEmpty()) summary = List.of(writtenThrough == null
                    ? "已根据灵感生成立项草案" : "已结合现有正文反推立项草案，已写事实保持不变");
            return new NovelProjectGenerateResponse(generated, writtenThrough, summary, "ai");
        } catch (AiProviderException error) {
            throw error;
        } catch (Exception error) {
            throw new AiProviderException("AI 返回的立项方案格式不正确，请重试", error);
        }
    }

    @Transactional(readOnly = true)
    public NovelProjectChatTurnOutput novelProjectChatTurn(NovelEntity novel, NovelProjectContent current,
                                                           String history, NovelProjectMemory currentMemory,
                                                           String backfillHistory, Long sourceMessageId,
                                                           String message, String target,
                                                           boolean importWrittenStory) {
        ProjectWrittenContext written = buildProjectWrittenContext(novel.getId(), importWrittenStory);
        if (!properties.configured()) {
            NovelProjectMemory memory = demoProjectMemory(currentMemory, message, sourceMessageId);
            if ("DISCUSS".equals(target)) {
                String reply = importWrittenStory && written.analyzedThroughChapterNo() != null
                        ? "我已经读取到第" + written.analyzedThroughChapterNo() + "章。可以先围绕现有主角和冲突整理一个大致方向，具体事件留到大纲和章节阶段再补。当前是演示模式，配置 AI 密钥后可生成立项草案。"
                        : "一句灵感就够开始。先确定主角的特别之处和故事的大致方向，能力机制与开篇细节可以边写边补。当前是演示模式，配置 AI 密钥后可继续讨论并生成草案。";
                return new NovelProjectChatTurnOutput(reply, null, memory, written.analyzedThroughChapterNo(), written.sourceContentVersionSum());
            }
            NovelProjectGenerateResponse generated = generateNovelProject(novel.getId(),
                    new NovelProjectGenerateRequest(target, message, current));
            return new NovelProjectChatTurnOutput("我已根据目前的讨论整理出一份立项修改方案。当前是演示模式，请检查方案卡后再应用。",
                    generated.content(), memory, written.analyzedThroughChapterNo(), written.sourceContentVersionSum());
        }

        String system = """
                你是网络小说“立项顾问”，帮助作者用轻量讨论找到一个能开始写作的方向。先给可用建议，细节可以边写边补。
                规则：
                1. 普通 DISCUSS 默认不提问：先用简短文字接住作者的想法，给出一个可写的主线方向或推进建议。回复通常100至200字，不重复大段复述材料。
                   只有缺失信息会使题材、基调或主线方向产生根本分歧且无法合理暂定时，才可问一个简短问题；每轮最多一个问题，不能在同一句或编号下夹带多个子问题。
                   作者说“没想好”“先这样”“不要问这么细”时，停止追问并给一个可继续写的暂定方向；作者明确要求深入分析时才展开解释。
                2. 当作者明确要求新增、生成、修改、调整或定稿立项内容时，可以输出 suggestion；否则 suggestion 必须为 null。
                3. suggestion 是完整立项方案。没有讨论到的字段必须照抄当前草案，禁止因信息不足而清空。
                4. FOUNDATION 只修改定位、卖点、金手指、升级、期待、规模和开篇字段；DESCRIPTION 只修改简介；OUTLINE 只修改总纲；ALL 可修改全部字段。
                5. 已写剧情材料是不可推翻的事实，只能据此反推作品定位和规划后续。要区分“正文已经成立的事实”“AI反推结论”“后续建议”。
                6. 立项只需题材与基调、主角的独特处境、核心卖点和主线方向。简介突出主角处境、核心冲突和阅读钩子；总纲交代起点、主线目标和大致推进即可。
                7. 不主动追问能力触发条件、必然性、限制代价、详细升级规则、人物相遇方式或开篇三章；这些细节留到大纲和章节阶段，只有作者主动讨论或明确要求细化时才展开。
                   信息不全时先提出标明“可暂定”的建议，未定字段可保持原值或留空；禁止编造已确认事实，也不能把回答问题作为生成方案的前置条件。
                8. 长期记忆账本是已有讨论结论。回答和提问前必须逐条检查；禁止重新询问 answeredQuestions 或已能从 confirmedFacts/characterPremises 得出答案的问题。
                9. 必须把本轮新增结论合并后输出完整 memory。不得遗失旧事实；已回答的问题从 openQuestions 移入 answeredQuestions；每条保留简洁且自包含的主语。只有作者明确推翻时才能替换旧结论。
                   AI 的暂定建议在作者确认前不能写入 confirmedFacts、characterPremises 或 lockedConstraints。openQuestions 只保留真正影响核心方向的未定问题，清理其中的细节追问；记忆里的旧问题不是必须逐个作答的清单。
                10. sourceMessageIds 保留旧编号并加入本轮作者消息编号。只输出 JSON，不要 Markdown。格式：
                {"reply":"回复","suggestion":null,"memory":{"confirmedFacts":[],"characterPremises":[],"lockedConstraints":[],"rejectedIdeas":[],"answeredQuestions":[],"openQuestions":[],"authorPreferences":[],"sourceMessageIds":[]}}
                suggestion 非空时仍使用完整立项方案字段。
                """;
        String user = "【作品名称】\n" + novel.getTitle()
                + "\n\n【当前页面草案】\n" + writeJson(current)
                + "\n\n【本轮操作】\n" + target
                + "\n\n【最近立项对话】\n" + (history == null || history.isBlank() ? "尚无历史对话" : history)
                + "\n\n【持久长期记忆账本】\n" + writeJson(currentMemory)
                + (backfillHistory == null || backfillHistory.isBlank() ? "" : "\n\n【首次建立账本：需回溯的早期原始对话】\n" + backfillHistory)
                + (written.context().isBlank() ? "" : "\n\n【作者主动导入的已写剧情材料】\n" + written.context())
                + "\n\n【作者本轮消息 " + sourceMessageId + "】\n" + message;
        String raw = callProvider(null, "NOVEL_PROJECT_CHAT", system, user, 0.65, 10000,
                new ProviderOptions(true, false, 10000));
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            String reply = root.path("reply").asText("").trim();
            if (reply.isBlank()) throw new AiProviderException("AI 没有返回立项讨论内容，请重试");
            JsonNode suggestionNode = root.path("suggestion");
            NovelProjectContent suggestion = suggestionNode.isObject()
                    ? normalizeProjectSuggestion(suggestionNode, current, target) : null;
            JsonNode memoryNode = root.path("memory");
            NovelProjectMemory memory = memoryNode.isObject()
                    ? objectMapper.treeToValue(memoryNode, NovelProjectMemory.class) : currentMemory;
            memory = normalizeProjectMemory(memory, currentMemory, sourceMessageId);
            return new NovelProjectChatTurnOutput(reply, suggestion, memory,
                    written.analyzedThroughChapterNo(), written.sourceContentVersionSum());
        } catch (AiProviderException error) {
            throw error;
        } catch (Exception error) {
            throw new AiProviderException("AI 返回的立项讨论格式不正确，请重试", error);
        }
    }

    private NovelProjectContent normalizeProjectSuggestion(JsonNode node, NovelProjectContent current, String target) {
        boolean foundation = "FOUNDATION".equals(target) || "ALL".equals(target) || "DISCUSS".equals(target);
        boolean description = "DESCRIPTION".equals(target) || "ALL".equals(target) || "DISCUSS".equals(target);
        boolean outline = "OUTLINE".equals(target) || "ALL".equals(target) || "DISCUSS".equals(target);
        return new NovelProjectContent(
                projectText(node, "inspiration", current.inspiration(), 5_000, foundation),
                projectText(node, "genre", current.genre(), 100, foundation),
                projectText(node, "channel", current.channel(), 100, foundation),
                projectText(node, "targetAudience", current.targetAudience(), 500, foundation),
                projectText(node, "platform", current.platform(), 100, foundation),
                projectText(node, "coreSellingPoint", current.coreSellingPoint(), 5_000, foundation),
                projectText(node, "protagonistHook", current.protagonistHook(), 5_000, foundation),
                projectText(node, "growthRoute", current.growthRoute(), 10_000, foundation),
                projectText(node, "readerExpectations", current.readerExpectations(), 5_000, foundation),
                projectText(node, "openingThreeChapters", current.openingThreeChapters(), 10_000, foundation),
                projectInteger(node, "expectedWords", current.expectedWords(), foundation, 10_000, 10_000_000),
                projectInteger(node, "expectedVolumes", current.expectedVolumes(), foundation, 1, 100),
                projectInteger(node, "chapterWordTarget", current.chapterWordTarget(), foundation, 300, 10_000),
                projectText(node, "description", current.description(), 1_000, description),
                projectText(node, "outline", current.outline(), 100_000, outline));
    }

    private NovelProjectMemory normalizeProjectMemory(NovelProjectMemory proposed, NovelProjectMemory current,
                                                       Long sourceMessageId) {
        NovelProjectMemory base = current == null ? emptyProjectMemory() : current;
        NovelProjectMemory next = proposed == null ? base : proposed;
        LinkedHashSet<Long> sources = new LinkedHashSet<>(safeLongList(base.sourceMessageIds()));
        sources.addAll(safeLongList(next.sourceMessageIds()));
        if (sourceMessageId != null) sources.add(sourceMessageId);
        return new NovelProjectMemory(
                mergeMemoryList(base.confirmedFacts(), next.confirmedFacts()),
                mergeMemoryList(base.characterPremises(), next.characterPremises()),
                mergeMemoryList(base.lockedConstraints(), next.lockedConstraints()),
                mergeMemoryList(base.rejectedIdeas(), next.rejectedIdeas()),
                mergeMemoryList(base.answeredQuestions(), next.answeredQuestions()),
                next.openQuestions() == null ? safeStringList(base.openQuestions()) : safeStringList(next.openQuestions()),
                mergeMemoryList(base.authorPreferences(), next.authorPreferences()),
                sources.stream().limit(500).toList());
    }

    private NovelProjectMemory demoProjectMemory(NovelProjectMemory current, String message, Long sourceMessageId) {
        NovelProjectMemory base = current == null ? emptyProjectMemory() : current;
        List<String> facts = new ArrayList<>(safeStringList(base.confirmedFacts()));
        if (hasText(message)) facts.add("作者原话：" + message.trim());
        return normalizeProjectMemory(new NovelProjectMemory(facts, base.characterPremises(),
                base.lockedConstraints(), base.rejectedIdeas(), base.answeredQuestions(), base.openQuestions(),
                base.authorPreferences(), base.sourceMessageIds()), base, sourceMessageId);
    }

    private NovelProjectMemory emptyProjectMemory() {
        return new NovelProjectMemory(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private List<String> mergeMemoryList(List<String> left, List<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(safeStringList(left));
        merged.addAll(safeStringList(right));
        return merged.stream().limit(120).toList();
    }

    private List<String> safeStringList(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(this::hasText).map(String::trim).distinct().limit(120).toList();
    }

    private List<Long> safeLongList(List<Long> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).distinct().toList();
    }

    private ProjectWrittenContext buildProjectWrittenContext(long novelId, boolean enabled) {
        if (!enabled) return ProjectWrittenContext.empty();
        List<ChapterEntity> chapters = chapterRepository.findByNovelIdOrderByChapterNoAsc(novelId).stream()
                .filter(chapter -> chapter.getContent() != null && !chapter.getContent().isBlank()).toList();
        if (chapters.isEmpty()) return ProjectWrittenContext.empty();
        int totalChars = chapters.stream().mapToInt(chapter -> chapter.getContent().length()).sum();
        long versionSum = chapters.stream().map(ChapterEntity::getContentVersion).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
        StringBuilder text = new StringBuilder();
        if (totalChars <= 30_000) {
            for (ChapterEntity chapter : chapters) {
                text.append("第").append(chapter.getChapterNo()).append("章《").append(chapter.getTitle()).append("》\n")
                        .append(chapter.getContent()).append("\n\n");
            }
        } else {
            Set<Long> rawIds = new LinkedHashSet<>();
            chapters.stream().limit(3).map(ChapterEntity::getId).forEach(rawIds::add);
            chapters.stream().skip(Math.max(0, chapters.size() - 2L)).map(ChapterEntity::getId).forEach(rawIds::add);
            Map<Integer, ChapterMemoryEntity> memories = chapterMemoryRepository
                    .findByNovelIdAndStatusOrderByChapterNoAsc(novelId, "CONFIRMED").stream()
                    .collect(Collectors.toMap(ChapterMemoryEntity::getChapterNo, item -> item, (left, right) -> right));
            for (ChapterEntity chapter : chapters) {
                text.append("第").append(chapter.getChapterNo()).append("章《").append(chapter.getTitle()).append("》：\n");
                if (rawIds.contains(chapter.getId())) text.append(chapterExcerpt(chapter.getContent(), 5_000));
                else {
                    ChapterMemoryEntity memory = memories.get(chapter.getChapterNo());
                    if (memory != null && Objects.equals(memory.getSourceChapterVersion(), chapter.getContentVersion())) {
                        ChapterMemoryContent content = readMemoryContent(memory);
                        text.append(content.summary());
                        if (hasText(content.plotProgress())) text.append("\n主线推进：").append(content.plotProgress());
                    } else text.append("（本章没有版本一致的已确认章节压缩，未导入正文）");
                }
                text.append("\n\n");
                if (text.length() >= 45_000) break;
            }
        }
        String context = text.length() <= 45_000 ? text.toString() : text.substring(0, 45_000);
        return new ProjectWrittenContext(context, chapters.get(chapters.size() - 1).getChapterNo(), versionSum);
    }

    private record ProjectWrittenContext(String context, Integer analyzedThroughChapterNo, Long sourceContentVersionSum) {
        private static ProjectWrittenContext empty() { return new ProjectWrittenContext("", null, null); }
    }

    private NovelProjectContent copyProject(NovelProjectContent current, String description, String outline) {
        return new NovelProjectContent(current.inspiration(), current.genre(), current.channel(), current.targetAudience(),
                current.platform(), current.coreSellingPoint(), current.protagonistHook(), current.growthRoute(),
                current.readerExpectations(), current.openingThreeChapters(), current.expectedWords(),
                current.expectedVolumes(), current.chapterWordTarget(), description, outline);
    }

    private String projectText(JsonNode node, String field, String current, int limit, boolean allowChange) {
        if (!allowChange) return current;
        String generated = node.path(field).asText("").trim();
        String value = generated.isBlank() ? current : generated;
        if (value == null) return null;
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private Integer projectInteger(JsonNode node, String field, Integer current, boolean allowChange, int min, int max) {
        if (!allowChange || !node.hasNonNull(field) || !node.path(field).canConvertToInt()) return current;
        int value = node.path(field).asInt();
        return Math.max(min, Math.min(max, value));
    }

    @Transactional(readOnly = true)
    public StoryVolumeGenerateResponse generateStoryVolume(long novelId, StoryVolumeGenerateRequest request) {
        NovelEntity novel = requireNovel(novelId);
        if (request.chapterEnd() != null && request.chapterEnd() < request.chapterStart())
            throw new WritingConflictException("卷末章节不能早于卷首章节");
        List<ChapterEntity> writtenChapters = chapterRepository.findByNovelIdOrderByChapterNoAsc(novelId).stream()
                .filter(chapter -> chapter.getChapterNo() >= request.chapterStart())
                .filter(chapter -> request.chapterEnd() == null || chapter.getChapterNo() <= request.chapterEnd())
                .filter(chapter -> chapter.getContent() != null && !chapter.getContent().isBlank())
                .toList();
        Integer analyzedThrough = writtenChapters.isEmpty() ? null
                : writtenChapters.get(writtenChapters.size() - 1).getChapterNo();
        String writtenFacts = buildVolumeWrittenFacts(novelId, writtenChapters);
        String currentJson = request.currentContent() == null ? "未提供" : writeJson(request.currentContent());

        if (!properties.configured()) {
            StoryVolumeContent current = request.currentContent();
            String title = hasText(request.title()) ? request.title().trim()
                    : current != null && hasText(current.title()) ? current.title() : "第" + request.volumeNo() + "卷";
            String retrospective = writtenChapters.isEmpty() ? "当前分卷尚无已写正文。"
                    : "已读取第" + request.chapterStart() + "章至第" + analyzedThrough + "章；配置 API Key 后将提取不可改写的剧情事实。";
            StoryVolumeContent demo = new StoryVolumeContent(title,
                    current != null && hasText(current.objective()) ? current.objective() : "请填写本卷需要完成的核心目标",
                    current != null && hasText(current.retrospective()) ? current.retrospective() : retrospective,
                    current != null && hasText(current.futurePlan()) ? current.futurePlan() : "请规划本卷剩余剧情阶段与章节安排",
                    current == null ? "" : nullToEmpty(current.keyTurningPoints()),
                    current == null ? "" : nullToEmpty(current.climax()),
                    current == null ? "" : nullToEmpty(current.endingHook()),
                    current == null ? "" : nullToEmpty(current.foreshadows()),
                    current == null ? "" : nullToEmpty(current.lockedBeats()));
            return new StoryVolumeGenerateResponse(demo, analyzedThrough,
                    List.of("当前为演示模式，已识别本卷已写章节；配置 API Key 后会分析事实并规划剩余剧情"), "demo");
        }

        String signal = request.normalizedInstruction() + "\n" + currentJson + "\n" + writtenFacts;
        CharacterContextService.Selection characters = characterContextService.select(novelId, signal);
        OrganizationContextService.Selection organizations = organizationContextService.select(novelId, signal);
        StringBuilder settings = new StringBuilder();
        for (WorldSettingEntity setting : worldSettingRepository.findByNovelIdOrderByIdAsc(novelId)) {
            settings.append("- [").append(setting.getCategory()).append("] ").append(setting.getTitle())
                    .append("：").append(setting.getContent()).append('\n');
            if (settings.length() >= 8_000) break;
        }
        String system = """
                你是长篇网络小说的分卷策划编辑。你要根据作品资料和本卷已经写出的章节，整理本卷主线并规划尚未写出的部分。
                必须遵守：
                1. 已写章节是不可推翻的事实。retrospective 只能准确总结已发生内容，不得把建议或未来安排写成既成事实。
                2. futurePlan 只规划尚未写出的剧情；不得要求倒改已写正文来迁就规划。
                3. 若当前表单已有内容，应尽量保留作者明确设定；lockedBeats 中的节点绝对不能删除或改变结果。
                4. 安排要服务于全书大纲，明确阶段推进、关键转折、高潮、卷末结果和下一卷钩子，避免空泛套话。
                5. 若卷内已经写了很多章节，应顺着当前走势规划剩余篇幅，不要从卷首重新编排。
                6. 只输出一个 JSON 对象，不要 Markdown、解释或额外文本。
                JSON 格式：
                {"content":{"title":"卷标题","objective":"本卷核心目标","retrospective":"已写剧情事实总结","futurePlan":"剩余剧情安排","keyTurningPoints":"每行一个关键转折","climax":"本卷高潮","endingHook":"卷末结果与下一卷钩子","foreshadows":"每行一个需要埋设或回收的伏笔","lockedBeats":"作者锁定节点，原样保留"},"changeSummary":["本次规划摘要"]}
                所有 content 字段必须是字符串。没有内容时输出空字符串。
                """;
        String user = "【作品】\n" + novel.getTitle()
                + "\n\n【简介】\n" + safeText(novel.getDescription(), 1500)
                + "\n\n【全书大纲】\n" + safeText(novel.getOutline(), 6000)
                + promptSection("已确认立项约束", projectContext(novelId))
                + "\n\n【分卷范围】\n第" + request.volumeNo() + "卷，第" + request.chapterStart() + "章至"
                + (request.chapterEnd() == null ? "未定卷末" : "第" + request.chapterEnd() + "章")
                + "；已写到" + (analyzedThrough == null ? "尚无正文" : "第" + analyzedThrough + "章")
                + "\n\n【当前分卷表单】\n" + currentJson
                + "\n\n【已写章节事实材料】\n" + (writtenFacts.isBlank() ? "本卷尚无已写正文" : writtenFacts)
                + promptSection("相关人物档案", characters.promptContext())
                + promptSection("相关组织档案", organizations.promptContext())
                + promptSection("世界观设定", settings.toString())
                + "\n\n【作者要求】\n" + (request.normalizedInstruction().isBlank() ? "请基于现状生成合理的本卷主线" : request.normalizedInstruction());
        String raw = callProvider(null, "GENERATE_STORY_VOLUME", system, user, 0.45, 8192,
                new ProviderOptions(true, false, 8192));
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            JsonNode node = root.path("content");
            StoryVolumeContent content = new StoryVolumeContent(
                    requiredVolumeText(node, "title", request.title(), "第" + request.volumeNo() + "卷"),
                    requiredVolumeText(node, "objective", null, "明确本卷核心目标"),
                    volumeText(node, "retrospective"), volumeText(node, "futurePlan"),
                    volumeText(node, "keyTurningPoints"), volumeText(node, "climax"),
                    volumeText(node, "endingHook"), volumeText(node, "foreshadows"),
                    request.currentContent() != null && hasText(request.currentContent().lockedBeats())
                            ? request.currentContent().lockedBeats().trim() : volumeText(node, "lockedBeats"));
            List<String> summary = firstStringList(root, "changeSummary", "changes", "修改摘要");
            if (summary.isEmpty()) summary = List.of("已根据现有章节整理事实并规划本卷剩余剧情");
            return new StoryVolumeGenerateResponse(content, analyzedThrough, summary, "ai");
        } catch (AiProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiProviderException("AI 返回的分卷主线格式不正确，请重试", exception);
        }
    }

    private String buildVolumeWrittenFacts(long novelId, List<ChapterEntity> chapters) {
        Map<Integer, ChapterMemoryEntity> memories = chapterMemoryRepository
                .findByNovelIdAndStatusOrderByChapterNoAsc(novelId, "CONFIRMED").stream()
                .collect(Collectors.toMap(ChapterMemoryEntity::getChapterNo, item -> item, (left, right) -> right));
        StringBuilder text = new StringBuilder();
        int perChapterLimit = chapters.isEmpty() ? 1800 : Math.max(120, Math.min(1800, 26_000 / chapters.size()));
        for (ChapterEntity chapter : chapters) {
            ChapterMemoryEntity memory = memories.get(chapter.getChapterNo());
            text.append("第").append(chapter.getChapterNo()).append("章《").append(chapter.getTitle()).append("》：\n");
            if (memory != null && Objects.equals(memory.getSourceChapterVersion(), chapter.getContentVersion())) {
                ChapterMemoryContent content = readMemoryContent(memory);
                text.append(content.summary());
                if (hasText(content.plotProgress())) text.append("\n主线推进：").append(content.plotProgress());
                if (!content.unresolvedQuestions().isEmpty())
                    text.append("\n未解决：").append(String.join("；", content.unresolvedQuestions()));
            } else {
                text.append(chapterExcerpt(chapter.getContent(), perChapterLimit));
            }
            text.append("\n\n");
        }
        return text.length() <= 30_000 ? text.toString() : text.substring(0, 30_000);
    }

    private String chapterExcerpt(String content, int limit) {
        if (content == null || content.isBlank()) return "（本章暂无正文）";
        String normalized = content.trim();
        if (normalized.length() <= limit) return normalized;
        int head = Math.max(1, limit * 3 / 5);
        int tail = Math.max(1, limit - head - 1);
        return normalized.substring(0, head) + "…" + normalized.substring(normalized.length() - tail);
    }

    private String volumeText(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        int limit = switch (field) {
            case "title" -> 200;
            case "objective", "climax", "endingHook" -> 5_000;
            case "keyTurningPoints", "foreshadows", "lockedBeats" -> 10_000;
            default -> 30_000;
        };
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private String requiredVolumeText(JsonNode node, String field, String fallback, String defaultValue) {
        String value = volumeText(node, field);
        if (value.isBlank() && hasText(fallback)) value = fallback.trim();
        return value.isBlank() ? defaultValue : value;
    }

    private String nullToEmpty(String value) { return value == null ? "" : value; }

    private List<String> storyBibleFieldNames(String type) {
        return switch (type) {
            case "CHARACTER" -> List.of("name", "aliases", "role", "affiliations", "description", "personality", "goal", "currentState", "relationships");
            case "ORGANIZATION" -> List.of("name", "aliases", "type", "description", "goal", "structure", "relationships");
            case "WORLD_SETTING" -> List.of("category", "title", "content");
            default -> throw new AiProviderException("不支持的故事资料类型");
        };
    }

    private List<String> detectStoryBibleTargets(String type, String instruction) {
        if (instruction == null || instruction.isBlank()) return List.of();
        Map<String, List<String>> keywords = switch (type) {
            case "CHARACTER" -> Map.of(
                    "name", List.of("人物姓名", "角色姓名", "名字"),
                    "aliases", List.of("别名", "简称", "称呼"),
                    "role", List.of("角色定位", "人物定位"),
                    "affiliations", List.of("所属组织", "所属势力", "所属阵营", "加入的组织"),
                    "description", List.of("身份与经历", "人物经历", "人物背景", "身世"),
                    "personality", List.of("性格特征", "性格"),
                    "goal", List.of("目标与动机", "人物目标", "动机"),
                    "currentState", List.of("当前状态", "身体状态", "当前位置", "掌握的信息"),
                    "relationships", List.of("当前关系", "人物关系", "关系变化"));
            case "ORGANIZATION" -> Map.of(
                    "name", List.of("组织名称"),
                    "aliases", List.of("别名"),
                    "type", List.of("组织类型"),
                    "description", List.of("组织概况", "组织来历", "概况"),
                    "goal", List.of("目标与立场", "组织目标", "立场"),
                    "structure", List.of("结构与重要成员", "组织结构", "重要成员", "成员", "部门", "等级"),
                    "relationships", List.of("与其他势力的关系", "势力关系", "盟友", "敌对关系"));
            case "WORLD_SETTING" -> Map.of(
                    "category", List.of("分类"),
                    "title", List.of("设定标题", "标题"),
                    "content", List.of("详细内容", "设定内容", "世界规则", "规则"));
            default -> Map.of();
        };
        List<String> matched = new ArrayList<>();
        for (String fieldName : storyBibleFieldNames(type)) {
            if (keywords.getOrDefault(fieldName, List.of()).stream().anyMatch(instruction::contains)) matched.add(fieldName);
        }
        return matched;
    }

    private String limitStoryBibleField(String type, String fieldName, String value) {
        int limit = switch (fieldName) {
            case "name", "role", "type" -> 100;
            case "category" -> 50;
            case "title" -> 200;
            case "aliases", "affiliations" -> 500;
            case "content" -> 10_000;
            default -> 5_000;
        };
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit);
    }

    private String safeText(String value, int maxLength) {
        if (value == null || value.isBlank()) return "未提供";
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String promptSection(String title, String content) {
        if (content == null || content.isBlank()) return "";
        return "\n\n【" + title + "】\n" + content;
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new AiProviderException("故事资料序列化失败", exception); }
    }

    private StoryBibleAssistResponse demoStoryBibleAssist(StoryBibleAssistRequest request,
                                                           Map<String, String> currentFields,
                                                           List<String> targetFields) {
        Map<String, String> fields = new LinkedHashMap<>(currentFields);
        String instruction = request.normalizedInstruction();
        String target = targetFields.get(0);
        if (!instruction.isBlank()) {
            String existing = fields.getOrDefault(target, "");
            fields.put(target, existing.isBlank() ? instruction : existing + "\n" + instruction);
        }
        return new StoryBibleAssistResponse(request.type(), request.mode(), fields,
                List.of("当前为演示模式，已将关键信息整理到表单；配置 API Key 后会由 AI 结构化扩写"), "demo");
    }

    @Transactional(noRollbackFor = AiProviderException.class)
    public AgentRunResponse createPlan(long novelId, long chapterId, PlanRequest request) {
        NovelEntity novel = requireNovel(novelId);
        ChapterEntity chapter = requireChapter(chapterId);
        if (!chapter.getNovelId().equals(novelId)) throw new WritingNotFoundException("章节不属于当前小说");

        AgentRunEntity run = new AgentRunEntity();
        run.setNovelId(novelId);
        run.setChapterId(chapterId);
        run.setStatus("RUNNING");
        run.setCurrentStep("LOAD_CONTEXT");
        run.setGuidance(request.normalizedGuidance());
        run.setProviderMode(properties.configured() ? "ai" : "demo");
        run.setOperation("CHAPTER_WRITING");
        run.setModel(activeModel());
        run.setTotalDurationMs(0L);
        runRepository.saveAndFlush(run);

        long started = System.nanoTime();
        String context = buildContext(novel, chapter, request.normalizedGuidance());
        addStep(run.getId(), 1, "LOAD_CONTEXT", "COMPLETED",
                "已读取大纲、人物、世界观和最近章节（上下文 " + context.length() + " 字）", elapsed(started));

        run.setCurrentStep("PLAN_CHAPTER");
        started = System.nanoTime();
        ChapterPlan plan = properties.configured()
                ? requestPlan(run.getId(), context, request.normalizedGuidance())
                : demoCall(run.getId(), "PLAN_CHAPTER", () -> demoPlan(chapter, request.normalizedGuidance()));
        try {
            run.setPlanJson(objectMapper.writeValueAsString(plan));
        } catch (Exception e) {
            throw new AiProviderException("章节计划保存失败", e);
        }
        run.setStatus("WAITING_APPROVAL");
        run.setCurrentStep("WAITING_PLAN_APPROVAL");
        run.setTotalDurationMs(sumStepDuration(run.getId()) + elapsed(started));
        runRepository.saveAndFlush(run);
        addStep(run.getId(), 2, "PLAN_CHAPTER", "COMPLETED", "章节计划已生成，等待作者确认", elapsed(started));
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public DirectorConversationResponse getDirectorConversation(long chapterId) {
        requireChapter(chapterId);
        return directorSessionRepository.findByChapterId(chapterId)
                .map(this::toDirectorConversation)
                .orElseGet(() -> new DirectorConversationResponse(chapterId, List.of(), null, null));
    }

    @Transactional
    public ChapterPlan saveDirectorPlan(long chapterId, SaveDirectorPlanRequest request) {
        ChapterEntity chapter = requireChapter(chapterId);
        persistDirectorPlan(chapter, request.plan());
        log.info("章节导演主线手工保存完成 novelId={} chapterId={} developments={} characters={} continuityNotes={}",
                chapter.getNovelId(), chapterId, request.plan().developments().size(),
                request.plan().characters().size(), request.plan().continuityNotes().size());
        return request.plan();
    }

    @Transactional(noRollbackFor = AiProviderException.class)
    public DirectorConversationResponse chatWithDirector(long novelId, long chapterId,
                                                         DirectorChatRequest request) {
        return chatWithDirectorInternal(novelId, chapterId, request, ignored -> {});
    }

    @Transactional(noRollbackFor = AiProviderException.class)
    public DirectorConversationResponse chatWithDirectorStreamed(long novelId, long chapterId,
                                                                 DirectorChatRequest request,
                                                                 Consumer<String> onTextDelta) {
        return chatWithDirectorInternal(novelId, chapterId, request,
                onTextDelta == null ? ignored -> {} : onTextDelta);
    }

    private DirectorConversationResponse chatWithDirectorInternal(long novelId, long chapterId,
                                                                  DirectorChatRequest request,
                                                                  Consumer<String> onTextDelta) {
        NovelEntity novel = requireNovel(novelId);
        ChapterEntity chapter = requireChapter(chapterId);
        if (!chapter.getNovelId().equals(novelId)) throw new WritingNotFoundException("章节不属于当前小说");

        ChapterDirectorSessionEntity session = directorSessionRepository.findByChapterId(chapterId).orElseGet(() -> {
            ChapterDirectorSessionEntity created = new ChapterDirectorSessionEntity();
            created.setNovelId(novelId);
            created.setChapterId(chapterId);
            return directorSessionRepository.saveAndFlush(created);
        });
        List<ChapterDirectorMessageEntity> previousMessages =
                directorMessageRepository.findBySessionIdOrderByIdAsc(session.getId());
        ChapterPlan currentPlan = request.currentPlan() != null
                ? request.currentPlan() : readDirectorPlan(session.getCurrentPlanJson());

        AgentRunEntity run = new AgentRunEntity();
        run.setNovelId(novelId); run.setChapterId(chapterId); run.setStatus("RUNNING");
        run.setCurrentStep("LOAD_CONTEXT"); run.setGuidance(request.normalizedMessage());
        run.setProviderMode(properties.configured() ? "ai" : "demo");
        run.setOperation("CHAPTER_DIRECTOR_CHAT"); run.setModel(activeModel()); run.setTotalDurationMs(0L);
        runRepository.saveAndFlush(run);

        ChapterDirectorMessageEntity userMessage = new ChapterDirectorMessageEntity();
        userMessage.setSessionId(session.getId()); userMessage.setRunId(run.getId());
        userMessage.setRole("USER"); userMessage.setContent(request.normalizedMessage());
        directorMessageRepository.saveAndFlush(userMessage);

        long started = System.nanoTime();
        String context = buildDirectorContext(novel, chapter, request.normalizedMessage(), planCharacterSignal(currentPlan));
        RagContextService.RagContext rag = retrieveDirectorRag(
                novel, chapter, previousMessages, request.normalizedMessage());
        if (!rag.promptContext().isBlank()) {
            context += "\n【按需召回的远期剧情】\n" + rag.promptContext() + '\n';
        }
        addStep(run.getId(), 1, "LOAD_CONTEXT", "COMPLETED",
                "已读取作品资料、当前章节主线、最近导演对话，并采用 "
                        + rag.debugInfo().usedCount() + " 条远期剧情记忆", elapsed(started));
        String directorStep = request.shouldGeneratePlan() ? "GENERATE_CHAPTER_PLAN" : "DISCUSS_CHAPTER";
        run.setCurrentStep(directorStep); runRepository.saveAndFlush(run);

        started = System.nanoTime();
        DirectorTurnOutput turn;
        try {
            turn = properties.configured()
                    ? requestDirectorTurn(run.getId(), context, previousMessages, currentPlan,
                        request.normalizedMessage(), request.useThinking(), request.shouldGeneratePlan(), onTextDelta)
                    : demoCall(run.getId(), directorStep, () -> request.shouldGeneratePlan()
                        ? demoDirectorTurn(chapter, currentPlan, request.normalizedMessage())
                        : new DirectorTurnOutput("我已经记下这个想法。你可以继续补充人物动机、冲突或结尾方向，准备好后再生成本章主线。", null, List.of()));
            String planJson = turn.plan() == null ? null : objectMapper.writeValueAsString(turn.plan());
            run.setPlanJson(planJson);
            run.setStatus(turn.plan() == null ? "COMPLETED" : "WAITING_APPROVAL");
            run.setCurrentStep(turn.plan() == null ? "DIRECTOR_REPLIED" : "WAITING_PLAN_APPROVAL");
            run.setTotalDurationMs(sumStepDuration(run.getId()) + elapsed(started));
            runRepository.saveAndFlush(run);
            addStep(run.getId(), 2, directorStep, "COMPLETED",
                    turn.plan() == null ? "已回应作者，本轮未生成章节主线"
                            : "已根据对话生成章节主线，等待作者继续沟通或确认", elapsed(started));

            ChapterDirectorMessageEntity assistant = new ChapterDirectorMessageEntity();
            assistant.setSessionId(session.getId()); assistant.setRunId(run.getId());
            assistant.setRole("ASSISTANT"); assistant.setContent(turn.reply().strip());
            assistant.setPlanJson(planJson);
            assistant.setChangeSummaryJson(objectMapper.writeValueAsString(turn.changeSummary()));
            directorMessageRepository.saveAndFlush(assistant);

            if (turn.plan() != null) {
                session.setCurrentPlanJson(planJson);
                session.setCurrentRunId(run.getId());
            }
            directorSessionRepository.saveAndFlush(session);
        } catch (AiProviderException error) {
            markDirectorRunFailed(run, error.getMessage());
            throw error;
        } catch (Exception error) {
            String message = "章节导演对话保存失败";
            markDirectorRunFailed(run, message);
            throw new AiProviderException(message, error);
        }
        log.info("章节导演多轮对话完成 novelId={} chapterId={} sessionId={} runId={} generatePlan={} previousMessages={} changes={}",
                novelId, chapterId, session.getId(), run.getId(), request.shouldGeneratePlan(),
                previousMessages.size(), turn.changeSummary().size());
        return toDirectorConversation(session);
    }

    @Transactional(readOnly = true)
    public DirectorRagPreviewResponse previewDirectorRag(long novelId, long chapterId,
                                                         DirectorRagPreviewRequest request) {
        NovelEntity novel = requireNovel(novelId);
        ChapterEntity chapter = requireChapter(chapterId);
        if (!chapter.getNovelId().equals(novelId)) throw new WritingNotFoundException("章节不属于当前小说");
        List<ChapterDirectorMessageEntity> messages = directorSessionRepository.findByChapterId(chapterId)
                .map(session -> directorMessageRepository.findBySessionIdOrderByIdAsc(session.getId()))
                .orElse(List.of());
        RagContextService.RagContext rag = retrieveDirectorRag(
                novel, chapter, messages, request.normalizedMessage());
        List<DirectorRagMemory> memories = rag.debugInfo().memories().stream()
                .map(item -> new DirectorRagMemory(item.pointId(), item.score(), item.chapterId(),
                        item.chapterOrder(), item.memoryType(), item.text(), item.used()))
                .toList();
        return new DirectorRagPreviewResponse(rag.debugInfo().enabled(), rag.debugInfo().status(),
                rag.debugInfo().retrievedCount(), rag.debugInfo().usedCount(),
                rag.debugInfo().durationMs(), memories);
    }

    @Transactional
    public DirectorConversationResponse deleteDirectorTurn(long messageId) {
        ChapterDirectorMessageEntity selectedMessage = directorMessageRepository.findById(messageId)
                .orElseThrow(() -> new WritingNotFoundException("章节导演消息不存在：" + messageId));
        ChapterDirectorSessionEntity session = directorSessionRepository.findById(selectedMessage.getSessionId())
                .orElseThrow(() -> new WritingNotFoundException("章节导演会话不存在"));
        List<ChapterDirectorMessageEntity> messages =
                directorMessageRepository.findBySessionIdOrderByIdAsc(session.getId());
        Long deletedRunId = selectedMessage.getRunId();
        List<ChapterDirectorMessageEntity> deletedMessages = messages.stream()
                .filter(message -> deletedRunId == null
                        ? message.getId().equals(selectedMessage.getId())
                        : deletedRunId.equals(message.getRunId()))
                .toList();
        directorMessageRepository.deleteAll(deletedMessages);

        if (deletedRunId != null && deletedRunId.equals(session.getCurrentRunId())) {
            ChapterDirectorMessageEntity previousPlanMessage = messages.stream()
                    .filter(message -> !deletedMessages.contains(message))
                    .filter(message -> hasText(message.getPlanJson()))
                    .reduce((first, second) -> second)
                    .orElse(null);
            session.setCurrentPlanJson(previousPlanMessage == null ? null : previousPlanMessage.getPlanJson());
            session.setCurrentRunId(previousPlanMessage == null ? null : previousPlanMessage.getRunId());
            directorSessionRepository.saveAndFlush(session);
        }
        directorMessageRepository.flush();
        log.info("章节导演对话轮次删除完成 sessionId={} requestedMessageId={} runId={} deletedMessages={} currentRunId={}",
                session.getId(), messageId, deletedRunId, deletedMessages.size(), session.getCurrentRunId());
        return toDirectorConversation(session);
    }

    @Transactional(noRollbackFor = AiProviderException.class)
    public AgentRunResponse createDraft(long runId, DraftRequest request) {
        AgentRunEntity run = requireRun(runId);
        if (!"WAITING_APPROVAL".equals(run.getStatus()) && !"COMPLETED".equals(run.getStatus()))
            throw new WritingConflictException("当前 Agent 任务尚未进入计划确认阶段");
        NovelEntity novel = requireNovel(run.getNovelId());
        ChapterEntity chapter = requireChapter(run.getChapterId());

        run.setStatus("RUNNING");
        run.setCurrentStep("WRITE_DRAFT");
        try { run.setPlanJson(objectMapper.writeValueAsString(request.plan())); }
        catch (Exception e) { throw new AiProviderException("章节计划格式不正确", e); }
        persistDirectorPlan(chapter, request.plan());
        runRepository.saveAndFlush(run);

        long started = System.nanoTime();
        String context = buildContext(novel, chapter, planCharacterSignal(request.plan()));
        String stylePrompt = writingStyleService.resolvePrompt(request.styleId(), novel.getId());
        String draft = properties.configured()
                ? requestDraft(run.getId(), context, request.plan(), request.normalizedTargetLength(), stylePrompt)
                : demoCall(run.getId(), "WRITE_DRAFT", () -> demoDraft(request.plan()));
        run.setDraft(draft);
        run.setStatus("COMPLETED");
        run.setCurrentStep("DRAFT_READY");
        run.setTotalDurationMs(sumStepDuration(run.getId()) + elapsed(started));
        runRepository.saveAndFlush(run);
        addStep(run.getId(), nextStepNo(run.getId()), "WRITE_DRAFT", "COMPLETED",
                "已按确认后的计划生成 " + draft.replaceAll("\\s", "").length() + " 字草稿", elapsed(started));
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public AgentRunResponse getRun(long runId) { return toResponse(requireRun(runId)); }

    @Transactional(readOnly = true)
    public List<AgentRunSummaryResponse> listRuns(long novelId, int limit) {
        requireNovel(novelId);
        int safeLimit = Math.max(1, Math.min(100, limit));
        return runRepository.findByNovelIdOrderByCreatedAtDesc(novelId, PageRequest.of(0, safeLimit))
                .stream().map(this::toSummaryResponse).toList();
    }

    @Transactional(noRollbackFor = AiProviderException.class)
    public ChapterRevisionResponse rewrite(long novelId, long chapterId, RewriteRequest request) {
        NovelEntity novel = requireNovel(novelId);
        ChapterEntity chapter = requireChapter(chapterId);
        if (!chapter.getNovelId().equals(novelId)) throw new WritingNotFoundException("章节不属于当前小说");
        if (request.endOffset() < request.startOffset()) throw new WritingConflictException("正文选区范围不正确");

        AgentRunEntity run = new AgentRunEntity();
        run.setNovelId(novelId);
        run.setChapterId(chapterId);
        run.setStatus("RUNNING");
        run.setCurrentStep("LOAD_CONTEXT");
        run.setGuidance(request.normalizedInstruction());
        run.setProviderMode(properties.configured() ? "ai" : "demo");
        run.setOperation("PARAGRAPH_REWRITE"); run.setModel(activeModel()); run.setTotalDurationMs(0L);
        runRepository.saveAndFlush(run);

        long started = System.nanoTime();
        RewriteContext rewriteContext = buildRewriteContext(novel, request);
        String storyContext = rewriteContext.text();
        String stylePrompt = writingStyleService.resolvePrompt(request.styleId(), novelId);
        log.info("段落打磨按需装载上下文 novelId={} chapterId={} thinkingEnabled={} storyContextChars={} selectedCharacters={} selectedOrganizations={} beforeChars={} selectedChars={} afterChars={}",
                novelId, chapterId, request.useThinking(), storyContext.length(), rewriteContext.characterNames(),
                rewriteContext.organizationNames(),
                request.normalizedBeforeContext().length(), request.selectedText().length(), request.normalizedAfterContext().length());
        addStep(run.getId(), 1, "LOAD_CONTEXT", "COMPLETED",
                "已读取故事资料、章节上下文和正文选区", elapsed(started));

        run.setCurrentStep("REWRITE_SELECTION");
        runRepository.saveAndFlush(run);
        started = System.nanoTime();
        RewriteOutput output = properties.configured()
                ? requestRewrite(run.getId(), storyContext, request, stylePrompt)
                : demoCall(run.getId(), "REWRITE_SELECTION", () -> demoRewrite(request));
        run.setDraft(output.rewrittenText().strip());
        run.setStatus("COMPLETED");
        run.setCurrentStep("REWRITE_READY");
        run.setTotalDurationMs(sumStepDuration(run.getId()) + elapsed(started));
        runRepository.saveAndFlush(run);
        addStep(run.getId(), 2, "REWRITE_SELECTION", "COMPLETED",
                "已生成局部修改版本，等待作者确认", elapsed(started));

        ChapterRevisionEntity revision = new ChapterRevisionEntity();
        revision.setNovelId(novelId);
        revision.setChapterId(chapterId);
        revision.setAgentRunId(run.getId());
        revision.setRevisionType(request.mode());
        revision.setInstruction(request.normalizedInstruction());
        revision.setOriginalText(request.selectedText());
        revision.setRevisedText(output.rewrittenText().strip());
        revision.setStartOffset(request.startOffset());
        revision.setEndOffset(request.endOffset());
        revision.setStatus("GENERATED");
        try {
            revision.setChangeSummaryJson(objectMapper.writeValueAsString(output.changeSummary()));
            revision.setWarningsJson(objectMapper.writeValueAsString(output.warnings()));
        } catch (Exception e) { throw new AiProviderException("修改结果保存失败", e); }
        return toRevisionResponse(revisionRepository.saveAndFlush(revision));
    }

    @Transactional(readOnly = true)
    public List<ChapterRevisionResponse> listRevisions(long chapterId) {
        requireChapter(chapterId);
        return revisionRepository.findByChapterIdOrderByCreatedAtDesc(chapterId).stream()
                .map(this::toRevisionResponse).toList();
    }

    @Transactional
    public ChapterRevisionResponse decideRevision(long revisionId, RevisionDecisionRequest request) {
        ChapterRevisionEntity revision = requireRevision(revisionId);
        if (!Objects.equals(revision.getVersion(), request.version()))
            throw new WritingConflictException("修改记录状态已变化，请刷新后重试");
        boolean allowed = switch (request.status()) {
            case "ACCEPTED", "REJECTED" -> "GENERATED".equals(revision.getStatus());
            case "ROLLED_BACK" -> "ACCEPTED".equals(revision.getStatus());
            default -> false;
        };
        if (!allowed) throw new WritingConflictException("当前修改记录不能变更为 " + request.status());
        revision.setStatus(request.status());
        ChapterRevisionEntity saved = revisionRepository.saveAndFlush(revision);
        consistencyIssueRepository.findByRevisionId(revisionId).ifPresent(issue -> {
            issue.setStatus(switch (request.status()) {
                case "ACCEPTED" -> "FIXED";
                case "REJECTED", "ROLLED_BACK" -> "OPEN";
                default -> issue.getStatus();
            });
            consistencyIssueRepository.saveAndFlush(issue);
        });
        return toRevisionResponse(saved);
    }

    @Transactional(noRollbackFor = AiProviderException.class)
    public ConsistencyReportResponse checkConsistency(long novelId, long chapterId, ConsistencyCheckRequest request) {
        NovelEntity novel = requireNovel(novelId);
        ChapterEntity chapter = requireChapter(chapterId);
        if (!chapter.getNovelId().equals(novelId)) throw new WritingNotFoundException("章节不属于当前小说");
        if (chapter.getContent().isBlank()) throw new WritingConflictException("当前章节没有正文，暂时无法检查");

        AgentRunEntity run = new AgentRunEntity();
        run.setNovelId(novelId); run.setChapterId(chapterId); run.setStatus("RUNNING");
        run.setCurrentStep("LOAD_CONTEXT"); run.setGuidance(request.normalizedFocus());
        run.setProviderMode(properties.configured() ? "ai" : "demo");
        run.setOperation("CONSISTENCY_CHECK"); run.setModel(activeModel()); run.setTotalDurationMs(0L);
        runRepository.saveAndFlush(run);

        long started = System.nanoTime();
        String context = buildContext(novel, chapter, request.normalizedFocus());
        addStep(run.getId(), 1, "LOAD_CONTEXT", "COMPLETED", "已读取故事资料和最近章节", elapsed(started));
        run.setCurrentStep("CHECK_CONSISTENCY"); runRepository.saveAndFlush(run);

        started = System.nanoTime();
        ConsistencyOutput output = properties.configured()
                ? requestConsistency(run.getId(), context, chapter, request.normalizedFocus())
                : demoCall(run.getId(), "CHECK_CONSISTENCY", () -> demoConsistency(chapter));
        run.setStatus("COMPLETED"); run.setCurrentStep("CONSISTENCY_REPORT_READY");
        run.setTotalDurationMs(sumStepDuration(run.getId()) + elapsed(started));
        runRepository.saveAndFlush(run);
        addStep(run.getId(), 2, "CHECK_CONSISTENCY", "COMPLETED",
                "一致性检查完成，发现 " + output.issues().size() + " 个问题", elapsed(started));

        ConsistencyReportEntity report = new ConsistencyReportEntity();
        report.setNovelId(novelId); report.setChapterId(chapterId); report.setAgentRunId(run.getId());
        report.setSourceContentVersion(chapter.getContentVersion());
        report.setScore(Math.max(0, Math.min(100, output.score()))); report.setSummary(output.summary().strip());
        report.setStatus("COMPLETED"); report.setProviderMode(run.getProviderMode());
        consistencyReportRepository.saveAndFlush(report);
        for (ConsistencyIssueOutput item : output.issues()) {
            if (item.quote().isBlank() || !chapter.getContent().contains(item.quote())) continue;
            ConsistencyIssueEntity issue = new ConsistencyIssueEntity();
            issue.setReportId(report.getId()); issue.setType(item.type().strip()); issue.setSeverity(item.severity());
            issue.setQuoteText(item.quote()); issue.setMessage(item.message().strip());
            issue.setSuggestion(item.suggestion().strip()); issue.setStatus("OPEN");
            consistencyIssueRepository.save(issue);
        }
        consistencyIssueRepository.flush();
        return toConsistencyReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<ConsistencyReportResponse> listConsistencyReports(long chapterId) {
        requireChapter(chapterId);
        return consistencyReportRepository.findByChapterIdOrderByCreatedAtDesc(chapterId).stream()
                .map(this::toConsistencyReportResponse).toList();
    }

    @Transactional
    public ConsistencyIssueResponse decideIssue(long issueId, IssueDecisionRequest request) {
        ConsistencyIssueEntity issue = requireConsistencyIssue(issueId);
        if (!Objects.equals(issue.getVersion(), request.version()))
            throw new WritingConflictException("问题状态已变化，请刷新后重试");
        if ("FIXED".equals(issue.getStatus())) throw new WritingConflictException("已修复的问题不能直接忽略");
        issue.setStatus(request.status());
        return toConsistencyIssueResponse(consistencyIssueRepository.saveAndFlush(issue));
    }

    @Transactional(noRollbackFor = AiProviderException.class)
    public ConsistencyIssueResponse fixIssue(long issueId, IssueFixRequest request) {
        ConsistencyIssueEntity issue = requireConsistencyIssue(issueId);
        if (!"OPEN".equals(issue.getStatus())) throw new WritingConflictException("只有待处理问题可以生成修复版本");
        ConsistencyReportEntity report = requireConsistencyReport(issue.getReportId());
        ChapterEntity chapter = requireChapter(report.getChapterId());
        int start = chapter.getContent().indexOf(issue.getQuoteText());
        if (start < 0 || chapter.getContent().indexOf(issue.getQuoteText(), start + 1) >= 0)
            throw new WritingConflictException("无法在当前正文中唯一定位问题片段，请重新执行一致性检查");
        int end = start + issue.getQuoteText().length();
        String instruction = "修复一致性问题：" + issue.getMessage() + "。建议：" + issue.getSuggestion();
        if (!request.normalizedInstruction().isBlank()) instruction += "。作者要求：" + request.normalizedInstruction();
        RewriteRequest rewriteRequest = new RewriteRequest(issue.getQuoteText(),
                chapter.getContent().substring(Math.max(0, start - 4000), start),
                chapter.getContent().substring(end, Math.min(chapter.getContent().length(), end + 4000)),
                "REWRITE", instruction, start, end, false, null);
        ChapterRevisionResponse revision = rewrite(report.getNovelId(), report.getChapterId(), rewriteRequest);
        issue.setRevisionId(revision.id()); issue.setStatus("WAITING_APPROVAL");
        ConsistencyIssueEntity saved = consistencyIssueRepository.saveAndFlush(issue);
        return toConsistencyIssueResponse(saved);
    }

    @Transactional(noRollbackFor = AiProviderException.class)
    public ChapterMemoryResponse extractChapterMemory(long chapterId, ExtractChapterMemoryRequest request) {
        ChapterEntity chapter = requireChapter(chapterId);
        NovelEntity novel = requireNovel(chapter.getNovelId());
        if (chapter.getContent().isBlank()) throw new WritingConflictException("当前章节没有正文，无法提取章节记忆");
        log.info("开始提取章节记忆 novelId={} chapterId={} chapterNo={} contentVersion={} contentChars={} skillVersion={} thinkingEnabled={}",
                novel.getId(), chapterId, chapter.getChapterNo(), chapter.getContentVersion(),
                chapter.getContent().length(), chapterCompressionSkill.version(), request.normalizedThinkingEnabled());

        AgentRunEntity run = new AgentRunEntity();
        run.setNovelId(novel.getId()); run.setChapterId(chapterId); run.setStatus("RUNNING");
        run.setCurrentStep("EXTRACT_CHAPTER_MEMORY"); run.setGuidance(request.normalizedInstruction());
        run.setProviderMode(properties.configured() ? "ai" : "demo");
        run.setOperation("CHAPTER_MEMORY"); run.setModel(activeModel()); run.setTotalDurationMs(0L);
        runRepository.saveAndFlush(run);

        long started = System.nanoTime();
        ChapterMemoryContent content;
        try {
            content = properties.configured()
                    ? requestChapterMemory(run.getId(), novel, chapter, request.normalizedInstruction(),
                            request.normalizedThinkingEnabled())
                    : demoCall(run.getId(), "EXTRACT_CHAPTER_MEMORY", () -> demoChapterMemory(chapter));
        }
        catch (AiProviderException error) {
            telemetry.failRun(run.getId(), "EXTRACT_CHAPTER_MEMORY", elapsed(started), error.getMessage());
            throw error;
        }
        log.info("章节记忆模型提取完成 runId={} chapterId={} scenes={} characters={} importantFacts={} elapsedMs={}",
                run.getId(), chapterId, content.scenes().size(), content.characters().size(),
                content.importantFacts().size(), elapsed(started));
        run.setStatus("COMPLETED"); run.setCurrentStep("CHAPTER_MEMORY_WAITING_APPROVAL");
        run.setTotalDurationMs(elapsed(started));
        runRepository.saveAndFlush(run);
        addStep(run.getId(), 1, "EXTRACT_CHAPTER_MEMORY", "COMPLETED",
                "章节记忆已提取，等待作者确认", elapsed(started));

        ChapterMemoryEntity memory = new ChapterMemoryEntity();
        memory.setNovelId(novel.getId()); memory.setChapterId(chapterId); memory.setChapterNo(chapter.getChapterNo());
        memory.setSourceChapterVersion(chapter.getContentVersion()); memory.setStatus("GENERATED");
        memory.setProviderMode(run.getProviderMode());
        applyMemoryContent(memory, content);
        ChapterMemoryEntity saved = chapterMemoryRepository.saveAndFlush(memory);
        chapter.setStatus("PENDING_FINAL");
        chapter.setCompletedAt(null);
        chapterRepository.saveAndFlush(chapter);
        log.info("章节记忆草稿保存完成 memoryId={} chapterId={} sourceVersion={} status={}",
                saved.getId(), chapterId, saved.getSourceChapterVersion(), saved.getStatus());
        return toChapterMemoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public ChapterMemoryResponse getChapterMemory(long chapterId) {
        requireChapter(chapterId);
        return chapterMemoryRepository.findFirstByChapterIdOrderByCreatedAtDesc(chapterId)
                .map(this::toChapterMemoryResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ChapterMemoryResponse> listChapterMemories(long novelId) {
        requireNovel(novelId);
        return chapterMemoryRepository.findByNovelIdAndStatusOrderByChapterNoAsc(novelId, "CONFIRMED")
                .stream().map(this::toChapterMemoryResponse).toList();
    }

    @Transactional
    public ChapterMemoryResponse confirmChapterMemory(long memoryId, ConfirmChapterMemoryRequest request) {
        ChapterMemoryEntity memory = requireChapterMemory(memoryId);
        log.info("开始确认章节记忆 memoryId={} novelId={} chapterId={} sourceVersion={} skillVersion={}",
                memoryId, memory.getNovelId(), memory.getChapterId(), memory.getSourceChapterVersion(),
                request.content().skillVersion());
        if (!Objects.equals(memory.getVersion(), request.version()))
            throw new WritingConflictException("章节记忆已经变化，请刷新后重试");
        if (!"GENERATED".equals(memory.getStatus()) && !"CONFIRMED".equals(memory.getStatus()))
            throw new WritingConflictException("当前章节记忆不能确认");
        ChapterEntity chapter = requireChapter(memory.getChapterId());
        if (!Objects.equals(chapter.getContentVersion(), memory.getSourceChapterVersion()))
            throw new WritingConflictException("提取后章节正文已经修改，请重新提取章节记忆");
        for (ChapterMemoryEntity previous : chapterMemoryRepository.findByChapterIdAndStatus(memory.getChapterId(), "CONFIRMED")) {
            if (!previous.getId().equals(memoryId)) previous.setStatus("SUPERSEDED");
        }
        applyMemoryContent(memory, request.content());
        memory.setStatus("CONFIRMED");
        chapterMemoryRepository.flush();
        ChapterMemoryEntity saved = chapterMemoryRepository.saveAndFlush(memory);
        chapter.setStatus("FINALIZED");
        chapter.setCompletedAt(LocalDateTime.now());
        chapterRepository.saveAndFlush(chapter);
        storyVolumeContextService.syncConfirmedChapterMemory(saved, request.content());
        characterHistoryService.applyConfirmedMemory(saved, request.content());
        if (!"demo".equals(saved.getProviderMode())) chapterMemoryVectorizer.replace(saved, request.content());
        log.info("章节记忆确认完成 memoryId={} chapterId={} vectorized={} status={}", saved.getId(),
                saved.getChapterId(), !"demo".equals(saved.getProviderMode()), saved.getStatus());
        return toChapterMemoryResponse(saved);
    }

    private String buildContext(NovelEntity novel, ChapterEntity selected, String... contextHints) {
        return buildContext(novel, selected, true, contextHints);
    }

    private String buildDirectorContext(NovelEntity novel, ChapterEntity selected, String... contextHints) {
        return buildContext(novel, selected, false, contextHints);
    }

    private String buildContext(NovelEntity novel, ChapterEntity selected, boolean includeOlderMemories,
                                String... contextHints) {
        StringBuilder text = new StringBuilder();
        text.append("【作品】").append(novel.getTitle()).append('\n');
        append(text, "简介", novel.getDescription());
        append(text, "总纲", novel.getOutline());
        append(text, "立项约束", projectContext(novel.getId()));
        List<ChapterEntity> chapters = chapterRepository.findByNovelIdOrderByChapterNoAsc(novel.getId());
        String volumeContext = storyVolumeContextService.currentConfirmedVolume(novel.getId(), selected.getId());
        if (!volumeContext.isBlank()) text.append('\n').append(volumeContext);
        int selectedIndex = 0;
        for (int i = 0; i < chapters.size(); i++) if (chapters.get(i).getId().equals(selected.getId())) selectedIndex = i;
        List<String> contextSignals = new ArrayList<>();
        for (int i = Math.max(0, selectedIndex - 2); i <= selectedIndex; i++)
            contextSignals.add(chapters.get(i).getContent());
        if (contextHints != null) contextSignals.addAll(List.of(contextHints));
        CharacterContextService.Selection characterContext = characterContextService.selectAtChapter(
                novel.getId(), selected.getChapterNo(), contextSignals.toArray(String[]::new));
        if (!characterContext.promptContext().isBlank()) {
            text.append("\n【本次涉及的人物档案】\n").append(characterContext.promptContext()).append('\n');
        }
        OrganizationContextService.Selection organizationContext = organizationContextService.select(
                novel.getId(), contextSignals.toArray(String[]::new));
        if (!organizationContext.promptContext().isBlank()) {
            text.append("\n【本次涉及的组织档案】\n").append(organizationContext.promptContext()).append('\n');
        }
        StringBuilder worldSettingText = new StringBuilder();
        List<WorldSettingEntity> settings = worldSettingRepository.findByNovelIdOrderByIdAsc(novel.getId());
        if (!settings.isEmpty()) {
            worldSettingText.append("\n【世界观设定】\n");
            for (WorldSettingEntity s : settings)
                worldSettingText.append("- [").append(s.getCategory()).append("] ").append(s.getTitle()).append("：").append(s.getContent()).append('\n');
        }
        String storyBible = text.toString();
        String worldSettings = worldSettingText.length() <= WORLD_SETTING_CONTEXT_LIMIT
                ? worldSettingText.toString()
                : worldSettingText.substring(0, WORLD_SETTING_CONTEXT_LIMIT);
        String memories = "";
        if (includeOlderMemories) {
            StringBuilder memoryText = new StringBuilder("\n【较早章节的结构化记忆】\n");
            for (ChapterMemoryEntity memory : chapterMemoryRepository
                    .findByNovelIdAndStatusOrderByChapterNoAsc(novel.getId(), "CONFIRMED")) {
                if (memory.getChapterNo() >= selected.getChapterNo() - 2) continue;
                ChapterMemoryContent content = readMemoryContent(memory);
                memoryText.append("第").append(memory.getChapterNo()).append("章：").append(content.summary()).append('\n');
                if (!content.characters().isEmpty()) {
                    memoryText.append("人物：");
                    for (MemoryCharacter c : content.characters())
                        memoryText.append(c.name()).append(c.stateChange() == null ? "" : "（" + c.stateChange() + "）").append("；");
                    memoryText.append('\n');
                }
                for (MemoryForeshadowing f : content.foreshadowings())
                    memoryText.append("伏笔[").append(f.status()).append("]：").append(f.content()).append('\n');
                for (MemoryImportantFact fact : content.importantFacts())
                    memoryText.append("重要信息[").append(fact.type()).append("]：").append(fact.content()).append('\n');
            }
            memories = keepTail(memoryText.toString(), 6000);
        }
        StringBuilder recentText = new StringBuilder("\n【最近正文】\n");
        int recentChapterCount = 0;
        for (int i = Math.max(0, selectedIndex - 2); i <= selectedIndex; i++) {
            ChapterEntity c = chapters.get(i);
            recentChapterCount++;
            recentText.append("第").append(c.getChapterNo()).append("章 ").append(c.getTitle()).append("：\n")
                    .append(keepTail(c.getContent(), RECENT_CHAPTER_CONTEXT_LIMIT)).append("\n\n");
        }
        String context = storyBible + worldSettings + memories + recentText;
        log.info("创作上下文构建完成 novelId={} chapterId={} storyBibleChars={} worldSettingSourceChars={} "
                        + "worldSettingChars={} memoryChars={} recentChapterCount={} recentChapterChars={} totalChars={}",
                novel.getId(), selected.getId(), storyBible.length(), worldSettingText.length(), worldSettings.length(),
                memories.length(), recentChapterCount, recentText.length(), context.length());
        return context;
    }

    private RagContextService.RagContext retrieveDirectorRag(NovelEntity novel, ChapterEntity chapter,
                                                             List<ChapterDirectorMessageEntity> messages,
                                                             String currentMessage) {
        int beforeChapterOrder = chapter.getChapterNo() - 2;
        Set<Long> validOlderChapterIds = chapterRepository.findByNovelIdOrderByChapterNoAsc(novel.getId()).stream()
                .filter(item -> item.getChapterNo() < beforeChapterOrder)
                .map(ChapterEntity::getId)
                .collect(Collectors.toSet());
        String dialogueQuery = buildDirectorRagDialogueQuery(messages, currentMessage);
        return ragContextService.retrieveOlderChapters(novel.getId(), chapter.getId(),
                beforeChapterOrder, validOlderChapterIds, chapter.getContent(), dialogueQuery);
    }

    private String buildDirectorRagDialogueQuery(List<ChapterDirectorMessageEntity> messages,
                                                 String currentMessage) {
        StringBuilder query = new StringBuilder();
        int start = Math.max(0, messages.size() - DIRECTOR_RAG_MESSAGE_LIMIT);
        for (int index = start; index < messages.size(); index++) {
            ChapterDirectorMessageEntity message = messages.get(index);
            String role = "USER".equals(message.getRole()) ? "作者" : "导演";
            query.append(role).append("：").append(message.getContent()).append('\n');
        }
        if (currentMessage != null && !currentMessage.isBlank()) {
            query.append("作者当前要求：").append(currentMessage.trim());
        }
        String result = query.toString().trim();
        if (result.length() > DIRECTOR_RAG_DIALOGUE_LIMIT) {
            result = result.substring(result.length() - DIRECTOR_RAG_DIALOGUE_LIMIT);
        }
        return result;
    }

    /** 当前章节局部正文由 RewriteRequest 单独提供，这里按需构建作品约束。 */
    private RewriteContext buildRewriteContext(NovelEntity novel, RewriteRequest request) {
        StringBuilder text = new StringBuilder();
        text.append("【作品】").append(novel.getTitle()).append('\n');
        appendWithinLimit(text, "简介", novel.getDescription(), REWRITE_STORY_CONTEXT_LIMIT);
        appendWithinLimit(text, "立项约束", projectContext(novel.getId()), REWRITE_STORY_CONTEXT_LIMIT);

        CharacterContextService.Selection characterContext = characterContextService.select(novel.getId(),
                request.normalizedBeforeContext(), request.selectedText(), request.normalizedAfterContext(),
                request.normalizedInstruction());
        if (!characterContext.promptContext().isBlank() && text.length() < REWRITE_STORY_CONTEXT_LIMIT) {
            appendRawWithinLimit(text, "\n【本段涉及的人物档案】\n", REWRITE_STORY_CONTEXT_LIMIT);
            appendRawWithinLimit(text, characterContext.promptContext() + '\n', REWRITE_STORY_CONTEXT_LIMIT);
        }

        OrganizationContextService.Selection organizationContext = organizationContextService.select(novel.getId(),
                request.normalizedBeforeContext(), request.selectedText(), request.normalizedAfterContext(),
                request.normalizedInstruction());
        if (!organizationContext.promptContext().isBlank() && text.length() < REWRITE_STORY_CONTEXT_LIMIT) {
            appendRawWithinLimit(text, "\n【本段涉及的组织档案】\n", REWRITE_STORY_CONTEXT_LIMIT);
            appendRawWithinLimit(text, organizationContext.promptContext() + '\n', REWRITE_STORY_CONTEXT_LIMIT);
        }

        if (text.length() < REWRITE_STORY_CONTEXT_LIMIT) {
            List<WorldSettingEntity> settings = worldSettingRepository.findByNovelIdOrderByIdAsc(novel.getId());
            if (!settings.isEmpty()) {
                appendRawWithinLimit(text, "\n【世界观约束】\n", REWRITE_STORY_CONTEXT_LIMIT);
                for (WorldSettingEntity setting : settings) {
                    String line = "- [" + setting.getCategory() + "] " + setting.getTitle()
                            + "：" + setting.getContent() + '\n';
                    appendRawWithinLimit(text, line, REWRITE_STORY_CONTEXT_LIMIT);
                }
            }
        }
        appendWithinLimit(text, "总纲", novel.getOutline(), REWRITE_STORY_CONTEXT_LIMIT);
        return new RewriteContext(text.toString(), characterContext.characterNames(), organizationContext.organizationNames());
    }

    private void appendWithinLimit(StringBuilder text, String title, String value, int limit) {
        if (!hasText(value) || text.length() >= limit) return;
        String section = "【" + title + "】" + value + '\n';
        int remaining = limit - text.length();
        text.append(section, 0, Math.min(remaining, section.length()));
    }

    private void appendRawWithinLimit(StringBuilder text, String value, int limit) {
        if (!hasText(value) || text.length() >= limit) return;
        int remaining = limit - text.length();
        text.append(value, 0, Math.min(remaining, value.length()));
    }

    private String projectContext(long novelId) {
        return projectProfileRepository.findByNovelId(novelId).map(profile -> {
            StringBuilder text = new StringBuilder();
            append(text, "题材与频道", joinNonBlank("｜", profile.getGenre(), profile.getChannel(), profile.getPlatform()));
            append(text, "目标读者", profile.getTargetAudience());
            append(text, "核心卖点", profile.getCoreSellingPoint());
            append(text, "主角金手指与限制", profile.getProtagonistHook());
            append(text, "升级路线", profile.getGrowthRoute());
            append(text, "读者期待", profile.getReaderExpectations());
            append(text, "开篇三章设计", profile.getOpeningThreeChapters());
            if (profile.getExpectedWords() != null || profile.getExpectedVolumes() != null || profile.getChapterWordTarget() != null) {
                text.append("规模：预计").append(profile.getExpectedWords() == null ? "未定" : profile.getExpectedWords() + "字")
                        .append("，").append(profile.getExpectedVolumes() == null ? "卷数未定" : profile.getExpectedVolumes() + "卷")
                        .append("，单章").append(profile.getChapterWordTarget() == null ? "字数未定" : profile.getChapterWordTarget() + "字").append('\n');
            }
            return text.length() <= 10_000 ? text.toString() : text.substring(0, 10_000);
        }).orElse("");
    }

    private String joinNonBlank(String delimiter, String... values) {
        return java.util.Arrays.stream(values).filter(this::hasText).map(String::trim).collect(Collectors.joining(delimiter));
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }

    private record RewriteContext(String text, List<String> characterNames, List<String> organizationNames) {}

    private String planCharacterSignal(ChapterPlan plan) {
        if (plan == null) return "";
        return String.join("\n", plan.characters()) + '\n' + plan.objective() + '\n'
                + plan.opening() + '\n' + String.join("\n", plan.developments()) + '\n'
                + plan.endingHook() + '\n' + String.join("\n", plan.continuityNotes());
    }

    private ChapterPlan requestPlan(long runId, String context, String guidance) {
        String system = """
                你是网络小说创作导演 Agent。基于作品资料和最近正文规划下一段/下一章，必须尊重已有人物名、能力、关系、世界规则和故事总纲。
                只输出一个合法 JSON 对象，不使用 Markdown，不解释。字段必须是：
                title(string), objective(string), opening(string), developments(string数组，3到5项), endingHook(string), characters(string数组), continuityNotes(string数组)。
                不得从其他故事借用人名；资料没有姓名时使用身份称谓，不擅自命名。
                """;
        String user = context + "\n【作者本次要求】\n" + (guidance.isBlank() ? "根据当前情节自然推进" : guidance);
        String raw = callProvider(runId, "PLAN_CHAPTER", system, user, 0.55, 0,
                new ProviderOptions(true, false, 0));
        try { return objectMapper.readValue(extractJson(raw), ChapterPlan.class); }
        catch (Exception firstError) {
            log.warn("章节计划首次响应解析失败 runId={} rawChars={} reason={}，准备自动重试",
                    runId, raw == null ? 0 : raw.length(), firstError.getMessage());
            String retryRaw = callProvider(runId, "PLAN_CHAPTER_RETRY",
                    system + "\n上一次响应不是完整合法的 JSON。本次请重新生成完整结果，使用紧凑 JSON，确保所有字符串和数组闭合。",
                    user, 0.35, 0, new ProviderOptions(true, false, 0));
            try { return objectMapper.readValue(extractJson(retryRaw), ChapterPlan.class); }
            catch (Exception retryError) {
                throw new AiProviderException("AI 两次返回的章节计划格式都不完整，请重试", retryError);
            }
        }
    }

    private DirectorTurnOutput requestDirectorTurn(long runId, String context,
                                                   List<ChapterDirectorMessageEntity> history,
                                                   ChapterPlan currentPlan, String message,
                                                   boolean thinkingEnabled, boolean generatePlan,
                                                   Consumer<String> onTextDelta) {
        if (!generatePlan) {
            return requestDirectorDiscussion(runId, context, history, currentPlan, message,
                    thinkingEnabled, onTextDelta);
        }
        String system = """
                你是网络小说的章节导演，负责与作者多轮讨论并调整“当前这一章”的主线计划。
                每一轮都必须在上一版计划上修改，除非作者明确要求推倒重来；不得擅自改变已经确定的人物身份、能力、关系和世界规则。
                你只规划本章，不直接写小说正文。作者提出局部修改时，保留未被要求修改的内容。
                只输出合法 JSON 对象，不使用 Markdown，不解释。字段必须是：
                reply(string，简洁回应本轮如何处理), changeSummary(string数组，列出本轮实际变化), plan(object)。
                plan 字段必须包含：title, objective, opening, developments(3到5项字符串), endingHook, characters, continuityNotes。
                示例结构：{"reply":"已保留开场并调整冲突推进","changeSummary":["第三个推进节点改为主角主动设局"],"plan":{"title":"章节名","objective":"本章目标","opening":"开场","developments":["推进1","推进2","推进3"],"endingHook":"结尾钩子","characters":["人物"],"continuityNotes":["约束"]}}。
                不得从其他故事借用人名；资料没有姓名时使用身份称谓，不擅自命名。
                """;
        String planJson;
        try { planJson = currentPlan == null ? "尚未形成计划" : objectMapper.writeValueAsString(currentPlan); }
        catch (Exception error) { throw new AiProviderException("当前章节计划格式不正确", error); }
        String user = context + "\n【当前章节主线计划】\n" + planJson
                + "\n【最近导演对话】\n" + directorHistory(history)
                + "\n【作者本轮要求】\n" + message;
        log.info("章节导演调用 runId={} thinkingEnabled={} contextChars={} historyCount={}",
                runId, thinkingEnabled, context.length(), history.size());
        String raw = callProvider(runId, "REVISE_CHAPTER_PLAN", system, user, 0.45, 0,
                new ProviderOptions(true, thinkingEnabled, 0));
        try {
            return parseDirectorTurn(raw, currentPlan);
        } catch (AiProviderException firstError) {
            log.warn("章节导演首次响应不可用 runId={} rawChars={}，准备关闭推理并完整重做",
                    runId, raw == null ? 0 : raw.length());
            String retrySystem = system + """

                    上一次响应在 JSON 中途结束或结构不完整。本次必须根据同一份当前计划和作者要求重新完整生成：
                    - 使用紧凑 JSON，不要换行和缩进；
                    - 所有字符串、数组和对象必须正确闭合；
                    - 不要省略 plan 的任何必需字段；
                    - 不要复述这些说明。
                    """;
            String retryRaw = callProvider(runId, "REVISE_CHAPTER_PLAN_RETRY", retrySystem, user,
                    0.3, 0, new ProviderOptions(true, false, 0));
            try {
                DirectorTurnOutput output = parseDirectorTurn(retryRaw, currentPlan);
                log.info("章节导演自动重试成功 runId={} rawChars={}", runId, retryRaw.length());
                return output;
            } catch (AiProviderException retryError) {
                throw new AiProviderException("AI 两次返回的章节主线格式都不完整，请重新发送要求", retryError);
            }
        }
    }

    private DirectorTurnOutput requestDirectorDiscussion(long runId, String context,
                                                         List<ChapterDirectorMessageEntity> history,
                                                         ChapterPlan currentPlan, String message,
                                                         boolean thinkingEnabled,
                                                         Consumer<String> onTextDelta) {
        String system = """
                你是网络小说的章节导演，正在与作者讨论“当前这一章”应该怎么写。
                本轮只回应作者、分析取舍、提出必要的追问或给出少量可选方向，不生成完整的章节主线，不输出结构化计划。
                尊重已确定的人物身份、能力、关系、世界规则和作者此前的要求；不要直接写小说正文。
                回答应简洁、具体，通常控制在三到六段。作者没有要求时，不要把开场、发展、结尾逐项概括一遍。
                """;
        String planJson;
        try { planJson = currentPlan == null ? "尚未生成主线" : objectMapper.writeValueAsString(currentPlan); }
        catch (Exception error) { throw new AiProviderException("当前章节计划格式不正确", error); }
        String user = context + "\n【当前已保存的章节主线】\n" + planJson
                + "\n【最近导演对话】\n" + directorHistory(history)
                + "\n【作者本轮消息】\n" + message;
        log.info("章节导演讨论调用 runId={} thinkingEnabled={} contextChars={} historyCount={}",
                runId, thinkingEnabled, context.length(), history.size());
        String reply = callProviderStreaming(runId, "DISCUSS_CHAPTER", system, user, 0.55,
                new ProviderOptions(false, thinkingEnabled, 0), onTextDelta);
        return new DirectorTurnOutput(reply.strip(), null, List.of());
    }

    DirectorTurnOutput parseDirectorTurn(String raw, ChapterPlan currentPlan) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            JsonNode planNode = firstNode(root, "plan", "chapterPlan", "currentPlan", "章节计划", "主线计划");
            if (planNode == null && (root.has("objective") || root.has("本章目标"))) planNode = root;
            if (planNode == null || !planNode.isObject())
                throw new IllegalArgumentException("响应中缺少plan对象");
            ChapterPlan plan = tolerantPlan(planNode, currentPlan);
            String reply = firstText(root, "reply", "assistantMessage", "message", "response", "回复");
            if (reply.isBlank()) reply = "已根据你的要求更新本章主线，你可以继续提出调整。";
            List<String> changes = firstStringList(root, "changeSummary", "changes", "change_summary", "修改摘要", "变更");
            if (changes.isEmpty()) changes = List.of("已根据本轮要求更新本章主线");
            return new DirectorTurnOutput(reply, plan, changes);
        } catch (Exception error) {
            log.warn("章节导演响应解析失败 rawChars={} reason={}", raw == null ? 0 : raw.length(), error.getMessage());
            throw new AiProviderException("AI 已返回内容，但章节主线格式无法解析，请重试", error);
        }
    }

    private ChapterPlan tolerantPlan(JsonNode node, ChapterPlan fallback) {
        String title = valueOr(firstText(node, "title", "标题", "章节名"), fallback == null ? "本章主线" : fallback.title());
        String objective = valueOr(firstText(node, "objective", "本章目标", "目标"), fallback == null ? "推进当前情节" : fallback.objective());
        String opening = valueOr(firstText(node, "opening", "开场", "开篇"), fallback == null ? "承接当前章节正文" : fallback.opening());
        List<String> developments = firstStringList(node, "developments", "情节推进", "发展", "剧情发展");
        if (developments.isEmpty() && fallback != null) developments = fallback.developments();
        if (developments.isEmpty()) developments = List.of("人物确认当前处境", "人物采取行动并遭遇阻力", "冲突推动新的选择");
        String endingHook = valueOr(firstText(node, "endingHook", "ending_hook", "结尾钩子", "钩子"), fallback == null ? "留下与主线相关的新悬念" : fallback.endingHook());
        List<String> characters = firstStringList(node, "characters", "出场人物", "人物");
        if (characters.isEmpty() && fallback != null) characters = fallback.characters();
        List<String> notes = firstStringList(node, "continuityNotes", "continuity_notes", "连续性约束", "约束");
        if (notes.isEmpty() && fallback != null) notes = fallback.continuityNotes();
        return new ChapterPlan(title, objective, opening, developments, endingHook, characters, notes);
    }

    private JsonNode firstNode(JsonNode node, String... names) {
        for (String name : names) if (node.has(name) && !node.path(name).isNull()) return node.path(name);
        return null;
    }

    private String firstText(JsonNode node, String... names) {
        JsonNode value = firstNode(node, names);
        return value != null && value.isValueNode() ? value.asText("").strip() : "";
    }

    private List<String> firstStringList(JsonNode node, String... names) {
        JsonNode value = firstNode(node, names);
        if (value == null) return List.of();
        if (value.isTextual()) return value.asText().lines().map(String::strip).filter(s -> !s.isBlank()).toList();
        if (!value.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            String text = item.isValueNode() ? item.asText("").strip()
                    : firstText(item, "text", "content", "event", "description", "summary", "value", "内容");
            if (!text.isBlank()) result.add(text);
        }
        return result;
    }

    private String valueOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    private void markDirectorRunFailed(AgentRunEntity run, String errorMessage) {
        run.setStatus("FAILED");
        run.setCurrentStep("DIRECTOR_CHAT_FAILED");
        run.setErrorMessage(errorMessage);
        runRepository.saveAndFlush(run);
        log.warn("章节导演对话失败 runId={} chapterId={} reason={}", run.getId(), run.getChapterId(), errorMessage);
    }

    private String directorHistory(List<ChapterDirectorMessageEntity> messages) {
        if (messages.isEmpty()) return "这是第一轮对话";
        StringBuilder history = new StringBuilder();
        int start = Math.max(0, messages.size() - 12);
        for (int index = start; index < messages.size(); index++) {
            ChapterDirectorMessageEntity item = messages.get(index);
            history.append("USER".equals(item.getRole()) ? "作者：" : "导演：")
                    .append(item.getContent()).append('\n');
        }
        return keepTail(history.toString(), 6000);
    }

    private String requestDraft(long runId, String context, ChapterPlan plan, int length, String stylePrompt) {
        String system = stylePrompt + "\n\n" + """
                你是网络小说正文写作 Agent。根据作者已确认的章节计划续写正文。
                只输出可直接插入编辑器的小说正文，不加标题、不解释、不使用 Markdown。
                严格延续原文人物、视角、语气和世界规则；不得借用其他故事人名，不得重复原文。
                目标长度约 %d 个汉字，完整推进计划并以自然钩子收束。
                """.formatted(length);
        try {
            int maxTokens = Math.min(properties.draftMaxTokens(), Math.max(4096, length * 3));
            return callProvider(runId, "WRITE_DRAFT", system, context + "\n【已确认章节计划】\n" + objectMapper.writeValueAsString(plan),
                    0.8, maxTokens, new ProviderOptions(false, true, properties.draftMaxTokens()));
        } catch (AiProviderException e) { throw e; }
        catch (Exception e) { throw new AiProviderException("章节计划序列化失败", e); }
    }

    private RewriteOutput requestRewrite(long runId, String storyContext, RewriteRequest request, String stylePrompt) {
        String modeInstruction = switch (request.mode()) {
            case "POLISH" -> "润色表达，增强语言质感和节奏，但不得改变任何剧情事实";
            case "REWRITE" -> "保留人物、事件和事实，用更自然有表现力的方式重新表达";
            case "EXPAND" -> "扩写所选内容，增加合适的动作、感官、心理或环境细节，不额外推进关键剧情";
            case "SHORTEN" -> "压缩冗余表达，保留所有关键事实、人物动作和必要信息";
            case "TONE" -> "按照作者要求调整氛围、语气和节奏，不改变剧情事实";
            case "CUSTOM" -> "严格按照作者的自定义要求修改";
            default -> throw new WritingConflictException("不支持的修改方式");
        };
        String system = stylePrompt + "\n\n" + """
                你是网络小说段落编辑 Agent。你的任务是局部修改作者选中的正文，并让结果能无缝接回前后文。
                必须遵守小说人物、视角、世界观和既有事实，不得从其他故事借用人名，不得解释创作过程。
                本次模式：%s。
                只输出一个合法 JSON 对象，不使用 Markdown。字段必须是：
                rewrittenText(string，修改后的替换文本), changeSummary(string数组，概括实际改动), warnings(string数组，存在可能改变事实等风险时说明，否则空数组)。
                rewrittenText 只能包含替换选区的正文，不能包含选区前后的内容。
                """.formatted(modeInstruction);
        String user = storyContext
                + "\n【紧邻选区的前文】\n" + request.normalizedBeforeContext()
                + "\n【需要修改的原文】\n" + request.selectedText()
                + "\n【紧邻选区的后文】\n" + request.normalizedAfterContext()
                + "\n【作者的额外要求】\n" + (request.normalizedInstruction().isBlank() ? "无" : request.normalizedInstruction());
        int initialMaxTokens = request.useThinking()
                ? 16_384
                : Math.min(8_000, Math.max(4_096, request.selectedText().length() * 4));
        String raw = callProvider(runId, "REWRITE_SELECTION", system, user, 0.65, initialMaxTokens,
                new ProviderOptions(true, request.useThinking(), request.useThinking() ? 32_768 : 8_192));
        try { return objectMapper.readValue(extractJson(raw), RewriteOutput.class); }
        catch (Exception e) { throw new AiProviderException("AI 返回的修改结果格式不正确，请重试", e); }
    }

    private ConsistencyOutput requestConsistency(long runId, String context, ChapterEntity chapter, String focus) {
        String system = """
                你是长篇网络小说的一致性审校 Agent。检查当前章节是否与人物档案、世界观、故事大纲和前文冲突。
                检查人物姓名与能力、性格和关系、时间线与地点、世界规则、剧情大纲、叙事视角、逻辑断裂和明显重复。
                只输出一个合法 JSON 对象，不使用 Markdown。字段必须是：
                score(integer,0到100), summary(string), issues(array)。每个 issue 必须包含：
                type(string), severity(HIGH或MEDIUM或LOW), quote(string), message(string), suggestion(string)。
                quote 必须逐字复制当前章节中的一段连续原文，且尽量短并能唯一定位；没有问题时 issues 返回空数组。
                不要为了凑数虚构问题，不要把个人文风偏好标成设定冲突。
                """;
        String user = context + "\n【本次必须检查的当前章节全文】\n" + keepTail(chapter.getContent(), 12000)
                + "\n【作者指定检查重点】\n" + (focus.isBlank() ? "全面检查" : focus);
        String raw = callProvider(runId, "CHECK_CONSISTENCY", system, user, 0.2, 4096);
        try { return objectMapper.readValue(extractJson(raw), ConsistencyOutput.class); }
        catch (Exception e) { throw new AiProviderException("AI 返回的一致性报告格式不正确，请重试", e); }
    }

    private ChapterMemoryContent requestChapterMemory(long runId, NovelEntity novel, ChapterEntity chapter,
                                                       String instruction, boolean thinkingEnabled) {
        String user = """
                小说名：%s
                章节序号：第%d章
                章节名：%s
                正文版本：%d
                作者补充要求：%s

                【已有角色名称映射】
                %s
                正文中的简称和别名必须归入对应的正式姓名；不得把同一人物拆成两个角色。

                <chapter>
                %s
                </chapter>
                """.formatted(novel.getTitle(), chapter.getChapterNo(), chapter.getTitle(), chapter.getContentVersion(),
                instruction.isBlank() ? "无" : instruction,
                characterNameResolver.promptGuide(novel.getId()).isBlank()
                        ? "暂无人物档案" : characterNameResolver.promptGuide(novel.getId()),
                keepTail(chapter.getContent(), 20_000));
        int initialMaxTokens = thinkingEnabled ? 0 : 4_096;
        ProviderOptions memoryOptions = new ProviderOptions(true, thinkingEnabled, 0);
        String raw = callProvider(runId, "EXTRACT_CHAPTER_MEMORY", chapterCompressionSkill.systemPrompt(), user,
                0.1, initialMaxTokens, memoryOptions);
        try {
            ChapterMemoryContent parsed = objectMapper.readValue(extractJson(raw), ChapterMemoryContent.class);
            return normalizeCharacterReferences(novel.getId(), chapterCompressionSkill.normalize(parsed));
        }
        catch (Exception firstError) {
            log.warn("章节记忆响应解析失败，准备自动重试 runId={} chapterId={} rawChars={} reason={}",
                    runId, chapter.getId(), raw == null ? 0 : raw.length(), firstError.getMessage());
            String retrySystem = chapterCompressionSkill.systemPrompt() + """


                    【格式修复要求】
                    上一次响应无法解析。请重新分析原章节，并输出更紧凑的完整 JSON。
                    必须闭合每一个字符串、对象和数组；不得使用 Markdown；不得在 JSON 前后添加解释。
                    内容过长时应缩短各字段文字，不得通过省略右括号或截断 JSON 来减少长度。
                    """;
            String retryRaw = callProvider(runId, "EXTRACT_CHAPTER_MEMORY_RETRY", retrySystem, user,
                    0.0, 4_096, new ProviderOptions(true, false, 4_096));
            try {
                ChapterMemoryContent parsed = objectMapper.readValue(extractJson(retryRaw), ChapterMemoryContent.class);
                ChapterMemoryContent normalized = normalizeCharacterReferences(novel.getId(), chapterCompressionSkill.normalize(parsed));
                log.info("章节记忆自动重试成功 runId={} chapterId={} rawChars={}",
                        runId, chapter.getId(), retryRaw.length());
                return normalized;
            }
            catch (Exception retryError) {
                log.warn("章节记忆自动重试仍解析失败 runId={} chapterId={} rawChars={} reason={}",
                        runId, chapter.getId(), retryRaw == null ? 0 : retryRaw.length(), retryError.getMessage());
                throw new AiProviderException("AI 两次返回的章节记忆格式都不完整，请重试", retryError);
            }
        }
    }

    private String callProvider(Long runId, String stepType, String system, String user,
                                double temperature, int initialMaxTokens) {
        return callProvider(runId, stepType, system, user, temperature, initialMaxTokens,
                new ProviderOptions(false, true, 8_192));
    }

    private String callProvider(Long runId, String stepType, String system, String user,
                                double temperature, int initialMaxTokens, ProviderOptions options) {
        long started = System.nanoTime();
        int attempts = 0;
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        int maxTokens = initialMaxTokens;
        try {
            while (true) {
                attempts++;
                Map<String, Object> payload = new java.util.LinkedHashMap<>();
                payload.put("model", properties.model());
                payload.put("temperature", temperature);
                if (maxTokens > 0) payload.put("max_tokens", maxTokens);
                payload.put("messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)));
                payload.put("thinking", Map.of("type", options.thinkingEnabled() ? "enabled" : "disabled"));
                if (options.thinkingEnabled()) payload.put("reasoning_effort", "high");
                if (options.jsonOutput()) payload.put("response_format", Map.of("type", "json_object"));
                HttpRequest request = HttpRequest.newBuilder(URI.create(properties.chatCompletionsUrl()))
                        .timeout(properties.timeout()).header("Authorization", "Bearer " + properties.apiKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8)).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300)
                    throw new AiProviderException("AI服务调用失败，状态码：" + response.statusCode());
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode usage = root.path("usage");
                int attemptPromptTokens = usage.path("prompt_tokens").asInt(0);
                int attemptCompletionTokens = usage.path("completion_tokens").asInt(0);
                int attemptTotalTokens = usage.path("total_tokens").asInt(0);
                int reasoningTokens = usage.path("completion_tokens_details").path("reasoning_tokens").asInt(-1);
                promptTokens += attemptPromptTokens;
                completionTokens += attemptCompletionTokens;
                totalTokens += attemptTotalTokens;
                JsonNode choice = root.path("choices").path(0);
                JsonNode message = choice.path("message");
                String content = extractMessageContent(message.path("content")).strip();
                String finishReason = choice.path("finish_reason").asText("unknown");
                int reasoningLength = message.path("reasoning_content").asText("").length();
                log.info("AI服务响应 runId={} stepType={} attempt={} thinkingEnabled={} maxTokens={} finishReason={} "
                                + "promptTokens={} completionTokens={} reasoningTokens={} reasoningChars={} contentChars={}",
                        runId, stepType, attempts, options.thinkingEnabled(), maxTokens, finishReason,
                        attemptPromptTokens, attemptCompletionTokens, reasoningTokens, reasoningLength, content.length());
                if (!content.isBlank()) {
                    if (runId != null) telemetry.recordCall(runId, stepType, activeModel(), "COMPLETED", promptTokens,
                            completionTokens, totalTokens, elapsed(started), attempts - 1, null);
                    return content;
                }
                if (attempts == 1 && maxTokens > 0 && maxTokens < options.retryMaxTokens()
                        && ("length".equals(finishReason) || reasoningLength > 0)) {
                    maxTokens = options.retryMaxTokens();
                    continue;
                }
                String detail = reasoningLength > 0 ? "，模型已生成 " + reasoningLength + " 字推理过程但没有最终答案" : "";
                throw new AiProviderException("AI服务没有返回最终内容（finish_reason=" + finishReason + detail + "），请重试或检查模型配置");
            }
        } catch (Exception exception) {
            AiProviderException error;
            if (exception instanceof AiProviderException aiError) error = aiError;
            else if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt(); error = new AiProviderException("AI Agent 请求已取消", exception);
            } else error = new AiProviderException("暂时无法连接AI服务", exception);
            if (runId != null) {
                telemetry.recordCall(runId, stepType, activeModel(), "FAILED", promptTokens,
                        completionTokens, totalTokens, elapsed(started), Math.max(0, attempts - 1), error.getMessage());
                telemetry.failRun(runId, stepType, elapsed(started), error.getMessage());
            }
            throw error;
        }
    }

    private String callProviderStreaming(Long runId, String stepType, String system, String user,
                                         double temperature, ProviderOptions options,
                                         Consumer<String> onTextDelta) {
        long started = System.nanoTime();
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        String finishReason = "unknown";
        int reasoningLength = 0;
        StringBuilder content = new StringBuilder();
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", properties.model());
            payload.put("temperature", temperature);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)));
            payload.put("thinking", Map.of("type", options.thinkingEnabled() ? "enabled" : "disabled"));
            if (options.thinkingEnabled()) payload.put("reasoning_effort", "high");
            if (options.jsonOutput()) payload.put("response_format", Map.of("type", "json_object"));
            payload.put("stream", true);
            payload.put("stream_options", Map.of("include_usage", true));

            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.chatCompletionsUrl()))
                    .timeout(properties.timeout()).header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new AiProviderException("AI服务调用失败，状态码：" + response.statusCode());
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
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
                    JsonNode choice = chunk.path("choices").path(0);
                    if (choice.isMissingNode()) continue;
                    String chunkFinishReason = choice.path("finish_reason").asText("");
                    if (!chunkFinishReason.isBlank()) finishReason = chunkFinishReason;
                    JsonNode delta = choice.path("delta");
                    reasoningLength += delta.path("reasoning_content").asText("").length();
                    String textDelta = extractMessageContent(delta.path("content"));
                    if (textDelta.isEmpty()) continue;
                    content.append(textDelta);
                    onTextDelta.accept(textDelta);
                }
            }
            if (content.toString().isBlank()) {
                String detail = reasoningLength > 0 ? "，模型已生成 " + reasoningLength + " 字推理过程但没有最终答案" : "";
                throw new AiProviderException("AI服务没有返回最终内容（finish_reason=" + finishReason + detail + "）");
            }
            if (runId != null) telemetry.recordCall(runId, stepType, activeModel(), "COMPLETED", promptTokens,
                    completionTokens, totalTokens, elapsed(started), 0, null);
            log.info("AI流式响应完成 runId={} stepType={} thinkingEnabled={} finishReason={} promptTokens={} "
                            + "completionTokens={} reasoningChars={} contentChars={} elapsedMs={}",
                    runId, stepType, options.thinkingEnabled(), finishReason, promptTokens, completionTokens,
                    reasoningLength, content.length(), elapsed(started));
            return content.toString();
        } catch (Exception exception) {
            AiProviderException error;
            if (exception instanceof AiProviderException aiError) error = aiError;
            else if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                error = new AiProviderException("AI Agent 请求已取消", exception);
            } else error = new AiProviderException("AI流式连接中断，请重试", exception);
            if (runId != null) {
                telemetry.recordCall(runId, stepType, activeModel(), "FAILED", promptTokens,
                        completionTokens, totalTokens, elapsed(started), 0, error.getMessage());
                telemetry.failRun(runId, stepType, elapsed(started), error.getMessage());
            }
            throw error;
        }
    }

    private record ProviderOptions(boolean jsonOutput, boolean thinkingEnabled, int retryMaxTokens) {}

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

    private ChapterPlan demoPlan(ChapterEntity chapter, String guidance) {
        String objective = guidance.isBlank() ? "承接当前悬念，让人物通过一次新的发现推动情节" : guidance;
        return new ChapterPlan(chapter.getTitle() + "·后续", objective,
                "从当前场景中一个被忽略的异常细节切入",
                List.of("人物确认异常并作出行动", "行动遭遇阻力，暴露新的信息", "人物必须在有限时间内作出选择"),
                "以一个尚未得到解释的新线索结束",
                List.of("沿用当前章节已有角色"), List.of("保持人物称谓、能力与当前视角一致"));
    }

    private DirectorTurnOutput demoDirectorTurn(ChapterEntity chapter, ChapterPlan currentPlan, String message) {
        ChapterPlan base = currentPlan == null ? demoPlan(chapter, message) : currentPlan;
        ChapterPlan revised = new ChapterPlan(base.title(), message, base.opening(), base.developments(),
                base.endingHook(), base.characters(), base.continuityNotes());
        return new DirectorTurnOutput("我保留了现有结构，并按照你的要求调整了本章核心目标。",
                revised, List.of("将本章目标调整为：" + message));
    }

    private String demoDraft(ChapterPlan plan) {
        return "风从半开的窗缝里挤进来，桌角那张纸忽然颤了一下。直到这时，他才发现纸背还压着一道极淡的痕迹。\n\n那不是随手留下的划痕，更像某种仓促写下又被刻意擦去的记号。他没有立刻伸手，而是先看向门外。走廊安静得反常，连方才一直响着的脚步声也不知何时停了。\n\n他把纸移到灯下，倾斜着辨认。模糊的线条逐渐拼成一个方向，恰好指向此前无人留意的地方。这个发现并没有带来答案，反而让原本确定的事情多出了一处无法解释的缺口。\n\n门外忽然响起轻轻两下叩门声。间隔分毫不差，像是来人早已知道他会看到这里。";
    }

    private RewriteOutput demoRewrite(RewriteRequest request) {
        String original = request.selectedText().strip();
        String revised = switch (request.mode()) {
            case "SHORTEN" -> original.replaceAll("非常|十分|其实|似乎|仿佛", "");
            case "EXPAND" -> original + "周围的声音在这一刻忽然清晰起来，连空气里细微的变化也变得无法忽略。";
            default -> original.replace("忽然", "蓦地").replace("说道", "低声说道");
        };
        if (revised.equals(original)) revised = original + "这一瞬间，某种难以言明的不安悄然浮了上来。";
        return new RewriteOutput(revised, List.of("根据所选模式调整了表达和节奏"),
                List.of("当前为演示模式，配置 API Key 后将结合完整故事资料改写"));
    }

    private ConsistencyOutput demoConsistency(ChapterEntity chapter) {
        String quote = chapter.getContent().strip().substring(0, Math.min(18, chapter.getContent().strip().length()));
        return new ConsistencyOutput(88, "演示检查已完成。配置 API Key 后会结合人物、世界观、大纲和前文进行真实审校。",
                List.of(new ConsistencyIssueOutput("STYLE", "LOW", quote,
                        "这是演示模式生成的示例问题，用于体验检查和修复流程。", "在不改变事实的前提下优化表达和节奏。")));
    }

    private ChapterMemoryContent normalizeCharacterReferences(long novelId, ChapterMemoryContent content) {
        Map<String, MemoryCharacter> mergedCharacters = new LinkedHashMap<>();
        for (MemoryCharacter item : content.characters()) {
            StoryCharacterEntity resolved = characterNameResolver.resolve(novelId, item.name());
            String canonicalName = resolved == null ? item.name().trim() : resolved.getName();
            MemoryCharacter normalized = new MemoryCharacter(canonicalName, item.role(), item.actions(),
                    item.stateChange(), item.newKnowledge(), resolved == null && Boolean.TRUE.equals(item.firstAppearance()),
                    item.foreshadowings(), item.profileChanges());
            mergedCharacters.merge(canonicalName, normalized, this::mergeMemoryCharacter);
            if (resolved != null && !item.name().trim().equals(resolved.getName()))
                log.info("章节人物名称已归一 novelId={} mention={} canonical={}", novelId, item.name(), resolved.getName());
        }
        List<MemoryScene> scenes = content.scenes().stream().map(scene -> new MemoryScene(
                scene.sceneIndex(), scene.title(), scene.boundaryType(), scene.continuityKey(), scene.timeSpan(),
                scene.locations(), scene.characters().stream().map(name -> canonicalCharacterName(novelId, name)).distinct().toList(),
                scene.goal(), scene.conflict(), scene.subEvents(), scene.result())).toList();
        List<IdentityReveal> reveals = content.identityReveals().stream().map(reveal -> new IdentityReveal(
                reveal.identityName(), canonicalCharacterName(novelId, reveal.realCharacterName()), reveal.evidence(),
                reveal.confidence(), reveal.applyMerge())).toList();
        return new ChapterMemoryContent(content.skillVersion(), content.summary(), content.timeInfo(), content.locations(),
                List.copyOf(mergedCharacters.values()), scenes, content.importantFacts(), content.keyEvents(),
                content.foreshadowings(), content.unresolvedQuestions(), content.plotProgress(), content.importance(), reveals);
    }

    private MemoryCharacter mergeMemoryCharacter(MemoryCharacter left, MemoryCharacter right) {
        return new MemoryCharacter(left.name(), mergeText(left.role(), right.role()),
                mergeText(left.actions(), right.actions()), mergeText(left.stateChange(), right.stateChange()),
                mergeText(left.newKnowledge(), right.newKnowledge()),
                Boolean.TRUE.equals(left.firstAppearance()) || Boolean.TRUE.equals(right.firstAppearance()),
                mergeDistinct(left.foreshadowings(), right.foreshadowings()),
                mergeDistinct(left.profileChanges(), right.profileChanges()));
    }

    private <T> List<T> mergeDistinct(List<T> left, List<T> right) {
        LinkedHashSet<T> values = new LinkedHashSet<>(left);
        values.addAll(right);
        return List.copyOf(values);
    }

    private String mergeText(String left, String right) {
        if (left == null || left.isBlank()) return right;
        if (right == null || right.isBlank() || left.contains(right)) return left;
        if (right.contains(left)) return right;
        return left + "；" + right;
    }

    private String canonicalCharacterName(long novelId, String mention) {
        StoryCharacterEntity resolved = characterNameResolver.resolve(novelId, mention);
        return resolved == null ? mention : resolved.getName();
    }

    private ChapterMemoryContent demoChapterMemory(ChapterEntity chapter) {
        String compact = chapter.getContent().replaceAll("\\s+", "").strip();
        String summary = compact.substring(0, Math.min(120, compact.length()));
        if (compact.length() > 120) summary += "……";
        ChapterMemoryContent demo = new ChapterMemoryContent(chapterCompressionSkill.version(), summary,
                "本章时间待作者确认", List.of(),
                List.of(new MemoryCharacter("本章已有角色", "待确认", "请配置 API Key 后自动提取",
                        "待确认", "待确认", false)),
                List.of(new MemoryScene(1, "本章主要场景", "HARD", "本章主要事件",
                        "待确认", List.of(), List.of("本章已有角色"), "待确认", "待确认",
                        List.of(new MemorySceneEvent("待确认", "待确认", summary)), "待确认")),
                List.of(new MemoryImportantFact("UNRESOLVED_THREAD", "本章尚未解决的问题待确认", "MEDIUM")),
                List.of(), List.of(), List.of(), "当前为演示模式", "MEDIUM");
        return chapterCompressionSkill.normalize(demo);
    }

    private void addStep(long runId, int no, String type, String status, String summary, long durationMs) {
        AgentStepEntity step = new AgentStepEntity();
        step.setRunId(runId); step.setStepNo(no); step.setType(type); step.setStatus(status);
        step.setSummary(summary); step.setDurationMs(durationMs); stepRepository.saveAndFlush(step);
    }

    private int nextStepNo(long runId) { return stepRepository.findByRunIdOrderByStepNoAsc(runId).size() + 1; }
    private long elapsed(long started) { return Duration.ofNanos(System.nanoTime() - started).toMillis(); }
    private long sumStepDuration(long runId) {
        return stepRepository.findByRunIdOrderByStepNoAsc(runId).stream()
                .map(AgentStepEntity::getDurationMs).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
    }

    private String activeModel() { return properties.configured() ? properties.model() : "demo"; }

    private <T> T demoCall(long runId, String stepType, Supplier<T> supplier) {
        long started = System.nanoTime();
        T result = supplier.get();
        telemetry.recordCall(runId, stepType, "demo", "COMPLETED", 0, 0, 0,
                elapsed(started), 0, null);
        return result;
    }
    private String keepTail(String text, int max) { return text.length() <= max ? text : text.substring(text.length() - max); }
    private String extractJson(String raw) {
        int start = raw.indexOf('{'), end = raw.lastIndexOf('}');
        return start >= 0 && end > start ? raw.substring(start, end + 1) : raw;
    }
    private void append(StringBuilder b, String name, String value) { if (value != null && !value.isBlank()) b.append("【").append(name).append("】").append(value).append('\n'); }
    private void appendInline(StringBuilder b, String value) { if (value != null && !value.isBlank()) b.append("；").append(value); }
    private NovelEntity requireNovel(long id) { return novelRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("小说不存在：" + id)); }
    private ChapterEntity requireChapter(long id) { return chapterRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("章节不存在：" + id)); }
    private AgentRunEntity requireRun(long id) { return runRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("Agent任务不存在：" + id)); }
    private ChapterRevisionEntity requireRevision(long id) { return revisionRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("修改记录不存在：" + id)); }
    private ConsistencyReportEntity requireConsistencyReport(long id) { return consistencyReportRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("一致性报告不存在：" + id)); }
    private ConsistencyIssueEntity requireConsistencyIssue(long id) { return consistencyIssueRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("一致性问题不存在：" + id)); }
    private ChapterMemoryEntity requireChapterMemory(long id) { return chapterMemoryRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("章节记忆不存在：" + id)); }

    private ChapterRevisionResponse toRevisionResponse(ChapterRevisionEntity revision) {
        return new ChapterRevisionResponse(revision.getId(), revision.getNovelId(), revision.getChapterId(), revision.getAgentRunId(),
                revision.getRevisionType(), revision.getInstruction(), revision.getOriginalText(), revision.getRevisedText(),
                revision.getStartOffset(), revision.getEndOffset(), revision.getStatus(),
                readStringList(revision.getChangeSummaryJson()), readStringList(revision.getWarningsJson()), revision.getVersion(),
                revision.getCreatedAt(), revision.getUpdatedAt());
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)); }
        catch (Exception e) { throw new AiProviderException("修改记录内容无法读取", e); }
    }

    private ChapterPlan readDirectorPlan(String json) {
        if (json == null || json.isBlank()) return null;
        try { return objectMapper.readValue(json, ChapterPlan.class); }
        catch (Exception error) { throw new AiProviderException("章节导演当前计划无法读取", error); }
    }

    private void persistDirectorPlan(ChapterEntity chapter, ChapterPlan plan) {
        ChapterDirectorSessionEntity session = directorSessionRepository.findByChapterId(chapter.getId()).orElseGet(() -> {
            ChapterDirectorSessionEntity created = new ChapterDirectorSessionEntity();
            created.setNovelId(chapter.getNovelId());
            created.setChapterId(chapter.getId());
            return created;
        });
        try { session.setCurrentPlanJson(objectMapper.writeValueAsString(plan)); }
        catch (Exception error) { throw new AiProviderException("章节主线保存失败", error); }
        directorSessionRepository.saveAndFlush(session);
        if ("DRAFT".equals(chapter.getStatus()) || "UNPLANNED".equals(chapter.getStatus())) {
            chapter.setStatus(chapter.getContent().isBlank() ? "PLANNED" : "WRITING");
            chapterRepository.saveAndFlush(chapter);
        }
    }

    private DirectorConversationResponse toDirectorConversation(ChapterDirectorSessionEntity session) {
        List<DirectorMessageResponse> messages = directorMessageRepository
                .findBySessionIdOrderByIdAsc(session.getId()).stream().map(this::toDirectorMessage).toList();
        return new DirectorConversationResponse(session.getChapterId(), messages,
                readDirectorPlan(session.getCurrentPlanJson()), session.getCurrentRunId());
    }

    private DirectorMessageResponse toDirectorMessage(ChapterDirectorMessageEntity message) {
        ChapterPlan plan = readDirectorPlan(message.getPlanJson());
        List<String> changes = message.getChangeSummaryJson() == null
                ? List.of() : readStringList(message.getChangeSummaryJson());
        return new DirectorMessageResponse(message.getId(), message.getRole(), message.getContent(),
                message.getRunId(), plan, changes, message.getCreatedAt());
    }

    private ConsistencyReportResponse toConsistencyReportResponse(ConsistencyReportEntity report) {
        List<ConsistencyIssueResponse> issues = consistencyIssueRepository.findByReportIdOrderByIdAsc(report.getId())
                .stream().map(this::toConsistencyIssueResponse).toList();
        ChapterEntity chapter = requireChapter(report.getChapterId());
        boolean stale = !Objects.equals(chapter.getContentVersion(), report.getSourceContentVersion());
        return new ConsistencyReportResponse(report.getId(), report.getNovelId(), report.getChapterId(), report.getAgentRunId(),
                report.getSourceContentVersion(), stale, report.getScore(), report.getSummary(), report.getStatus(),
                report.getProviderMode(), issues, report.getCreatedAt());
    }

    private ConsistencyIssueResponse toConsistencyIssueResponse(ConsistencyIssueEntity issue) {
        ChapterRevisionResponse revision = issue.getRevisionId() == null ? null
                : revisionRepository.findById(issue.getRevisionId()).map(this::toRevisionResponse).orElse(null);
        return new ConsistencyIssueResponse(issue.getId(), issue.getReportId(), issue.getType(), issue.getSeverity(),
                issue.getQuoteText(), issue.getMessage(), issue.getSuggestion(), issue.getStatus(), issue.getRevisionId(),
                issue.getVersion(), issue.getCreatedAt(), issue.getUpdatedAt(), revision);
    }

    private void applyMemoryContent(ChapterMemoryEntity memory, ChapterMemoryContent content) {
        memory.setSummary(content.summary().strip());
        memory.setTimeInfo(content.timeInfo() == null ? null : content.timeInfo().strip());
        memory.setPlotProgress(content.plotProgress() == null ? null : content.plotProgress().strip());
        memory.setImportance(content.importance());
        try { memory.setContentJson(objectMapper.writeValueAsString(content)); }
        catch (Exception e) { throw new AiProviderException("章节记忆保存失败", e); }
    }

    private ChapterMemoryContent readMemoryContent(ChapterMemoryEntity memory) {
        try { return objectMapper.readValue(memory.getContentJson(), ChapterMemoryContent.class); }
        catch (Exception e) { throw new AiProviderException("章节记忆内容无法读取", e); }
    }

    private ChapterMemoryResponse toChapterMemoryResponse(ChapterMemoryEntity memory) {
        ChapterEntity chapter = requireChapter(memory.getChapterId());
        boolean stale = !Objects.equals(chapter.getContentVersion(), memory.getSourceChapterVersion());
        return new ChapterMemoryResponse(memory.getId(), memory.getNovelId(), memory.getChapterId(), memory.getChapterNo(),
                memory.getSourceChapterVersion(), memory.getStatus(), readMemoryContent(memory), memory.getProviderMode(),
                stale, memory.getVersion(), memory.getCreatedAt(), memory.getUpdatedAt());
    }

    private AgentRunResponse toResponse(AgentRunEntity run) {
        ChapterPlan plan = null;
        try { if (run.getPlanJson() != null) plan = objectMapper.readValue(run.getPlanJson(), ChapterPlan.class); }
        catch (Exception e) { throw new AiProviderException("已保存的章节计划无法读取", e); }
        List<AgentStepResponse> steps = stepRepository.findByRunIdOrderByStepNoAsc(run.getId()).stream()
                .map(s -> new AgentStepResponse(s.getId(), s.getStepNo(), s.getType(), s.getStatus(), s.getSummary(), s.getDurationMs(), s.getCreatedAt())).toList();
        List<AiCallResponse> calls = callRepository.findByRunIdOrderByIdAsc(run.getId()).stream()
                .map(this::toCallResponse).toList();
        int promptTokens = calls.stream().map(AiCallResponse::promptTokens).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int completionTokens = calls.stream().map(AiCallResponse::completionTokens).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int totalTokens = calls.stream().map(AiCallResponse::totalTokens).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int retryCount = calls.stream().map(AiCallResponse::retryCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        return new AgentRunResponse(run.getId(), run.getNovelId(), run.getChapterId(), run.getStatus(), run.getCurrentStep(),
                run.getOperation(), run.getProviderMode(), run.getModel(), promptTokens, completionTokens, totalTokens,
                run.getTotalDurationMs(), retryCount, run.getErrorMessage(), plan, run.getDraft(), steps, calls,
                run.getCreatedAt(), run.getUpdatedAt());
    }

    private AiCallResponse toCallResponse(AiCallLogEntity call) {
        return new AiCallResponse(call.getId(), call.getStepType(), call.getModel(), call.getStatus(),
                call.getPromptTokens(), call.getCompletionTokens(), call.getTotalTokens(), call.getDurationMs(),
                call.getRetryCount(), call.getErrorMessage(), call.getCreatedAt());
    }

    private AgentRunSummaryResponse toSummaryResponse(AgentRunEntity run) {
        List<AiCallLogEntity> calls = callRepository.findByRunIdOrderByIdAsc(run.getId());
        int promptTokens = calls.stream().map(AiCallLogEntity::getPromptTokens).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int completionTokens = calls.stream().map(AiCallLogEntity::getCompletionTokens).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int totalTokens = calls.stream().map(AiCallLogEntity::getTotalTokens).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int retryCount = calls.stream().map(AiCallLogEntity::getRetryCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        String chapterTitle = chapterRepository.findById(run.getChapterId()).map(ChapterEntity::getTitle).orElse("章节已删除");
        return new AgentRunSummaryResponse(run.getId(), run.getNovelId(), run.getChapterId(), chapterTitle,
                run.getOperation(), run.getStatus(), run.getCurrentStep(), run.getProviderMode(), run.getModel(),
                promptTokens, completionTokens, totalTokens, run.getTotalDurationMs(), retryCount,
                run.getErrorMessage(), run.getCreatedAt());
    }
}
