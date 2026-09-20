package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface ChapterRevisionRepository extends JpaRepository<ChapterRevisionEntity, Long> {
    List<ChapterRevisionEntity> findByChapterIdOrderByCreatedAtDesc(Long chapterId);
}
