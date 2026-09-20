package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_director_message")
class ChapterDirectorMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "session_id", nullable = false) private Long sessionId;
    @Column(name = "run_id") private Long runId;
    @Column(nullable = false, length = 20) private String role;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "plan_json", columnDefinition = "LONGTEXT") private String planJson;
    @Column(name = "change_summary_json", columnDefinition = "TEXT") private String changeSummaryJson;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    Long getId() { return id; }
    Long getSessionId() { return sessionId; }
    void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    Long getRunId() { return runId; }
    void setRunId(Long runId) { this.runId = runId; }
    String getRole() { return role; }
    void setRole(String role) { this.role = role; }
    String getContent() { return content; }
    void setContent(String content) { this.content = content; }
    String getPlanJson() { return planJson; }
    void setPlanJson(String planJson) { this.planJson = planJson; }
    String getChangeSummaryJson() { return changeSummaryJson; }
    void setChangeSummaryJson(String changeSummaryJson) { this.changeSummaryJson = changeSummaryJson; }
    LocalDateTime getCreatedAt() { return createdAt; }
}
