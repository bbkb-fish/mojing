package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "consistency_report")
class ConsistencyReportEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "novel_id", nullable = false) private Long novelId;
    @Column(name = "chapter_id", nullable = false) private Long chapterId;
    @Column(name = "agent_run_id") private Long agentRunId;
    @Column(name = "source_content_version", nullable = false) private Long sourceContentVersion;
    @Column(nullable = false) private Integer score;
    @Column(nullable = false, length = 2000) private String summary;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "provider_mode", length = 20) private String providerMode;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; } void setNovelId(Long v) { novelId = v; }
    Long getChapterId() { return chapterId; } void setChapterId(Long v) { chapterId = v; }
    Long getAgentRunId() { return agentRunId; } void setAgentRunId(Long v) { agentRunId = v; }
    Long getSourceContentVersion() { return sourceContentVersion; } void setSourceContentVersion(Long v) { sourceContentVersion = v; }
    Integer getScore() { return score; } void setScore(Integer v) { score = v; }
    String getSummary() { return summary; } void setSummary(String v) { summary = v; }
    String getStatus() { return status; } void setStatus(String v) { status = v; }
    String getProviderMode() { return providerMode; } void setProviderMode(String v) { providerMode = v; }
    LocalDateTime getCreatedAt() { return createdAt; }
}
