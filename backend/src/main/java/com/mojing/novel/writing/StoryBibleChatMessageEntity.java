package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_bible_chat_message")
class StoryBibleChatMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "session_id", nullable = false) private Long sessionId;
    @Column(nullable = false, length = 20) private String role;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "suggestions_json", columnDefinition = "LONGTEXT") private String suggestionsJson;
    @Column(name = "applied_indexes_json", columnDefinition = "TEXT") private String appliedIndexesJson;
    @Column(name = "recalled_message_ids_json", columnDefinition = "TEXT") private String recalledMessageIdsJson;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    Long getId() { return id; }
    Long getSessionId() { return sessionId; }
    void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    String getRole() { return role; }
    void setRole(String role) { this.role = role; }
    String getContent() { return content; }
    void setContent(String content) { this.content = content; }
    String getSuggestionsJson() { return suggestionsJson; }
    void setSuggestionsJson(String suggestionsJson) { this.suggestionsJson = suggestionsJson; }
    String getAppliedIndexesJson() { return appliedIndexesJson; }
    void setAppliedIndexesJson(String appliedIndexesJson) { this.appliedIndexesJson = appliedIndexesJson; }
    String getRecalledMessageIdsJson() { return recalledMessageIdsJson; }
    void setRecalledMessageIdsJson(String recalledMessageIdsJson) { this.recalledMessageIdsJson = recalledMessageIdsJson; }
    LocalDateTime getCreatedAt() { return createdAt; }
}
