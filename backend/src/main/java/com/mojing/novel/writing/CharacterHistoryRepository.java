package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CharacterHistoryRepository extends JpaRepository<CharacterHistoryEntity, Long> {
    List<CharacterHistoryEntity> findByCharacterIdAndStatusOrderByChapterNoDescIdDesc(Long characterId, String status);
    List<CharacterHistoryEntity> findByChapterIdAndStatus(Long chapterId, String status);
    List<CharacterHistoryEntity> findByCharacterId(Long characterId);
}
