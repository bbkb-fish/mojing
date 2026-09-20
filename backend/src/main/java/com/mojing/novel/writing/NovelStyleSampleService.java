package com.mojing.novel.writing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 为一次性文风分析抽取代表性正文，不把整本小说反复发送给模型。 */
@Service
public class NovelStyleSampleService {
    private static final int MAX_SAMPLE_CHAPTERS = 5;
    private static final int MAX_CHARS_PER_CHAPTER = 5_000;
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;

    public NovelStyleSampleService(NovelRepository novelRepository, ChapterRepository chapterRepository) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
    }

    @Transactional(readOnly = true)
    public List<String> sample(long novelId) {
        if (!novelRepository.existsById(novelId)) throw new WritingNotFoundException("小说不存在：" + novelId);
        List<ChapterEntity> chapters = chapterRepository.findByNovelIdOrderByChapterNoAsc(novelId).stream()
                .filter(chapter -> chapter.getContent() != null && !chapter.getContent().isBlank())
                .toList();
        if (chapters.isEmpty()) throw new WritingConflictException("当前小说还没有正文，无法提取文风");

        Set<Integer> indexes = evenlyDistributedIndexes(chapters.size());
        List<String> samples = new ArrayList<>();
        for (int index : indexes) {
            ChapterEntity chapter = chapters.get(index);
            samples.add("第" + chapter.getChapterNo() + "章《" + chapter.getTitle() + "》节选：\n"
                    + representativeExcerpt(chapter.getContent().trim()));
        }
        return samples;
    }

    private Set<Integer> evenlyDistributedIndexes(int size) {
        Set<Integer> indexes = new LinkedHashSet<>();
        int count = Math.min(MAX_SAMPLE_CHAPTERS, size);
        if (count == 1) { indexes.add(0); return indexes; }
        for (int i = 0; i < count; i++) {
            indexes.add((int) Math.round((double) i * (size - 1) / (count - 1)));
        }
        return indexes;
    }

    private String representativeExcerpt(String content) {
        if (content.length() <= MAX_CHARS_PER_CHAPTER) return content;
        int headLength = 2_000;
        int middleLength = 1_500;
        int tailLength = 1_500;
        int middleStart = Math.max(headLength, content.length() / 2 - middleLength / 2);
        return content.substring(0, headLength)
                + "\n……（中间正文省略）……\n"
                + content.substring(middleStart, middleStart + middleLength)
                + "\n……（中间正文省略）……\n"
                + content.substring(content.length() - tailLength);
    }
}
