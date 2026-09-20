package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ChapterDirectorMessageRepository extends JpaRepository<ChapterDirectorMessageEntity, Long> {
    List<ChapterDirectorMessageEntity> findBySessionIdOrderByIdAsc(Long sessionId);
}
