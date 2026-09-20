package com.mojing.novel.writing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.writing.AgentDtos.ChapterMemoryContent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class StoryVolumeContextService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final StoryVolumeRepository volumeRepository;
    private final StoryPartRepository partRepository;
    private final ChapterRepository chapterRepository;

    public StoryVolumeContextService(StoryVolumeRepository volumeRepository, StoryPartRepository partRepository,
                                     ChapterRepository chapterRepository) {
        this.volumeRepository = volumeRepository;
        this.partRepository = partRepository;
        this.chapterRepository = chapterRepository;
    }

    @Transactional(readOnly = true)
    public String currentConfirmedVolume(Long novelId, Long chapterId) {
        if (novelId == null || chapterId == null) return "";
        ChapterEntity selected = chapterRepository.findById(chapterId).orElse(null);
        if (selected == null || !Objects.equals(selected.getNovelId(), novelId)) return "";
        List<ChapterEntity> chapters = chapterRepository.findByNovelIdOrderByChapterNoAsc(novelId);
        List<StoryVolumeEntity> confirmedVolumes = volumeRepository.findByNovelIdOrderByVolumeNoAsc(novelId).stream()
                .filter(volume -> "CONFIRMED".equals(volume.getStatus()))
                .toList();
        StoryVolumeEntity partMatched = confirmedVolumes.stream()
                .filter(volume -> partRepository.findByVolumeIdOrderByPartNoAsc(volume.getId()).stream()
                        .filter(part -> "CONFIRMED".equals(part.getStatus()))
                        .anyMatch(part -> contains(part, selected.getChapterNo())))
                .findFirst().orElse(null);
        if (partMatched != null && !stale(partMatched, chapters)) return format(partMatched, selected.getChapterNo());
        return confirmedVolumes.stream()
                .filter(volume -> selected.getChapterNo() >= volume.getChapterStart())
                .filter(volume -> volume.getChapterEnd() == null || selected.getChapterNo() <= volume.getChapterEnd())
                .filter(volume -> !stale(volume, chapters))
                .findFirst().map(volume -> format(volume, selected.getChapterNo())).orElse("");
    }

    @Transactional
    public void syncConfirmedChapterMemory(ChapterMemoryEntity memory, ChapterMemoryContent content) {
        ChapterEntity chapter = chapterRepository.findById(memory.getChapterId()).orElse(null);
        if (chapter == null) return;
        List<StoryVolumeEntity> volumes = volumeRepository.findByNovelIdOrderByVolumeNoAsc(memory.getNovelId());
        StoryVolumeEntity volume = volumes.stream()
                .filter(item -> partRepository.findByVolumeIdOrderByPartNoAsc(item.getId()).stream()
                        .anyMatch(part -> contains(part, chapter.getChapterNo())))
                .findFirst().orElse(null);
        if (volume == null) volume = volumes.stream()
                .filter(item -> chapter.getChapterNo() >= item.getChapterStart())
                .filter(item -> item.getChapterEnd() == null || chapter.getChapterNo() <= item.getChapterEnd())
                .findFirst().orElse(null);
        if (volume == null) return;
        List<ChapterFact> facts = new ArrayList<>(readChapterFacts(volume.getChapterFactsJson()));
        facts.removeIf(item -> Objects.equals(item.chapterId(), chapter.getId())
                || Objects.equals(item.chapterNo(), chapter.getChapterNo()));
        facts.add(new ChapterFact(chapter.getId(), chapter.getChapterNo(), chapter.getTitle(),
                memory.getSourceChapterVersion(), content.summary(), content.plotProgress(),
                content.unresolvedQuestions() == null ? List.of() : content.unresolvedQuestions()));
        facts.sort(Comparator.comparing(ChapterFact::chapterNo));
        try {
            volume.setChapterFactsJson(JSON.writeValueAsString(facts));
            volumeRepository.saveAndFlush(volume);
        } catch (Exception exception) {
            throw new WritingConflictException("章节记忆已确认，但同步分卷剧情事实失败，请重试");
        }
    }

    public static String readableChapterFacts(String json) {
        StringBuilder text = new StringBuilder();
        for (ChapterFact fact : readChapterFacts(json)) {
            text.append("【第").append(fact.chapterNo()).append("章《").append(fact.title()).append("》】\n")
                    .append(fact.summary());
            if (fact.plotProgress() != null && !fact.plotProgress().isBlank())
                text.append("\n主线推进：").append(fact.plotProgress());
            if (fact.unresolvedQuestions() != null && !fact.unresolvedQuestions().isEmpty())
                text.append("\n未解决：").append(String.join("；", fact.unresolvedQuestions()));
            text.append("\n\n");
        }
        return text.toString().trim();
    }

    private static List<ChapterFact> readChapterFacts(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return JSON.readValue(json, new TypeReference<List<ChapterFact>>() {}); }
        catch (Exception ignored) { return List.of(); }
    }

    private boolean stale(StoryVolumeEntity volume, List<ChapterEntity> chapters) {
        if (volume.getAnalyzedThroughChapterNo() == null) return false;
        long current = chapters.stream()
                .filter(chapter -> chapter.getChapterNo() >= volume.getChapterStart()
                        && chapter.getChapterNo() <= volume.getAnalyzedThroughChapterNo())
                .mapToLong(chapter -> chapter.getContentVersion() == null ? 0L : chapter.getContentVersion())
                .sum();
        return !Objects.equals(volume.getSourceContentVersionSum(), current);
    }

    private boolean contains(StoryPartEntity part, int chapterNo) {
        return chapterNo >= part.getChapterStart()
                && (part.getChapterEnd() == null || chapterNo <= part.getChapterEnd());
    }

    private String format(StoryVolumeEntity volume, int currentChapterNo) {
        StringBuilder text = new StringBuilder("【当前分卷主线：第")
                .append(volume.getVolumeNo()).append("卷《").append(volume.getTitle()).append("》】\n");
        append(text, "本卷目标", volume.getObjective());
        List<StoryPartEntity> parts = partRepository.findByVolumeIdOrderByPartNoAsc(volume.getId()).stream()
                .filter(part -> "CONFIRMED".equals(part.getStatus()))
                .toList();
        if (!parts.isEmpty()) {
            text.append("【卷内部分结构】\n");
            for (StoryPartEntity part : parts) {
                boolean current = currentChapterNo >= part.getChapterStart()
                        && (part.getChapterEnd() == null || currentChapterNo <= part.getChapterEnd());
                text.append(current ? "→ " : "- ").append("第").append(part.getPartNo()).append("部分《")
                        .append(part.getTitle()).append("》（第").append(part.getChapterStart()).append("～")
                        .append(part.getChapterEnd() == null ? "?" : part.getChapterEnd()).append("章）");
                if (part.getObjective() != null) text.append("：").append(part.getObjective());
                text.append('\n');
                if (current) {
                    append(text, "当前部分推进计划", part.getPlan());
                    append(text, "当前部分已写事实", part.getRetrospective());
                }
            }
        }
        append(text, "已写剧情事实", volume.getRetrospective());
        append(text, "章节压缩确认的剧情事实", readableChapterFacts(volume.getChapterFactsJson()));
        append(text, "剩余剧情安排", volume.getFuturePlan());
        append(text, "关键转折", volume.getKeyTurningPoints());
        append(text, "本卷高潮", volume.getClimax());
        append(text, "卷末钩子", volume.getEndingHook());
        append(text, "伏笔安排", volume.getForeshadows());
        append(text, "作者锁定节点", volume.getLockedBeats());
        return text.length() <= 10_000 ? text.toString() : text.substring(0, 10_000);
    }

    private void append(StringBuilder text, String label, String value) {
        if (value != null && !value.isBlank()) text.append('【').append(label).append("】").append(value).append('\n');
    }

    private record ChapterFact(Long chapterId, Integer chapterNo, String title, Long sourceContentVersion,
                               String summary, String plotProgress, List<String> unresolvedQuestions) {}
}
