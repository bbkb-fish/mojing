package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface StoryBibleChatMessageRepository extends JpaRepository<StoryBibleChatMessageEntity, Long> {
    List<StoryBibleChatMessageEntity> findBySessionIdOrderByIdAsc(Long sessionId);
}
