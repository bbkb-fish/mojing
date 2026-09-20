package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "novel_project_chat_session")
class NovelProjectChatSessionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "novel_id", nullable = false, unique = true) private Long novelId;
    @Column(name = "memory_json", columnDefinition = "LONGTEXT") private String memoryJson;
    @Column(name = "memory_updated_through_message_id") private Long memoryUpdatedThroughMessageId;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    Long getId() { return id; }
    Long getNovelId() { return novelId; } void setNovelId(Long value) { novelId = value; }
    String getMemoryJson() { return memoryJson; } void setMemoryJson(String value) { memoryJson = value; }
    Long getMemoryUpdatedThroughMessageId() { return memoryUpdatedThroughMessageId; }
    void setMemoryUpdatedThroughMessageId(Long value) { memoryUpdatedThroughMessageId = value; }
}
