package com.mojing.novel.writing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.completion.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.mojing.novel.writing.AgentDtos.*;

@Service
public class CharacterHistoryService {
    private static final Logger log = LoggerFactory.getLogger(CharacterHistoryService.class);
    private final CharacterHistoryRepository historyRepository;
    private final StoryCharacterRepository characterRepository;
    private final CharacterIdentityRepository identityRepository;
    private final CharacterNameResolver characterNameResolver;
    private final ObjectMapper objectMapper;

    public CharacterHistoryService(CharacterHistoryRepository historyRepository,
                                   StoryCharacterRepository characterRepository,
                                   CharacterIdentityRepository identityRepository,
                                   CharacterNameResolver characterNameResolver, ObjectMapper objectMapper) {
        this.historyRepository = historyRepository;
        this.characterRepository = characterRepository;
        this.identityRepository = identityRepository;
        this.characterNameResolver = characterNameResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void applyConfirmedMemory(ChapterMemoryEntity memory, ChapterMemoryContent content) {
        List<CharacterHistoryEntity> previous = historyRepository.findByChapterIdAndStatus(memory.getChapterId(), "CONFIRMED");
        previous.forEach(item -> item.setStatus("SUPERSEDED"));
        historyRepository.saveAll(previous);

        int createdCharacters = 0;
        int histories = 0;
        int appliedChanges = 0;
        for (MemoryCharacter item : content.characters()) {
            StoryCharacterEntity character = characterNameResolver.resolve(memory.getNovelId(), item.name());
            if (character == null && Boolean.TRUE.equals(item.firstAppearance())
                    && !characterNameResolver.matchesKnownName(memory.getNovelId(), item.name())) {
                character = new StoryCharacterEntity();
                character.setNovelId(memory.getNovelId());
                character.setName(item.name().trim());
                character.setRole(normalize(item.role()));
                character = characterRepository.saveAndFlush(character);
                createdCharacters++;
            }
            if (character == null) {
                log.warn("章节人物未匹配档案，跳过历史写入 memoryId={} chapterId={} name={}",
                        memory.getId(), memory.getChapterId(), item.name());
                continue;
            }
            CharacterHistoryEntity history = new CharacterHistoryEntity();
            history.setNovelId(memory.getNovelId()); history.setCharacterId(character.getId());
            history.setCharacterName(character.getName()); history.setChapterId(memory.getChapterId());
            history.setChapterNo(memory.getChapterNo()); history.setSourceChapterVersion(memory.getSourceChapterVersion());
            history.setActions(normalize(item.actions())); history.setStateChange(normalize(item.stateChange()));
            history.setNewKnowledge(normalize(item.newKnowledge())); history.setStatus("CONFIRMED");
            history.setForeshadowingsJson(write(item.foreshadowings()));
            history.setProfileChangesJson(write(item.profileChanges()));
            historyRepository.save(history);
            histories++;
            for (CharacterProfileChange change : item.profileChanges()) {
                if (!Boolean.TRUE.equals(change.applyToProfile())) continue;
                applyChange(character, change);
                appliedChanges++;
            }
            characterRepository.save(character);
        }
        for (IdentityReveal reveal : content.identityReveals()) {
            if (Boolean.TRUE.equals(reveal.applyMerge())) applyIdentityReveal(memory, reveal);
        }
        log.info("章节人物历史已应用 memoryId={} chapterId={} histories={} createdCharacters={} appliedProfileChanges={}",
                memory.getId(), memory.getChapterId(), histories, createdCharacters, appliedChanges);
    }

    private void applyIdentityReveal(ChapterMemoryEntity memory, IdentityReveal reveal) {
        StoryCharacterEntity canonical = characterNameResolver.resolve(memory.getNovelId(), reveal.realCharacterName());
        if (canonical == null) throw new WritingConflictException("真实人物档案不存在或名称有歧义：" + reveal.realCharacterName());
        StoryCharacterEntity source = characterRepository
                .findFirstByNovelIdAndNameIgnoreCase(memory.getNovelId(), reveal.identityName().trim()).orElse(null);
        CharacterIdentityEntity identity = identityRepository
                .findFirstByNovelIdAndIdentityNameIgnoreCase(memory.getNovelId(), reveal.identityName().trim())
                .orElseGet(CharacterIdentityEntity::new);
        identity.setNovelId(memory.getNovelId()); identity.setCanonicalCharacterId(canonical.getId());
        identity.setSourceCharacterId(source == null ? null : source.getId());
        identity.setIdentityName(reveal.identityName().trim()); identity.setIdentityType("DISGUISE");
        identity.setRevealChapterNo(memory.getChapterNo()); identity.setDescription(normalize(reveal.evidence()));
        if (source != null && !source.getId().equals(canonical.getId())) {
            List<CharacterHistoryEntity> sourceHistory = historyRepository.findByCharacterId(source.getId());
            identity.setFirstAppearanceChapterNo(sourceHistory.stream().map(CharacterHistoryEntity::getChapterNo)
                    .min(Integer::compareTo).orElse(null));
            sourceHistory.forEach(item -> item.setCharacterId(canonical.getId()));
            historyRepository.saveAll(sourceHistory);
            source.setStatus("MERGED"); source.setMergedIntoCharacterId(canonical.getId());
            characterRepository.save(source);
        }
        identityRepository.save(identity);
        log.info("人物身份已合并 novelId={} revealChapter={} identity={} canonical={} migratedSourceId={}",
                memory.getNovelId(), memory.getChapterNo(), reveal.identityName(), canonical.getName(),
                source == null ? null : source.getId());
    }

    @Transactional(readOnly = true)
    public List<CharacterIdentityResponse> identities(long characterId) {
        return identityRepository.findByCanonicalCharacterIdOrderByRevealChapterNoAsc(characterId).stream()
                .map(item -> new CharacterIdentityResponse(item.getId(), item.getIdentityName(), item.getIdentityType(),
                        item.getFirstAppearanceChapterNo(), item.getRevealChapterNo(), item.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CharacterHistoryResponse> list(long characterId) {
        StoryCharacterEntity character = characterRepository.findById(characterId)
                .orElseThrow(() -> new WritingNotFoundException("人物不存在：" + characterId));
        return historyRepository.findByCharacterIdAndStatusOrderByChapterNoDescIdDesc(characterId, "CONFIRMED")
                .stream().map(item -> toResponse(item, character.getName())).toList();
    }

    private void applyChange(StoryCharacterEntity character, CharacterProfileChange change) {
        String current = switch (change.field()) {
            case "DESCRIPTION" -> character.getDescription();
            case "PERSONALITY" -> character.getPersonality();
            case "GOAL" -> character.getGoal();
            case "AFFILIATIONS" -> character.getAffiliations();
            case "CURRENT_STATE" -> character.getCurrentState();
            case "RELATIONSHIPS" -> character.getRelationships();
            default -> null;
        };
        String updated = updateText(current, change);
        switch (change.field()) {
            case "DESCRIPTION" -> character.setDescription(updated);
            case "PERSONALITY" -> character.setPersonality(updated);
            case "GOAL" -> character.setGoal(updated);
            case "AFFILIATIONS" -> character.setAffiliations(updated);
            case "CURRENT_STATE" -> character.setCurrentState(updated);
            case "RELATIONSHIPS" -> character.setRelationships(updated);
        }
    }

    private String updateText(String current, CharacterProfileChange change) {
        String base = current == null ? "" : current.trim();
        String next = change.newValue().trim();
        return switch (change.operation()) {
            case "ADD" -> containsPart(base, next) ? emptyToNull(base) : emptyToNull(base.isBlank() ? next : base + "；" + next);
            case "REMOVE" -> emptyToNull(base.replace(next, "").replaceAll("^[；,，\\s]+|[；,，\\s]+$", ""));
            case "REPLACE" -> {
                String old = change.oldValue() == null ? "" : change.oldValue().trim();
                yield emptyToNull(!old.isBlank() && base.contains(old) ? base.replace(old, next) : next);
            }
            default -> emptyToNull(base);
        };
    }

    private boolean containsPart(String current, String value) {
        return !value.isBlank() && (current.equals(value) || current.contains("；" + value) || current.startsWith(value + "；"));
    }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception error) { throw new AiProviderException("人物历史序列化失败", error); }
    }
    private <T> List<T> read(String value, TypeReference<List<T>> type) {
        if (value == null || value.isBlank()) return List.of();
        try { return objectMapper.readValue(value, type); }
        catch (Exception error) { return List.of(); }
    }
    private CharacterHistoryResponse toResponse(CharacterHistoryEntity item, String currentName) {
        return new CharacterHistoryResponse(item.getId(), item.getNovelId(), item.getCharacterId(), currentName,
                item.getChapterId(), item.getChapterNo(), item.getSourceChapterVersion(), item.getActions(),
                item.getStateChange(), item.getNewKnowledge(),
                read(item.getForeshadowingsJson(), new TypeReference<>() {}),
                read(item.getProfileChangesJson(), new TypeReference<>() {}), item.getStatus(), item.getCreatedAt());
    }
}
