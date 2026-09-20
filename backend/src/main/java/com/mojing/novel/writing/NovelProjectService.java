package com.mojing.novel.writing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.mojing.novel.writing.AgentDtos.*;

@Service
public class NovelProjectService {
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final NovelProjectProfileRepository profileRepository;

    public NovelProjectService(NovelRepository novelRepository, ChapterRepository chapterRepository,
                               NovelProjectProfileRepository profileRepository) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public NovelProjectResponse get(long novelId) {
        NovelEntity novel = requireNovel(novelId);
        NovelProjectProfileEntity profile = profileRepository.findByNovelId(novelId).orElse(null);
        return response(novel, profile);
    }

    @Transactional
    public NovelProjectResponse save(long novelId, NovelProjectSaveRequest request) {
        NovelEntity novel = requireNovel(novelId);
        if (!Objects.equals(novel.getVersion(), request.novelVersion()))
            throw new WritingConflictException("作品资料已在其他页面修改，请刷新立项工作台后重试");
        NovelProjectProfileEntity profile = profileRepository.findByNovelId(novelId).orElseGet(() -> {
            NovelProjectProfileEntity created = new NovelProjectProfileEntity();
            created.setNovelId(novelId);
            return created;
        });
        if (profile.getId() != null && !Objects.equals(profile.getVersion(), request.profileVersion()))
            throw new WritingConflictException("立项资料已在其他页面修改，请刷新后重试");
        NovelProjectContent content = request.content();
        profile.setInspiration(normalize(content.inspiration()));
        profile.setGenre(normalize(content.genre()));
        profile.setChannel(normalize(content.channel()));
        profile.setTargetAudience(normalize(content.targetAudience()));
        profile.setPlatform(normalize(content.platform()));
        profile.setCoreSellingPoint(normalize(content.coreSellingPoint()));
        profile.setProtagonistHook(normalize(content.protagonistHook()));
        profile.setGrowthRoute(normalize(content.growthRoute()));
        profile.setReaderExpectations(normalize(content.readerExpectations()));
        profile.setOpeningThreeChapters(normalize(content.openingThreeChapters()));
        profile.setExpectedWords(content.expectedWords());
        profile.setExpectedVolumes(content.expectedVolumes());
        profile.setChapterWordTarget(content.chapterWordTarget());
        novel.setDescription(normalize(content.description()));
        novel.setOutline(normalize(content.outline()));
        profile = profileRepository.saveAndFlush(profile);
        novel = novelRepository.saveAndFlush(novel);
        return response(novel, profile);
    }

    private NovelProjectResponse response(NovelEntity novel, NovelProjectProfileEntity profile) {
        NovelProjectContent content = new NovelProjectContent(
                profile == null ? null : profile.getInspiration(), profile == null ? null : profile.getGenre(),
                profile == null ? null : profile.getChannel(), profile == null ? null : profile.getTargetAudience(),
                profile == null ? null : profile.getPlatform(), profile == null ? null : profile.getCoreSellingPoint(),
                profile == null ? null : profile.getProtagonistHook(), profile == null ? null : profile.getGrowthRoute(),
                profile == null ? null : profile.getReaderExpectations(), profile == null ? null : profile.getOpeningThreeChapters(),
                profile == null ? null : profile.getExpectedWords(), profile == null ? null : profile.getExpectedVolumes(),
                profile == null ? null : profile.getChapterWordTarget(), novel.getDescription(), novel.getOutline());
        Integer writtenThrough = chapterRepository.findByNovelIdOrderByChapterNoAsc(novel.getId()).stream()
                .filter(item -> item.getContent() != null && !item.getContent().isBlank())
                .map(ChapterEntity::getChapterNo).reduce((first, second) -> second).orElse(null);
        return new NovelProjectResponse(novel.getId(), content, novel.getVersion(),
                profile == null ? null : profile.getVersion(), writtenThrough,
                profile == null ? novel.getUpdatedAt() : profile.getUpdatedAt());
    }

    private NovelEntity requireNovel(long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new WritingNotFoundException("小说不存在：" + novelId));
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
