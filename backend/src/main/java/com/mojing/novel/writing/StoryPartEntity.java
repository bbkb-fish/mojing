package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_part")
class StoryPartEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "novel_id", nullable = false)
    private Long novelId;
    @Column(name = "volume_id", nullable = false)
    private Long volumeId;
    @Column(name = "part_no", nullable = false)
    private Integer partNo;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(name = "chapter_start", nullable = false)
    private Integer chapterStart;
    @Column(name = "chapter_end")
    private Integer chapterEnd;
    @Column(columnDefinition = "TEXT")
    private String objective;
    @Column(columnDefinition = "LONGTEXT")
    private String plan;
    @Column(columnDefinition = "LONGTEXT")
    private String retrospective;
    @Column(nullable = false, length = 20)
    private String status = "GENERATED";
    @Version @Column(nullable = false)
    private Long version = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; }
    void setNovelId(Long novelId) { this.novelId = novelId; }
    Long getVolumeId() { return volumeId; }
    void setVolumeId(Long volumeId) { this.volumeId = volumeId; }
    Integer getPartNo() { return partNo; }
    void setPartNo(Integer partNo) { this.partNo = partNo; }
    String getTitle() { return title; }
    void setTitle(String title) { this.title = title; }
    Integer getChapterStart() { return chapterStart; }
    void setChapterStart(Integer chapterStart) { this.chapterStart = chapterStart; }
    Integer getChapterEnd() { return chapterEnd; }
    void setChapterEnd(Integer chapterEnd) { this.chapterEnd = chapterEnd; }
    String getObjective() { return objective; }
    void setObjective(String objective) { this.objective = objective; }
    String getPlan() { return plan; }
    void setPlan(String plan) { this.plan = plan; }
    String getRetrospective() { return retrospective; }
    void setRetrospective(String retrospective) { this.retrospective = retrospective; }
    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
