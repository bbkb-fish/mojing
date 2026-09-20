package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface NovelProjectChatSessionRepository extends JpaRepository<NovelProjectChatSessionEntity, Long> {
    Optional<NovelProjectChatSessionEntity> findByNovelId(Long novelId);
}
