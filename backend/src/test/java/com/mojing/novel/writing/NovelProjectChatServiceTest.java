package com.mojing.novel.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.mojing.novel.writing.AgentDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class NovelProjectChatServiceTest {
    private final NovelRepository novels = mock(NovelRepository.class);
    private final NovelProjectChatSessionRepository sessions = mock(NovelProjectChatSessionRepository.class);
    private final NovelProjectChatMessageRepository messages = mock(NovelProjectChatMessageRepository.class);
    private final ChapterRepository chapters = mock(ChapterRepository.class);
    private final NovelProjectService projects = mock(NovelProjectService.class);
    private final NovelAgentService agent = mock(NovelAgentService.class);
    private final NovelProjectChatService service = new NovelProjectChatService(
            novels, sessions, messages, chapters, projects, agent, new ObjectMapper());

    @Test
    void backfillsOldConversationAndPersistsLongTermMemory() {
        NovelEntity novel = mock(NovelEntity.class);
        NovelProjectChatSessionEntity session = mock(NovelProjectChatSessionEntity.class);
        NovelProjectChatMessageEntity old = mock(NovelProjectChatMessageEntity.class);
        NovelProjectChatMessageEntity savedUser = mock(NovelProjectChatMessageEntity.class);
        when(novels.findById(7L)).thenReturn(Optional.of(novel));
        when(session.getId()).thenReturn(3L);
        when(session.getNovelId()).thenReturn(7L);
        when(session.getMemoryJson()).thenReturn(null);
        when(sessions.findByNovelId(7L)).thenReturn(Optional.of(session));
        when(old.getId()).thenReturn(2L);
        when(old.getRole()).thenReturn("USER");
        when(old.getContent()).thenReturn("公主因为被陷害而被囚禁，哈基米一直暗中照顾她");
        when(messages.findBySessionIdOrderByIdAsc(3L)).thenReturn(List.of(old), List.of());
        when(savedUser.getId()).thenReturn(9L);
        when(messages.saveAndFlush(any())).thenAnswer(invocation -> {
            NovelProjectChatMessageEntity value = invocation.getArgument(0);
            return "USER".equals(value.getRole()) ? savedUser : value;
        });
        when(projects.get(7L)).thenReturn(new NovelProjectResponse(7L, emptyProject(), 1L, null, null, LocalDateTime.now()));
        NovelProjectMemory memory = new NovelProjectMemory(List.of("公主被囚禁"), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(2L, 9L));
        when(agent.novelProjectChatTurn(any(), any(), anyString(), any(), anyString(), any(), anyString(), anyString(), anyBoolean()))
                .thenReturn(new NovelProjectChatTurnOutput("已记住", null, memory, null, null));

        service.chat(7L, new NovelProjectChatRequest("继续讨论", "DISCUSS", false, emptyProject()));

        ArgumentCaptor<String> backfill = ArgumentCaptor.forClass(String.class);
        verify(agent).novelProjectChatTurn(eq(novel), any(), anyString(), any(), backfill.capture(), eq(9L),
                eq("继续讨论"), eq("DISCUSS"), eq(false));
        assertThat(backfill.getValue()).contains("公主因为被陷害而被囚禁");
        verify(session).setMemoryJson(contains("公主被囚禁"));
        verify(session).setMemoryUpdatedThroughMessageId(9L);
    }

    @Test
    void refusesToApplyMessageFromAnotherNovel() {
        NovelProjectChatMessageEntity message = applicableMessage();
        NovelProjectChatSessionEntity session = mock(NovelProjectChatSessionEntity.class);
        when(session.getNovelId()).thenReturn(8L);
        when(messages.findById(11L)).thenReturn(Optional.of(message));
        when(sessions.findById(3L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.apply(7L, 11L))
                .isInstanceOf(WritingNotFoundException.class)
                .hasMessageContaining("不属于当前小说");
        verifyNoInteractions(projects);
    }

    @Test
    void refusesImportedSuggestionAfterWrittenChapterChanges() {
        NovelProjectChatMessageEntity message = applicableMessage();
        when(message.getSourceNovelVersion()).thenReturn(4L);
        when(message.getSourceProfileVersion()).thenReturn(2L);
        when(message.getSourceContentVersionSum()).thenReturn(2L);
        when(message.getAnalyzedThroughChapterNo()).thenReturn(5);
        NovelProjectChatSessionEntity session = mock(NovelProjectChatSessionEntity.class);
        when(session.getNovelId()).thenReturn(7L);
        when(messages.findById(11L)).thenReturn(Optional.of(message));
        when(sessions.findById(3L)).thenReturn(Optional.of(session));
        when(projects.get(7L)).thenReturn(new NovelProjectResponse(7L, emptyProject(), 4L, 2L, 5, LocalDateTime.now()));
        ChapterEntity chapter = mock(ChapterEntity.class);
        when(chapter.getContent()).thenReturn("已修改正文");
        when(chapter.getContentVersion()).thenReturn(3L);
        when(chapter.getChapterNo()).thenReturn(5);
        when(chapters.findByNovelIdOrderByChapterNoAsc(7L)).thenReturn(List.of(chapter));

        assertThatThrownBy(() -> service.apply(7L, 11L))
                .isInstanceOf(WritingConflictException.class)
                .hasMessageContaining("正文在方案生成后已经变化");
        verify(projects, never()).save(anyLong(), any());
    }

    private NovelProjectChatMessageEntity applicableMessage() {
        NovelProjectChatMessageEntity message = mock(NovelProjectChatMessageEntity.class);
        when(message.getRole()).thenReturn("ASSISTANT");
        when(message.getSuggestionJson()).thenReturn("{}");
        when(message.getApplied()).thenReturn(false);
        when(message.getSessionId()).thenReturn(3L);
        return message;
    }

    private NovelProjectContent emptyProject() {
        return new NovelProjectContent("", "", "", "", "", "", "", "", "", "",
                null, null, null, "", "");
    }
}
