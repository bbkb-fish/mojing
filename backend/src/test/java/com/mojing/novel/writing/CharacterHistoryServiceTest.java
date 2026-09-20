package com.mojing.novel.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.mojing.novel.writing.AgentDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CharacterHistoryServiceTest {
    @Test
    void appendsHistoryAndAppliesOnlyAcceptedProfileChanges() {
        CharacterHistoryRepository histories = mock(CharacterHistoryRepository.class);
        StoryCharacterRepository characters = mock(StoryCharacterRepository.class);
        CharacterIdentityRepository identities = mock(CharacterIdentityRepository.class);
        CharacterHistoryService service = new CharacterHistoryService(
                histories, characters, identities, new CharacterNameResolver(characters), new ObjectMapper());
        StoryCharacterEntity character = new StoryCharacterEntity();
        character.setId(7L); character.setNovelId(2L); character.setName("哈基米"); character.setPersonality("严肃");
        when(characters.findByNovelIdAndStatusOrderByIdAsc(2L, "ACTIVE")).thenReturn(List.of(character));
        when(histories.findByChapterIdAndStatus(12L, "CONFIRMED")).thenReturn(List.of());

        CharacterProfileChange personality = new CharacterProfileChange("PERSONALITY", "ADD", "",
                "喜欢找乐子", "化身老人捉弄同伴", "MEDIUM", false);
        CharacterProfileChange relationship = new CharacterProfileChange("RELATIONSHIPS", "ADD", "",
                "对艾琳：由敌对转为友好", "双方共同脱险", "HIGH", true);
        MemoryCharacter memoryCharacter = new MemoryCharacter("哈基米", "主角", "与艾琳共同对敌",
                "关系缓和", "得知艾琳没有泄密", false, List.of("仍未说明为何认识蛇形标记"),
                List.of(personality, relationship));
        ChapterMemoryContent content = new ChapterMemoryContent("chapter-compression-v1", "两人共同脱险", "",
                List.of(), List.of(memoryCharacter), List.of(), List.of(), List.of(), List.of(), List.of(), "", "HIGH");
        ChapterMemoryEntity chapterMemory = mock(ChapterMemoryEntity.class);
        when(chapterMemory.getId()).thenReturn(20L); when(chapterMemory.getNovelId()).thenReturn(2L);
        when(chapterMemory.getChapterId()).thenReturn(12L); when(chapterMemory.getChapterNo()).thenReturn(7);
        when(chapterMemory.getSourceChapterVersion()).thenReturn(3L);

        service.applyConfirmedMemory(chapterMemory, content);

        assertThat(character.getPersonality()).isEqualTo("严肃");
        assertThat(character.getRelationships()).isEqualTo("对艾琳：由敌对转为友好");
        verify(histories).save(any(CharacterHistoryEntity.class));
        verify(characters).save(character);
    }
}
