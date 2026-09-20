package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_bible_chat_session")
class StoryBibleChatSessionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "novel_id", nullable = false, unique = true) private Long novelId;
    @Version @Column(nullable = false) private Long version = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; }
    void setNovelId(Long novelId) { this.novelId = novelId; }
}
