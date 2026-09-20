package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface StoryCharacterRepository extends JpaRepository<StoryCharacterEntity, Long> {
    List<StoryCharacterEntity> findByNovelIdOrderByIdAsc(Long novelId);
    List<StoryCharacterEntity> findByNovelIdAndStatusOrderByIdAsc(Long novelId, String status);
    java.util.Optional<StoryCharacterEntity> findFirstByNovelIdAndNameIgnoreCase(Long novelId, String name);
}
