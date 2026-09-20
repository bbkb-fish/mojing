package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "novel_project_chat_message")
class NovelProjectChatMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "session_id", nullable = false) private Long sessionId;
    @Column(nullable = false, length = 20) private String role;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String content;
    @Column(name = "suggestion_json", columnDefinition = "LONGTEXT") private String suggestionJson;
    @Column(name = "source_novel_version") private Long sourceNovelVersion;
    @Column(name = "source_profile_version") private Long sourceProfileVersion;
    @Column(name = "analyzed_through_chapter_no") private Integer analyzedThroughChapterNo;
    @Column(name = "source_content_version_sum") private Long sourceContentVersionSum;
    @Column(nullable = false) private Boolean applied = false;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    Long getId() { return id; }
    Long getSessionId() { return sessionId; } void setSessionId(Long value) { sessionId = value; }
    String getRole() { return role; } void setRole(String value) { role = value; }
    String getContent() { return content; } void setContent(String value) { content = value; }
    String getSuggestionJson() { return suggestionJson; } void setSuggestionJson(String value) { suggestionJson = value; }
    Long getSourceNovelVersion() { return sourceNovelVersion; } void setSourceNovelVersion(Long value) { sourceNovelVersion = value; }
    Long getSourceProfileVersion() { return sourceProfileVersion; } void setSourceProfileVersion(Long value) { sourceProfileVersion = value; }
    Integer getAnalyzedThroughChapterNo() { return analyzedThroughChapterNo; } void setAnalyzedThroughChapterNo(Integer value) { analyzedThroughChapterNo = value; }
    Long getSourceContentVersionSum() { return sourceContentVersionSum; } void setSourceContentVersionSum(Long value) { sourceContentVersionSum = value; }
    Boolean getApplied() { return applied; } void setApplied(Boolean value) { applied = value; }
    LocalDateTime getCreatedAt() { return createdAt; }
}
