package com.mojing.novel.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.qdrant.QdrantMemoryService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.mojing.novel.writing.AgentDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StoryBibleChatServiceTest {
    private final NovelRepository novels = mock(NovelRepository.class);
    private final StoryCharacterRepository characters = mock(StoryCharacterRepository.class);
    private final StoryOrganizationRepository organizations = mock(StoryOrganizationRepository.class);
    private final WorldSettingRepository settings = mock(WorldSettingRepository.class);
    private final StoryVolumeRepository volumes = mock(StoryVolumeRepository.class);
    private final NovelProjectProfileRepository projectProfiles = mock(NovelProjectProfileRepository.class);
    private final StoryBibleChatSessionRepository sessions = mock(StoryBibleChatSessionRepository.class);
    private final StoryBibleChatMessageRepository messages = mock(StoryBibleChatMessageRepository.class);
    private final StoryBibleMessageReferenceRepository references = mock(StoryBibleMessageReferenceRepository.class);
    private final StoryBibleService bible = mock(StoryBibleService.class);
    private final NovelAgentService agent = mock(NovelAgentService.class);
    private final ObjectMapper json = new ObjectMapper();
    private final QdrantMemoryService qdrant = mock(QdrantMemoryService.class);
    private final StoryBibleChatService service = new StoryBibleChatService(novels, characters, organizations,
            settings, volumes, projectProfiles, sessions, messages, references, bible, agent, json, qdrant);

    @Test
    void applyingCreateSuggestionWritesCharacterOnlyAfterExplicitApply() throws Exception {
        StoryBibleCharacterSuggestion suggestion = new StoryBibleCharacterSuggestion("CREATE", null, "闻烬", "闻先生",
                "本卷主要反派", "雾港议会", "秩序维护者", "克制", "控制灾难", "尚未登场",
                "认可主角能力但否定其选择", "适合作为价值观对手");
        StoryBibleChatSessionEntity session = mock(StoryBibleChatSessionEntity.class);
        when(session.getId()).thenReturn(3L);
        when(session.getNovelId()).thenReturn(7L);
        StoryBibleChatMessageEntity message = mock(StoryBibleChatMessageEntity.class);
        when(message.getId()).thenReturn(11L);
        when(message.getSessionId()).thenReturn(3L);
        when(message.getRole()).thenReturn("ASSISTANT");
        when(message.getContent()).thenReturn("我准备了一名反派");
        when(message.getSuggestionsJson()).thenReturn(json.writeValueAsString(List.of(suggestion)));
        when(message.getAppliedIndexesJson()).thenReturn("[]");
        when(messages.findById(11L)).thenReturn(Optional.of(message));
        when(messages.findBySessionIdOrderByIdAsc(3L)).thenReturn(List.of(message));
        when(references.findByMessageIdIn(List.of(11L))).thenReturn(List.of());
        when(sessions.findById(3L)).thenReturn(Optional.of(session));
        when(characters.findFirstByNovelIdAndNameIgnoreCase(7L, "闻烬")).thenReturn(Optional.empty());
        CharacterResponse created = new CharacterResponse(21L, 7L, "闻烬", "闻先生", "本卷主要反派",
                "雾港议会", "秩序维护者", "克制", "控制灾难", "尚未登场",
                "认可主角能力但否定其选择", List.of(), 0L);
        when(bible.createCharacter(eq(7L), any(CharacterRequest.class))).thenReturn(created);

        StoryBibleApplySuggestionResponse response = service.apply(11L, 0);

        assertThat(((CharacterResponse) response.result()).name()).isEqualTo("闻烬");
        verify(bible).createCharacter(eq(7L), any(CharacterRequest.class));
        verify(message).setAppliedIndexesJson("[0]");
        verify(messages).saveAndFlush(message);
    }

    @Test
    void applyingCreateSuggestionCanCreateOrganizationAndRecordReference() throws Exception {
        StoryBibleSuggestion suggestion = suggestion("ORGANIZATION", "CREATE", null, "灰塔议会", null, null);
        StoryBibleChatMessageEntity message = prepareApply(suggestion);
        OrganizationResponse created = mock(OrganizationResponse.class);
        when(created.id()).thenReturn(31L);
        when(created.name()).thenReturn("灰塔议会");
        when(bible.createOrganization(eq(7L), any(OrganizationRequest.class))).thenReturn(created);

        StoryBibleApplySuggestionResponse response = service.apply(11L, 0);

        assertThat(response.type()).isEqualTo("ORGANIZATION");
        assertThat(((OrganizationResponse) response.result()).name()).isEqualTo("灰塔议会");
        verify(bible).createOrganization(eq(7L), argThat(request -> "灰塔议会".equals(request.name())));
        verify(references).saveAllAndFlush(argThat(items -> {
            for (StoryBibleMessageReferenceEntity item : items) {
                if ("ORGANIZATION".equals(item.getEntityType()) && Long.valueOf(31L).equals(item.getEntityId())
                        && "APPLIED".equals(item.getReferenceType())) return true;
            }
            return false;
        }));
        verify(message).setAppliedIndexesJson("[0]");
    }

    @Test
    void applyingCreateSuggestionCanCreateWorldSetting() throws Exception {
        StoryBibleSuggestion suggestion = suggestion("WORLD_SETTING", "CREATE", null, null, "禁术", "逆转记忆的代价是失去姓名");
        prepareApply(suggestion);
        WorldSettingResponse created = mock(WorldSettingResponse.class);
        when(created.id()).thenReturn(41L);
        when(bible.createWorldSetting(eq(7L), any(WorldSettingRequest.class))).thenReturn(created);

        StoryBibleApplySuggestionResponse response = service.apply(11L, 0);

        assertThat(response.type()).isEqualTo("WORLD_SETTING");
        verify(bible).createWorldSetting(eq(7L), argThat(request -> "禁术".equals(request.title())
                && "其他".equals(request.category()) && request.content().contains("失去姓名")));
    }

    @Test
    void applyingVolumeUpdatePreservesWrittenFactsAndLockedBeats() throws Exception {
        StoryBibleSuggestion suggestion = suggestion("VOLUME", "UPDATE", 51L, null, "风暴将至", null);
        prepareApply(suggestion);
        StoryVolumeEntity current = mock(StoryVolumeEntity.class);
        when(current.getId()).thenReturn(51L);
        when(current.getNovelId()).thenReturn(7L);
        when(current.getVolumeNo()).thenReturn(1);
        when(current.getTitle()).thenReturn("旧标题");
        when(current.getChapterStart()).thenReturn(1);
        when(current.getChapterEnd()).thenReturn(40);
        when(current.getRetrospective()).thenReturn("已经写完的剧情事实");
        when(current.getLockedBeats()).thenReturn("不可改动的转折");
        when(current.getAnalyzedThroughChapterNo()).thenReturn(12);
        when(current.getVersion()).thenReturn(3L);
        when(volumes.findById(51L)).thenReturn(Optional.of(current));
        StoryVolumeResponse updated = mock(StoryVolumeResponse.class);
        when(updated.id()).thenReturn(51L);
        when(bible.updateStoryVolume(eq(51L), any(StoryVolumeRequest.class))).thenReturn(updated);

        service.apply(11L, 0);

        verify(bible).updateStoryVolume(eq(51L), argThat(request ->
                "风暴将至".equals(request.title())
                        && "已经写完的剧情事实".equals(request.retrospective())
                        && "不可改动的转折".equals(request.lockedBeats())
                        && request.analyzedThroughChapterNo() == 12));
    }

    @Test
    void oldOriginalTurnIsRecalledBySharedCharacterAndOrganization() {
        NovelEntity novel = mock(NovelEntity.class);
        when(novel.getId()).thenReturn(7L);
        when(novel.getTitle()).thenReturn("雾港");
        when(novels.findById(7L)).thenReturn(Optional.of(novel));
        StoryBibleChatSessionEntity session = mock(StoryBibleChatSessionEntity.class);
        when(session.getId()).thenReturn(3L);
        when(session.getNovelId()).thenReturn(7L);
        when(sessions.findByNovelId(7L)).thenReturn(Optional.of(session));

        StoryCharacterEntity hero = mock(StoryCharacterEntity.class);
        when(hero.getId()).thenReturn(12L);
        when(hero.getName()).thenReturn("哈基米");
        when(characters.findByNovelIdAndStatusOrderByIdAsc(7L, "ACTIVE")).thenReturn(List.of(hero));
        StoryOrganizationEntity guild = mock(StoryOrganizationEntity.class);
        when(guild.getId()).thenReturn(4L);
        when(guild.getName()).thenReturn("黑潮商会");
        when(organizations.findByNovelIdOrderByIdAsc(7L)).thenReturn(List.of(guild));
        when(settings.findByNovelIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(volumes.findByNovelIdOrderByVolumeNoAsc(7L)).thenReturn(List.of());

        List<StoryBibleChatMessageEntity> history = IntStream.rangeClosed(1, 18)
                .mapToObj(index -> chatMessage((long) index, index % 2 == 1 ? "USER" : "ASSISTANT",
                        index == 1 ? "哈基米是否应该信任黑潮商会？"
                                : index == 2 ? "他可以合作，但要保留对商会账本的怀疑。" : "近期的其他讨论"))
                .toList();
        when(messages.findBySessionIdOrderByIdAsc(3L)).thenReturn(history);
        when(messages.saveAndFlush(any(StoryBibleChatMessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoryBibleMessageReferenceEntity characterRef = reference(1L, "CHARACTER", 12L);
        StoryBibleMessageReferenceEntity organizationRef = reference(1L, "ORGANIZATION", 4L);
        when(references.findByMessageIdIn(any())).thenReturn(List.of(characterRef, organizationRef));
        when(references.findByEntityTypeAndEntityIdIn("CHARACTER", List.of(12L))).thenReturn(List.of(characterRef));
        when(references.findByEntityTypeAndEntityIdIn("ORGANIZATION", List.of(4L))).thenReturn(List.of(organizationRef));
        when(qdrant.searchStoryBibleMessages(eq(7L), anyString(), eq(8))).thenReturn(List.of());
        when(agent.storyBibleChatTurn(eq(novel), anyString(), anyString(), anyString()))
                .thenReturn(new StoryBibleChatTurnOutput("可以沿用早期的不信任设定。", List.of()));

        service.chat(7L, new StoryBibleChatRequest("继续分析哈基米和黑潮商会的信任关系", null, null));

        org.mockito.ArgumentCaptor<String> historyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(agent).storyBibleChatTurn(eq(novel), anyString(), historyCaptor.capture(), anyString());
        assertThat(historyCaptor.getValue()).contains("[消息1]作者", "[消息2]资料库顾问", "商会账本的怀疑");
    }

    private StoryBibleChatMessageEntity chatMessage(Long id, String role, String content) {
        StoryBibleChatMessageEntity message = mock(StoryBibleChatMessageEntity.class);
        when(message.getId()).thenReturn(id);
        when(message.getRole()).thenReturn(role);
        when(message.getContent()).thenReturn(content);
        return message;
    }

    private StoryBibleMessageReferenceEntity reference(Long messageId, String type, Long entityId) {
        StoryBibleMessageReferenceEntity reference = new StoryBibleMessageReferenceEntity();
        reference.setMessageId(messageId);
        reference.setEntityType(type);
        reference.setEntityId(entityId);
        reference.setReferenceType("MENTION");
        return reference;
    }

    private StoryBibleChatMessageEntity prepareApply(StoryBibleSuggestion suggestion) throws Exception {
        StoryBibleChatSessionEntity session = mock(StoryBibleChatSessionEntity.class);
        when(session.getId()).thenReturn(3L);
        when(session.getNovelId()).thenReturn(7L);
        StoryBibleChatMessageEntity message = mock(StoryBibleChatMessageEntity.class);
        when(message.getId()).thenReturn(11L);
        when(message.getSessionId()).thenReturn(3L);
        when(message.getRole()).thenReturn("ASSISTANT");
        when(message.getSuggestionsJson()).thenReturn(json.writeValueAsString(List.of(suggestion)));
        when(message.getAppliedIndexesJson()).thenReturn("[]");
        when(messages.findById(11L)).thenReturn(Optional.of(message));
        when(messages.findBySessionIdOrderByIdAsc(3L)).thenReturn(List.of(message));
        when(sessions.findById(3L)).thenReturn(Optional.of(session));
        when(references.findByMessageIdIn(List.of(11L))).thenReturn(List.of());
        return message;
    }

    private StoryBibleSuggestion suggestion(String type, String action, Long targetId, String name,
                                             String title, String content) {
        return new StoryBibleSuggestion(type, action, targetId, name, null, type.equals("ORGANIZATION") ? "秘密议会" : null,
                null, "由顾问生成", null, "控制城市", null, null, "七席议员", 
                null, title, content,
                type.equals("VOLUME") ? 1 : null, type.equals("VOLUME") ? 1 : null,
                type.equals("VOLUME") ? 40 : null, "建立冲突", "推进主线", "身份揭晓", "王城决战",
                "敌人逃脱", "旧信物", "满足作者要求");
    }
}
