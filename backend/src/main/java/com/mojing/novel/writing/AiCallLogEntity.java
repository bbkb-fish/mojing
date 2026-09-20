package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_call_log")
class AiCallLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "run_id", nullable = false) private Long runId;
    @Column(name = "step_type", nullable = false, length = 50) private String stepType;
    @Column(nullable = false, length = 100) private String model;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "prompt_tokens", nullable = false) private Integer promptTokens = 0;
    @Column(name = "completion_tokens", nullable = false) private Integer completionTokens = 0;
    @Column(name = "total_tokens", nullable = false) private Integer totalTokens = 0;
    @Column(name = "duration_ms", nullable = false) private Long durationMs = 0L;
    @Column(name = "retry_count", nullable = false) private Integer retryCount = 0;
    @Column(name = "error_message", length = 2000) private String errorMessage;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    Long getId() { return id; }
    Long getRunId() { return runId; } void setRunId(Long v) { runId = v; }
    String getStepType() { return stepType; } void setStepType(String v) { stepType = v; }
    String getModel() { return model; } void setModel(String v) { model = v; }
    String getStatus() { return status; } void setStatus(String v) { status = v; }
    Integer getPromptTokens() { return promptTokens; } void setPromptTokens(Integer v) { promptTokens = v; }
    Integer getCompletionTokens() { return completionTokens; } void setCompletionTokens(Integer v) { completionTokens = v; }
    Integer getTotalTokens() { return totalTokens; } void setTotalTokens(Integer v) { totalTokens = v; }
    Long getDurationMs() { return durationMs; } void setDurationMs(Long v) { durationMs = v; }
    Integer getRetryCount() { return retryCount; } void setRetryCount(Integer v) { retryCount = v; }
    String getErrorMessage() { return errorMessage; } void setErrorMessage(String v) { errorMessage = v; }
    LocalDateTime getCreatedAt() { return createdAt; }
}
