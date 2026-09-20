package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ChapterDirectorSessionRepository extends JpaRepository<ChapterDirectorSessionEntity, Long> {
    Optional<ChapterDirectorSessionEntity> findByChapterId(Long chapterId);
}
