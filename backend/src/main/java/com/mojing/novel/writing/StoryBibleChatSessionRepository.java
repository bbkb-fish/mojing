package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface StoryBibleChatSessionRepository extends JpaRepository<StoryBibleChatSessionEntity, Long> {
    Optional<StoryBibleChatSessionEntity> findByNovelId(Long novelId);
}
