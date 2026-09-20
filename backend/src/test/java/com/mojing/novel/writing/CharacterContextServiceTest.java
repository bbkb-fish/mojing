package com.mojing.novel.writing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CharacterContextServiceTest {
    private final StoryCharacterRepository repository = mock(StoryCharacterRepository.class);
    private final CharacterIdentityRepository identities = mock(CharacterIdentityRepository.class);
    private final CharacterContextService service = new CharacterContextService(
            repository, identities, new CharacterNameResolver(repository));

    @Test
    void selectsOnlyExplicitlyMentionedOtherCharactersAndIncludesAffiliations() {
        StoryCharacterEntity current = character(1L, "哈基米", "主角", "议会");
        StoryCharacterEntity mentioned = character(2L, "阿璃", "盟友", "玄天剑宗");
        mentioned.setGoal("寻找失踪的师父");
        StoryCharacterEntity unrelated = character(3L, "周衡", "反派", "黑潮会");
        unrelated.setGoal("夺取议会控制权");
        List<StoryCharacterEntity> characters = List.of(current, mentioned, unrelated);
        when(repository.findByNovelIdOrderByIdAsc(7L)).thenReturn(characters);
        when(repository.findByNovelIdAndStatusOrderByIdAsc(7L, "ACTIVE")).thenReturn(characters);

        CharacterContextService.Selection selection = service.selectRelated(
                7L, "哈基米", "哈基米请阿璃调查河边留下的剑痕");

        assertThat(selection.characterNames()).containsExactly("阿璃");
        assertThat(selection.promptContext()).contains("所属组织：玄天剑宗", "寻找失踪的师父")
                .doesNotContain("夺取议会控制权");
    }

    @Test
    void relatedSelectionDoesNotFallbackToMainCharacterForPronouns() {
        List<StoryCharacterEntity> characters = List.of(
                character(1L, "哈基米", "主角", "议会"),
                character(2L, "阿璃", "盟友", "玄天剑宗"));
        when(repository.findByNovelIdOrderByIdAsc(7L)).thenReturn(characters);
        when(repository.findByNovelIdAndStatusOrderByIdAsc(7L, "ACTIVE")).thenReturn(characters);

        CharacterContextService.Selection selection = service.selectRelated(7L, "哈基米", "她对此感到不安");

        assertThat(selection.characterNames()).isEmpty();
        assertThat(selection.promptContext()).isEmpty();
    }

    private StoryCharacterEntity character(long id, String name, String role, String affiliations) {
        StoryCharacterEntity entity = new StoryCharacterEntity();
        entity.setId(id);
        entity.setNovelId(7L);
        entity.setName(name);
        entity.setRole(role);
        entity.setAffiliations(affiliations);
        return entity;
    }
}
