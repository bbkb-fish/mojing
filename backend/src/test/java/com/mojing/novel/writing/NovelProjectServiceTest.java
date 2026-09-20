package com.mojing.novel.writing;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static com.mojing.novel.writing.AgentDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NovelProjectServiceTest {
    private final NovelRepository novels = mock(NovelRepository.class);
    private final ChapterRepository chapters = mock(ChapterRepository.class);
    private final NovelProjectProfileRepository profiles = mock(NovelProjectProfileRepository.class);
    private final NovelProjectService service = new NovelProjectService(novels, chapters, profiles);

    @Test
    void savesProjectProfileAndCanonicalDescriptionOnlyAfterConfirmation() {
        NovelEntity novel = mock(NovelEntity.class);
        when(novel.getId()).thenReturn(7L);
        when(novel.getVersion()).thenReturn(2L);
        when(novel.getDescription()).thenReturn("新简介");
        when(novel.getOutline()).thenReturn("新大纲");
        when(novels.findById(7L)).thenReturn(Optional.of(novel));
        when(profiles.findByNovelId(7L)).thenReturn(Optional.empty());
        when(profiles.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(novels.saveAndFlush(novel)).thenReturn(novel);
        when(chapters.findByNovelIdOrderByChapterNoAsc(7L)).thenReturn(List.of());
        NovelProjectContent content = new NovelProjectContent("一句灵感", "都市异能", "男频", "18-35岁读者",
                "起点", "死亡倒计时与救人抉择", "看见倒计时，但救人会消耗自己的寿命", "从救一人到改变城市规则",
                "救人反转、能力升级、幕后真相", "前三章完成能力展示与第一次代价", 1_200_000, 6, 2500,
                "新简介", "新大纲");

        NovelProjectResponse response = service.save(7L, new NovelProjectSaveRequest(content, 2L, null));

        verify(novel).setDescription("新简介");
        verify(novel).setOutline("新大纲");
        ArgumentCaptor<NovelProjectProfileEntity> captor = ArgumentCaptor.forClass(NovelProjectProfileEntity.class);
        verify(profiles).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNovelId()).isEqualTo(7L);
        assertThat(captor.getValue().getCoreSellingPoint()).contains("死亡倒计时");
        assertThat(response.content().description()).isEqualTo("新简介");
    }
}
