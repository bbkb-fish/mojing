package com.mojing.novel.writing;

import com.mojing.novel.writing.AgentDtos.ChapterMemoryContent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoryVolumeContextServiceTest {
    private final StoryVolumeRepository volumes = mock(StoryVolumeRepository.class);
    private final StoryPartRepository parts = mock(StoryPartRepository.class);
    private final ChapterRepository chapters = mock(ChapterRepository.class);
    private final StoryVolumeContextService service = new StoryVolumeContextService(volumes, parts, chapters);

    @Test
    void confirmedVolumeIsUsedUntilAnalyzedChapterChanges() {
        ChapterEntity selected = mock(ChapterEntity.class);
        when(selected.getNovelId()).thenReturn(7L);
        when(selected.getChapterNo()).thenReturn(12);
        when(selected.getContentVersion()).thenReturn(3L);
        when(chapters.findById(12L)).thenReturn(Optional.of(selected));
        when(chapters.findByNovelIdOrderByChapterNoAsc(7L)).thenReturn(List.of(selected));

        StoryVolumeEntity volume = mock(StoryVolumeEntity.class);
        when(volume.getVolumeNo()).thenReturn(1);
        when(volume.getTitle()).thenReturn("雾港来信");
        when(volume.getChapterStart()).thenReturn(1);
        when(volume.getChapterEnd()).thenReturn(30);
        when(volume.getAnalyzedThroughChapterNo()).thenReturn(18);
        when(volume.getSourceContentVersionSum()).thenReturn(3L);
        when(volume.getStatus()).thenReturn("CONFIRMED");
        when(volume.getObjective()).thenReturn("找到失踪案的第一层真相");
        when(volumes.findByNovelIdOrderByVolumeNoAsc(7L)).thenReturn(List.of(volume));

        StoryPartEntity part = mock(StoryPartEntity.class);
        when(part.getPartNo()).thenReturn(2);
        when(part.getTitle()).thenReturn("潜入雾港");
        when(part.getChapterStart()).thenReturn(10);
        when(part.getChapterEnd()).thenReturn(16);
        when(part.getObjective()).thenReturn("查清失踪者去向");
        when(part.getPlan()).thenReturn("从码头线索推进到仓库对峙");
        when(part.getStatus()).thenReturn("CONFIRMED");
        when(parts.findByVolumeIdOrderByPartNoAsc(volume.getId())).thenReturn(List.of(part));

        assertThat(service.currentConfirmedVolume(7L, 12L))
                .contains("第1卷《雾港来信》", "找到失踪案的第一层真相", "第2部分《潜入雾港》",
                        "当前部分推进计划", "从码头线索推进到仓库对峙");

        when(selected.getContentVersion()).thenReturn(4L);
        assertThat(service.currentConfirmedVolume(7L, 12L)).isEmpty();
    }

    @Test
    void confirmedChapterMemoryIsAddedToItsVolumeAndReplacesTheSameChapter() {
        ChapterEntity chapter = mock(ChapterEntity.class);
        when(chapter.getId()).thenReturn(12L);
        when(chapter.getChapterNo()).thenReturn(12);
        when(chapter.getTitle()).thenReturn("钟楼来客");
        when(chapters.findById(12L)).thenReturn(Optional.of(chapter));

        ChapterMemoryEntity memory = mock(ChapterMemoryEntity.class);
        when(memory.getNovelId()).thenReturn(7L);
        when(memory.getChapterId()).thenReturn(12L);
        when(memory.getSourceChapterVersion()).thenReturn(3L);

        StoryVolumeEntity volume = new StoryVolumeEntity();
        volume.setNovelId(7L);
        volume.setVolumeNo(1);
        volume.setTitle("雾港来信");
        volume.setChapterStart(1);
        volume.setChapterEnd(30);
        when(volumes.findByNovelIdOrderByVolumeNoAsc(7L)).thenReturn(List.of(volume));

        ChapterMemoryContent first = memoryContent("主角第一次进入钟楼", "找到失踪者留下的暗号", List.of("守钟人是谁"));
        service.syncConfirmedChapterMemory(memory, first);
        assertThat(StoryVolumeContextService.readableChapterFacts(volume.getChapterFactsJson()))
                .contains("第12章《钟楼来客》", "主角第一次进入钟楼", "找到失踪者留下的暗号", "守钟人是谁");

        ChapterMemoryContent replacement = memoryContent("主角在钟楼发现密道", "确认暗号指向旧港", List.of("密道通往何处"));
        service.syncConfirmedChapterMemory(memory, replacement);
        assertThat(StoryVolumeContextService.readableChapterFacts(volume.getChapterFactsJson()))
                .contains("主角在钟楼发现密道", "确认暗号指向旧港", "密道通往何处")
                .doesNotContain("主角第一次进入钟楼", "守钟人是谁");
        verify(volumes, times(2)).saveAndFlush(volume);
    }

    private ChapterMemoryContent memoryContent(String summary, String plotProgress, List<String> unresolvedQuestions) {
        return new ChapterMemoryContent("test", summary, null, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), unresolvedQuestions, plotProgress, "MEDIUM");
    }
}
