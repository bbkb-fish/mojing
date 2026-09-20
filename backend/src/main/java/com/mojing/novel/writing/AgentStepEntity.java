package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_step")
class AgentStepEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false)
    private Long runId;
    @Column(name = "step_no", nullable = false)
    private Integer stepNo;
    @Column(nullable = false, length = 50)
    private String type;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "summary", length = 1000)
    private String summary;
    @Column(name = "duration_ms")
    private Long durationMs;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    Long getId() { return id; }
    Long getRunId() { return runId; }
    void setRunId(Long runId) { this.runId = runId; }
    Integer getStepNo() { return stepNo; }
    void setStepNo(Integer stepNo) { this.stepNo = stepNo; }
    String getType() { return type; }
    void setType(String type) { this.type = type; }
    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }
    String getSummary() { return summary; }
    void setSummary(String summary) { this.summary = summary; }
    Long getDurationMs() { return durationMs; }
    void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    LocalDateTime getCreatedAt() { return createdAt; }
}
