package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_revision")
class ChapterRevisionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "novel_id", nullable = false)
    private Long novelId;
    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;
    @Column(name = "agent_run_id")
    private Long agentRunId;
    @Column(name = "revision_type", nullable = false, length = 30)
    private String revisionType;
    @Column(length = 2000)
    private String instruction;
    @Column(name = "original_text", nullable = false, columnDefinition = "LONGTEXT")
    private String originalText;
    @Column(name = "revised_text", nullable = false, columnDefinition = "LONGTEXT")
    private String revisedText;
    @Column(name = "start_offset", nullable = false)
    private Integer startOffset;
    @Column(name = "end_offset", nullable = false)
    private Integer endOffset;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "change_summary_json", columnDefinition = "TEXT")
    private String changeSummaryJson;
    @Column(name = "warnings_json", columnDefinition = "TEXT")
    private String warningsJson;
    @Version @Column(nullable = false)
    private Long version = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; }
    void setNovelId(Long novelId) { this.novelId = novelId; }
    Long getChapterId() { return chapterId; }
    void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    Long getAgentRunId() { return agentRunId; }
    void setAgentRunId(Long agentRunId) { this.agentRunId = agentRunId; }
    String getRevisionType() { return revisionType; }
    void setRevisionType(String revisionType) { this.revisionType = revisionType; }
    String getInstruction() { return instruction; }
    void setInstruction(String instruction) { this.instruction = instruction; }
    String getOriginalText() { return originalText; }
    void setOriginalText(String originalText) { this.originalText = originalText; }
    String getRevisedText() { return revisedText; }
    void setRevisedText(String revisedText) { this.revisedText = revisedText; }
    Integer getStartOffset() { return startOffset; }
    void setStartOffset(Integer startOffset) { this.startOffset = startOffset; }
    Integer getEndOffset() { return endOffset; }
    void setEndOffset(Integer endOffset) { this.endOffset = endOffset; }
    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }
    String getChangeSummaryJson() { return changeSummaryJson; }
    void setChangeSummaryJson(String changeSummaryJson) { this.changeSummaryJson = changeSummaryJson; }
    String getWarningsJson() { return warningsJson; }
    void setWarningsJson(String warningsJson) { this.warningsJson = warningsJson; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
