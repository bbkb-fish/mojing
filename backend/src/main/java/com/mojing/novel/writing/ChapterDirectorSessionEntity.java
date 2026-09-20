package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_director_session")
class ChapterDirectorSessionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "novel_id", nullable = false) private Long novelId;
    @Column(name = "chapter_id", nullable = false, unique = true) private Long chapterId;
    @Column(name = "current_run_id") private Long currentRunId;
    @Column(name = "current_plan_json", columnDefinition = "LONGTEXT") private String currentPlanJson;
    @Version @Column(nullable = false) private Long version;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; }
    void setNovelId(Long novelId) { this.novelId = novelId; }
    Long getChapterId() { return chapterId; }
    void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    Long getCurrentRunId() { return currentRunId; }
    void setCurrentRunId(Long currentRunId) { this.currentRunId = currentRunId; }
    String getCurrentPlanJson() { return currentPlanJson; }
    void setCurrentPlanJson(String currentPlanJson) { this.currentPlanJson = currentPlanJson; }
}
