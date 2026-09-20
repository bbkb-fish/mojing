package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface NovelProjectChatMessageRepository extends JpaRepository<NovelProjectChatMessageEntity, Long> {
    List<NovelProjectChatMessageEntity> findBySessionIdOrderByIdAsc(Long sessionId);
}
