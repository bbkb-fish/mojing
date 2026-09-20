package com.mojing.novel.writing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/** 根据轻量元数据为一次 AI 调用按需装载人物完整档案。 */
@Service
public class CharacterContextService {
    private static final int PROFILE_CONTEXT_LIMIT = 12_000;
    private static final Logger log = LoggerFactory.getLogger(CharacterContextService.class);
    private final StoryCharacterRepository characterRepository;
    private final CharacterIdentityRepository identityRepository;
    private final CharacterNameResolver characterNameResolver;

    public CharacterContextService(StoryCharacterRepository characterRepository,
                                   CharacterIdentityRepository identityRepository,
                                   CharacterNameResolver characterNameResolver) {
        this.characterRepository = characterRepository;
        this.identityRepository = identityRepository;
        this.characterNameResolver = characterNameResolver;
    }

    public Selection select(Long novelId, String... signals) {
        return selectInternal(novelId, null, null, true, signals);
    }

    public Selection selectAtChapter(Long novelId, Integer chapterNo, String... signals) {
        return selectInternal(novelId, chapterNo, null, true, signals);
    }

    /** 资料编辑时只选择文本中明确提到的其他人物，不因“他/她”等泛指词回退加载主角。 */
    public Selection selectRelated(Long novelId, String excludedCharacterName, String... signals) {
        return selectInternal(novelId, null, excludedCharacterName, false, signals);
    }

    private Selection selectInternal(Long novelId, Integer targetChapterNo, String excludedCharacterName, boolean allowGenericFallback,
                                     String... signals) {
        if (novelId == null) return Selection.empty();
        List<StoryCharacterEntity> characters = characterRepository.findByNovelIdOrderByIdAsc(novelId);
        List<CharacterMetadata> metadata = characters.stream()
                .map(character -> new CharacterMetadata(character.getId(), character.getName(), character.getRole(),
                        characterNameResolver.routingNames(character)))
                .toList();
        List<Long> selectedIds = new ArrayList<>(selectIds(novelId, metadata, allowGenericFallback, signals));
        List<String> usableSignals = signals == null ? List.of() : Arrays.stream(signals).filter(CharacterContextService::hasText).toList();
        for (CharacterIdentityEntity identity : identityRepository.findByNovelId(novelId)) {
            if (usableSignals.stream().noneMatch(signal -> signal.contains(identity.getIdentityName()))) continue;
            Long visibleId = targetChapterNo != null && targetChapterNo < identity.getRevealChapterNo()
                    && identity.getSourceCharacterId() != null
                    ? identity.getSourceCharacterId() : identity.getCanonicalCharacterId();
            if (!selectedIds.contains(visibleId)) selectedIds.add(visibleId);
        }
        List<StoryCharacterEntity> selected = characters.stream()
                .filter(character -> selectedIds.contains(character.getId()))
                .filter(character -> !hasText(excludedCharacterName)
                        || !character.getName().equals(excludedCharacterName.trim()))
                .toList();

        StringBuilder context = new StringBuilder();
        for (StoryCharacterEntity character : selected) {
            StringBuilder profile = new StringBuilder("- ").append(character.getName());
            if (hasText(character.getAliases())) profile.append("（别名：").append(character.getAliases()).append("）");
            if (hasText(character.getRole())) profile.append("（").append(character.getRole()).append("）");
            if (hasText(character.getAffiliations())) profile.append("；所属组织：").append(character.getAffiliations());
            append(profile, character.getDescription());
            append(profile, character.getPersonality());
            append(profile, character.getGoal());
            if (hasText(character.getCurrentState())) profile.append("；当前状态：").append(character.getCurrentState());
            if (hasText(character.getRelationships())) profile.append("；当前关系：").append(character.getRelationships());
            profile.append('\n');
            appendWithinLimit(context, profile.toString());
        }
        List<String> names = selected.stream().map(StoryCharacterEntity::getName).toList();
        log.info("人物档案按需装载 novelId={} metadataCount={} selectedCount={} selectedNames={} contextChars={}",
                novelId, metadata.size(), names.size(), names, context.length());
        return new Selection(context.toString().stripTrailing(), names);
    }

    private List<Long> selectIds(Long novelId, List<CharacterMetadata> metadata, boolean allowGenericFallback, String... signals) {
        List<String> usableSignals = signals == null ? List.of() : Arrays.stream(signals)
                .filter(CharacterContextService::hasText)
                .toList();
        List<Long> matched = new ArrayList<>();
        for (CharacterMetadata character : metadata) {
            for (String routingName : character.routingNames()) {
                if (usableSignals.stream().noneMatch(signal -> signal.contains(routingName))) continue;
                StoryCharacterEntity resolved = characterNameResolver.resolve(novelId, routingName);
                if (resolved != null && resolved.getId().equals(character.id()) && !matched.contains(character.id()))
                    matched.add(character.id());
            }
        }
        if (!matched.isEmpty()) return matched;

        if (!allowGenericFallback) return List.of();
        boolean hasCharacterReference = usableSignals.stream().anyMatch(this::containsCharacterReference);
        if (!hasCharacterReference) return List.of();
        return metadata.stream()
                .filter(character -> hasText(character.role()) && character.role().contains("主角"))
                .map(CharacterMetadata::id)
                .limit(2)
                .toList();
    }

    private boolean containsCharacterReference(String text) {
        return text.contains("主角") || text.contains("他") || text.contains("她") || text.contains("其人");
    }

    private void appendWithinLimit(StringBuilder target, String value) {
        if (!hasText(value) || target.length() >= PROFILE_CONTEXT_LIMIT) return;
        int remaining = PROFILE_CONTEXT_LIMIT - target.length();
        target.append(value, 0, Math.min(remaining, value.length()));
    }

    private void append(StringBuilder target, String value) {
        if (hasText(value)) target.append("；").append(value);
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }

    /** 元数据只参与路由，不会进入模型提示词。 */
    private record CharacterMetadata(Long id, String name, String role, List<String> routingNames) {}

    public record Selection(String promptContext, List<String> characterNames) {
        static Selection empty() { return new Selection("", List.of()); }
    }
}
