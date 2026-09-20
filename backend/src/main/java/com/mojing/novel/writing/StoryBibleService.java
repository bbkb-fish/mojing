package com.mojing.novel.writing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.mojing.novel.writing.AgentDtos.*;

@Service
public class StoryBibleService {
    private final NovelRepository novelRepository;
    private final StoryCharacterRepository characterRepository;
    private final StoryOrganizationRepository organizationRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final StoryVolumeRepository storyVolumeRepository;
    private final StoryPartRepository storyPartRepository;
    private final ChapterRepository chapterRepository;
    private final CharacterHistoryService characterHistoryService;

    public StoryBibleService(NovelRepository novelRepository, StoryCharacterRepository characterRepository,
                             StoryOrganizationRepository organizationRepository,
                             WorldSettingRepository worldSettingRepository,
                             StoryVolumeRepository storyVolumeRepository,
                             StoryPartRepository storyPartRepository,
                             ChapterRepository chapterRepository,
                             CharacterHistoryService characterHistoryService) {
        this.novelRepository = novelRepository;
        this.characterRepository = characterRepository;
        this.organizationRepository = organizationRepository;
        this.worldSettingRepository = worldSettingRepository;
        this.storyVolumeRepository = storyVolumeRepository;
        this.storyPartRepository = storyPartRepository;
        this.chapterRepository = chapterRepository;
        this.characterHistoryService = characterHistoryService;
    }

    @Transactional(readOnly = true)
    public List<CharacterResponse> listCharacters(long novelId) {
        requireNovel(novelId);
        return characterRepository.findByNovelIdAndStatusOrderByIdAsc(novelId, "ACTIVE").stream().map(this::toResponse).toList();
    }

    @Transactional
    public CharacterResponse createCharacter(long novelId, CharacterRequest request) {
        requireNovel(novelId);
        StoryCharacterEntity entity = new StoryCharacterEntity();
        entity.setNovelId(novelId);
        apply(entity, request);
        return toResponse(characterRepository.saveAndFlush(entity));
    }

    @Transactional
    public CharacterResponse updateCharacter(long id, CharacterRequest request) {
        StoryCharacterEntity entity = requireCharacter(id);
        checkVersion(entity.getVersion(), request.version());
        apply(entity, request);
        return toResponse(characterRepository.saveAndFlush(entity));
    }

    @Transactional
    public void deleteCharacter(long id) { characterRepository.delete(requireCharacter(id)); }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations(long novelId) {
        requireNovel(novelId);
        return organizationRepository.findByNovelIdOrderByIdAsc(novelId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrganizationResponse createOrganization(long novelId, OrganizationRequest request) {
        requireNovel(novelId);
        StoryOrganizationEntity entity = new StoryOrganizationEntity();
        entity.setNovelId(novelId);
        apply(entity, request);
        return toResponse(organizationRepository.saveAndFlush(entity));
    }

    @Transactional
    public OrganizationResponse updateOrganization(long id, OrganizationRequest request) {
        StoryOrganizationEntity entity = requireOrganization(id);
        checkVersion(entity.getVersion(), request.version());
        apply(entity, request);
        return toResponse(organizationRepository.saveAndFlush(entity));
    }

    @Transactional
    public void deleteOrganization(long id) { organizationRepository.delete(requireOrganization(id)); }

    @Transactional(readOnly = true)
    public List<WorldSettingResponse> listWorldSettings(long novelId) {
        requireNovel(novelId);
        return worldSettingRepository.findByNovelIdOrderByIdAsc(novelId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public WorldSettingResponse createWorldSetting(long novelId, WorldSettingRequest request) {
        requireNovel(novelId);
        WorldSettingEntity entity = new WorldSettingEntity();
        entity.setNovelId(novelId);
        apply(entity, request);
        return toResponse(worldSettingRepository.saveAndFlush(entity));
    }

    @Transactional
    public WorldSettingResponse updateWorldSetting(long id, WorldSettingRequest request) {
        WorldSettingEntity entity = requireWorldSetting(id);
        checkVersion(entity.getVersion(), request.version());
        apply(entity, request);
        return toResponse(worldSettingRepository.saveAndFlush(entity));
    }

    @Transactional
    public void deleteWorldSetting(long id) { worldSettingRepository.delete(requireWorldSetting(id)); }

    @Transactional(readOnly = true)
    public List<StoryVolumeResponse> listStoryVolumes(long novelId) {
        requireNovel(novelId);
        return storyVolumeRepository.findByNovelIdOrderByVolumeNoAsc(novelId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public StoryVolumeResponse createStoryVolume(long novelId, StoryVolumeRequest request) {
        requireNovel(novelId);
        validateStoryVolume(novelId, null, request);
        StoryVolumeEntity entity = new StoryVolumeEntity();
        entity.setNovelId(novelId);
        apply(entity, request);
        return toResponse(storyVolumeRepository.saveAndFlush(entity));
    }

    @Transactional
    public StoryVolumeResponse updateStoryVolume(long id, StoryVolumeRequest request) {
        StoryVolumeEntity entity = requireStoryVolume(id);
        checkVersion(entity.getVersion(), request.version());
        validateStoryVolume(entity.getNovelId(), id, request);
        apply(entity, request);
        return toResponse(storyVolumeRepository.saveAndFlush(entity));
    }

    @Transactional
    public void deleteStoryVolume(long id) { storyVolumeRepository.delete(requireStoryVolume(id)); }

    @Transactional(readOnly = true)
    public List<StoryPartResponse> listStoryParts(long volumeId) {
        requireStoryVolume(volumeId);
        return storyPartRepository.findByVolumeIdOrderByPartNoAsc(volumeId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public StoryPartResponse createStoryPart(long volumeId, StoryPartRequest request) {
        StoryVolumeEntity volume = requireStoryVolume(volumeId);
        validateStoryPart(volume, null, request);
        StoryPartEntity entity = new StoryPartEntity();
        entity.setNovelId(volume.getNovelId());
        entity.setVolumeId(volumeId);
        apply(entity, request);
        return toResponse(storyPartRepository.saveAndFlush(entity));
    }

    @Transactional
    public StoryPartResponse updateStoryPart(long id, StoryPartRequest request) {
        StoryPartEntity entity = requireStoryPart(id);
        checkVersion(entity.getVersion(), request.version());
        StoryVolumeEntity volume = requireStoryVolume(entity.getVolumeId());
        validateStoryPart(volume, id, request);
        apply(entity, request);
        return toResponse(storyPartRepository.saveAndFlush(entity));
    }

    @Transactional
    public void deleteStoryPart(long id) { storyPartRepository.delete(requireStoryPart(id)); }

    private void apply(StoryCharacterEntity entity, CharacterRequest request) {
        entity.setName(request.name().trim());
        entity.setAliases(normalize(request.aliases()));
        entity.setRole(normalize(request.role()));
        entity.setAffiliations(normalize(request.affiliations()));
        entity.setDescription(normalize(request.description()));
        entity.setPersonality(normalize(request.personality()));
        entity.setGoal(normalize(request.goal()));
        entity.setCurrentState(normalize(request.currentState()));
        entity.setRelationships(normalize(request.relationships()));
    }

    private void apply(StoryOrganizationEntity entity, OrganizationRequest request) {
        entity.setName(request.name().trim());
        entity.setAliases(normalize(request.aliases()));
        entity.setType(normalize(request.type()));
        entity.setDescription(normalize(request.description()));
        entity.setGoal(normalize(request.goal()));
        entity.setStructure(normalize(request.structure()));
        entity.setRelationships(normalize(request.relationships()));
    }

    private void apply(WorldSettingEntity entity, WorldSettingRequest request) {
        entity.setCategory(request.category().trim());
        entity.setTitle(request.title().trim());
        entity.setContent(request.content().trim());
    }

    private void apply(StoryVolumeEntity entity, StoryVolumeRequest request) {
        entity.setVolumeNo(request.volumeNo());
        entity.setTitle(request.title().trim());
        entity.setChapterStart(request.chapterStart());
        entity.setChapterEnd(request.chapterEnd());
        entity.setObjective(normalize(request.objective()));
        entity.setRetrospective(normalize(request.retrospective()));
        entity.setFuturePlan(normalize(request.futurePlan()));
        entity.setKeyTurningPoints(normalize(request.keyTurningPoints()));
        entity.setClimax(normalize(request.climax()));
        entity.setEndingHook(normalize(request.endingHook()));
        entity.setForeshadows(normalize(request.foreshadows()));
        entity.setLockedBeats(normalize(request.lockedBeats()));
        entity.setAnalyzedThroughChapterNo(request.analyzedThroughChapterNo());
        entity.setSourceContentVersionSum(contentVersionSum(entity.getNovelId(), request.chapterStart(),
                request.analyzedThroughChapterNo()));
        entity.setStatus(request.status());
    }

    private void apply(StoryPartEntity entity, StoryPartRequest request) {
        entity.setPartNo(request.partNo());
        entity.setTitle(request.title().trim());
        entity.setChapterStart(request.chapterStart());
        entity.setChapterEnd(request.chapterEnd());
        entity.setObjective(normalize(request.objective()));
        entity.setPlan(normalize(request.plan()));
        entity.setRetrospective(normalize(request.retrospective()));
        entity.setStatus(request.status());
    }

    private void validateStoryVolume(long novelId, Long id, StoryVolumeRequest request) {
        if (request.chapterEnd() != null && request.chapterEnd() < request.chapterStart())
            throw new WritingConflictException("卷末章节不能早于卷首章节");
        if (request.analyzedThroughChapterNo() != null
                && (request.analyzedThroughChapterNo() < request.chapterStart()
                || request.chapterEnd() != null && request.analyzedThroughChapterNo() > request.chapterEnd()))
            throw new WritingConflictException("已分析章节必须位于当前分卷范围内");
        boolean duplicate = id == null
                ? storyVolumeRepository.existsByNovelIdAndVolumeNo(novelId, request.volumeNo())
                : storyVolumeRepository.existsByNovelIdAndVolumeNoAndIdNot(novelId, request.volumeNo(), id);
        if (duplicate) throw new WritingConflictException("第" + request.volumeNo() + "卷已经存在");
        // 卷本身只表达一个大事件，不再依赖章节范围划分。章节归属由卷下的“部分”决定。
    }

    private void validateStoryPart(StoryVolumeEntity volume, Long id, StoryPartRequest request) {
        if (request.chapterEnd() != null && request.chapterEnd() < request.chapterStart())
            throw new WritingConflictException("部分的结束章节不能早于起始章节");
        boolean duplicate = id == null
                ? storyPartRepository.existsByVolumeIdAndPartNo(volume.getId(), request.partNo())
                : storyPartRepository.existsByVolumeIdAndPartNoAndIdNot(volume.getId(), request.partNo(), id);
        if (duplicate) throw new WritingConflictException("当前卷的第" + request.partNo() + "部分已经存在");
        int requestedEnd = request.chapterEnd() == null ? Integer.MAX_VALUE : request.chapterEnd();
        boolean overlaps = storyPartRepository.findByVolumeIdOrderByPartNoAsc(volume.getId()).stream()
                .filter(item -> !Objects.equals(item.getId(), id))
                .anyMatch(item -> request.chapterStart() <= (item.getChapterEnd() == null ? Integer.MAX_VALUE : item.getChapterEnd())
                        && item.getChapterStart() <= requestedEnd);
        if (overlaps) throw new WritingConflictException("章节范围与当前卷的其他部分重叠");
    }

    private void requireNovel(long novelId) {
        if (!novelRepository.existsById(novelId)) throw new WritingNotFoundException("小说不存在：" + novelId);
    }

    private StoryCharacterEntity requireCharacter(long id) {
        return characterRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("人物不存在：" + id));
    }

    private StoryOrganizationEntity requireOrganization(long id) {
        return organizationRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("组织不存在：" + id));
    }

    private WorldSettingEntity requireWorldSetting(long id) {
        return worldSettingRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("世界观设定不存在：" + id));
    }

    private StoryVolumeEntity requireStoryVolume(long id) {
        return storyVolumeRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("分卷不存在：" + id));
    }

    private StoryPartEntity requireStoryPart(long id) {
        return storyPartRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("部分不存在：" + id));
    }

    private void checkVersion(Long current, Long requested) {
        if (requested != null && !Objects.equals(current, requested))
            throw new WritingConflictException("故事资料已被修改，请刷新后重试");
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private CharacterResponse toResponse(StoryCharacterEntity e) {
        return new CharacterResponse(e.getId(), e.getNovelId(), e.getName(), e.getAliases(), e.getRole(), e.getAffiliations(),
                e.getDescription(), e.getPersonality(), e.getGoal(), e.getCurrentState(), e.getRelationships(),
                characterHistoryService.identities(e.getId()), e.getVersion());
    }

    private OrganizationResponse toResponse(StoryOrganizationEntity e) {
        return new OrganizationResponse(e.getId(), e.getNovelId(), e.getName(), e.getAliases(), e.getType(),
                e.getDescription(), e.getGoal(), e.getStructure(), e.getRelationships(), e.getVersion());
    }

    private WorldSettingResponse toResponse(WorldSettingEntity e) {
        return new WorldSettingResponse(e.getId(), e.getNovelId(), e.getCategory(), e.getTitle(), e.getContent(), e.getVersion());
    }

    private StoryVolumeResponse toResponse(StoryVolumeEntity e) {
        boolean stale = e.getAnalyzedThroughChapterNo() != null
                && !Objects.equals(e.getSourceContentVersionSum(), contentVersionSum(e.getNovelId(),
                e.getChapterStart(), e.getAnalyzedThroughChapterNo()));
        return new StoryVolumeResponse(e.getId(), e.getNovelId(), e.getVolumeNo(), e.getTitle(),
                e.getChapterStart(), e.getChapterEnd(), e.getObjective(), e.getRetrospective(),
                StoryVolumeContextService.readableChapterFacts(e.getChapterFactsJson()), e.getFuturePlan(),
                e.getKeyTurningPoints(), e.getClimax(), e.getEndingHook(), e.getForeshadows(), e.getLockedBeats(),
                e.getAnalyzedThroughChapterNo(), e.getSourceContentVersionSum(), e.getStatus(), stale,
                e.getVersion(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private StoryPartResponse toResponse(StoryPartEntity e) {
        return new StoryPartResponse(e.getId(), e.getNovelId(), e.getVolumeId(), e.getPartNo(), e.getTitle(),
                e.getChapterStart(), e.getChapterEnd(), e.getObjective(), e.getPlan(), e.getRetrospective(),
                e.getStatus(), e.getVersion(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private long contentVersionSum(long novelId, int chapterStart, Integer analyzedThroughChapterNo) {
        if (analyzedThroughChapterNo == null) return 0L;
        return chapterRepository.findByNovelIdOrderByChapterNoAsc(novelId).stream()
                .filter(chapter -> chapter.getChapterNo() >= chapterStart
                        && chapter.getChapterNo() <= analyzedThroughChapterNo)
                .mapToLong(chapter -> chapter.getContentVersion() == null ? 0L : chapter.getContentVersion())
                .sum();
    }
}
