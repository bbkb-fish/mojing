package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface ChapterMemoryRepository extends JpaRepository<ChapterMemoryEntity, Long> {
    Optional<ChapterMemoryEntity> findFirstByChapterIdOrderByCreatedAtDesc(Long chapterId);
    Optional<ChapterMemoryEntity> findFirstByChapterIdAndStatusOrderByCreatedAtDesc(Long chapterId, String status);
    List<ChapterMemoryEntity> findByChapterIdAndStatus(Long chapterId, String status);
    List<ChapterMemoryEntity> findByNovelIdAndStatusOrderByChapterNoAsc(Long novelId, String status);
}
