package com.mojing.novel.writing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CharacterNameResolverTest {
    @Test
    void resolvesExplicitAliasAndUniqueWesternShortName() {
        StoryCharacterRepository repository = mock(StoryCharacterRepository.class);
        StoryCharacterEntity karl = character(1L, "卡尔·伯格", "卡尔局长");
        when(repository.findByNovelIdAndStatusOrderByIdAsc(2L, "ACTIVE")).thenReturn(List.of(karl));
        CharacterNameResolver resolver = new CharacterNameResolver(repository);

        assertThat(resolver.resolve(2L, "卡尔局长").getId()).isEqualTo(1L);
        assertThat(resolver.resolve(2L, "卡尔").getId()).isEqualTo(1L);
    }

    @Test
    void refusesAmbiguousDerivedShortName() {
        StoryCharacterRepository repository = mock(StoryCharacterRepository.class);
        when(repository.findByNovelIdAndStatusOrderByIdAsc(2L, "ACTIVE")).thenReturn(List.of(
                character(1L, "卡尔·伯格", null), character(2L, "卡尔·史密斯", null)));
        CharacterNameResolver resolver = new CharacterNameResolver(repository);

        assertThat(resolver.resolve(2L, "卡尔")).isNull();
        assertThat(resolver.matchesKnownName(2L, "卡尔")).isTrue();
    }

    private StoryCharacterEntity character(long id, String name, String aliases) {
        StoryCharacterEntity character = new StoryCharacterEntity();
        character.setId(id); character.setNovelId(2L); character.setName(name); character.setAliases(aliases);
        return character;
    }
}
