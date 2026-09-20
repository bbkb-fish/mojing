package com.mojing.novel.writing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganizationContextServiceTest {
    private final StoryOrganizationRepository repository = mock(StoryOrganizationRepository.class);
    private final OrganizationContextService service = new OrganizationContextService(repository);

    @Test
    void selectsOnlyOrganizationMentionedByNameOrAlias() {
        StoryOrganizationEntity swordSect = organization(1L, "玄天剑宗", "玄天宗，剑宗", "宗门");
        swordSect.setGoal("寻找失落剑谱");
        StoryOrganizationEntity chamber = organization(2L, "四海商会", "商会", "商业组织");
        chamber.setGoal("控制北境商路");
        when(repository.findByNovelIdOrderByIdAsc(7L)).thenReturn(List.of(swordSect, chamber));

        OrganizationContextService.Selection selection = service.select(7L, "哈基米拿着玄天宗令牌进入山门");

        assertThat(selection.organizationNames()).containsExactly("玄天剑宗");
        assertThat(selection.promptContext()).contains("寻找失落剑谱").doesNotContain("控制北境商路");
    }

    @Test
    void doesNotLoadAllOrganizationsWhenSignalDoesNotMatch() {
        when(repository.findByNovelIdOrderByIdAsc(7L)).thenReturn(List.of(
                organization(1L, "玄天剑宗", "玄天宗", "宗门"),
                organization(2L, "四海商会", "商会", "商业组织")));

        OrganizationContextService.Selection selection = service.select(7L, "哈基米独自在河边休息");

        assertThat(selection.organizationNames()).isEmpty();
        assertThat(selection.promptContext()).isEmpty();
    }

    private StoryOrganizationEntity organization(long id, String name, String aliases, String type) {
        StoryOrganizationEntity entity = new StoryOrganizationEntity();
        entity.setId(id);
        entity.setNovelId(7L);
        entity.setName(name);
        entity.setAliases(aliases);
        entity.setType(type);
        return entity;
    }
}
