package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CharacterIdentityRepository extends JpaRepository<CharacterIdentityEntity, Long> {
    List<CharacterIdentityEntity> findByCanonicalCharacterIdOrderByRevealChapterNoAsc(Long characterId);
    List<CharacterIdentityEntity> findByNovelId(Long novelId);
    Optional<CharacterIdentityEntity> findFirstByNovelIdAndIdentityNameIgnoreCase(Long novelId, String identityName);
}
