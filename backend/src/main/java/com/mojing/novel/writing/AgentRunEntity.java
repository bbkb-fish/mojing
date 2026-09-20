package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_run")
class AgentRunEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "novel_id", nullable = false)
    private Long novelId;
    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "current_step", nullable = false, length = 50)
    private String currentStep;
    @Column(columnDefinition = "TEXT")
    private String guidance;
    @Column(name = "plan_json", columnDefinition = "LONGTEXT")
    private String planJson;
    @Column(columnDefinition = "LONGTEXT")
    private String draft;
    @Column(name = "provider_mode", length = 20)
    private String providerMode;
    @Column(length = 50)
    private String operation;
    @Column(length = 100)
    private String model;
    @Column(name = "total_duration_ms")
    private Long totalDurationMs;
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; }
    void setNovelId(Long novelId) { this.novelId = novelId; }
    Long getChapterId() { return chapterId; }
    void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }
    String getCurrentStep() { return currentStep; }
    void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    String getGuidance() { return guidance; }
    void setGuidance(String guidance) { this.guidance = guidance; }
    String getPlanJson() { return planJson; }
    void setPlanJson(String planJson) { this.planJson = planJson; }
    String getDraft() { return draft; }
    void setDraft(String draft) { this.draft = draft; }
    String getProviderMode() { return providerMode; }
    void setProviderMode(String providerMode) { this.providerMode = providerMode; }
    String getOperation() { return operation; }
    void setOperation(String operation) { this.operation = operation; }
    String getModel() { return model; }
    void setModel(String model) { this.model = model; }
    Long getTotalDurationMs() { return totalDurationMs; }
    void setTotalDurationMs(Long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
    String getErrorMessage() { return errorMessage; }
    void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
