package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_memory")
class ChapterMemoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "novel_id", nullable = false) private Long novelId;
    @Column(name = "chapter_id", nullable = false) private Long chapterId;
    @Column(name = "chapter_no", nullable = false) private Integer chapterNo;
    @Column(name = "source_chapter_version", nullable = false) private Long sourceChapterVersion;
    @Column(nullable = false, length = 30) private String status;
    @Column(nullable = false, length = 2000) private String summary;
    @Column(name = "time_info", length = 500) private String timeInfo;
    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT") private String contentJson;
    @Column(name = "plot_progress", length = 2000) private String plotProgress;
    @Column(nullable = false, length = 20) private String importance;
    @Column(name = "provider_mode", length = 20) private String providerMode;
    @Version @Column(nullable = false) private Long version = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; } void setNovelId(Long v) { novelId = v; }
    Long getChapterId() { return chapterId; } void setChapterId(Long v) { chapterId = v; }
    Integer getChapterNo() { return chapterNo; } void setChapterNo(Integer v) { chapterNo = v; }
    Long getSourceChapterVersion() { return sourceChapterVersion; } void setSourceChapterVersion(Long v) { sourceChapterVersion = v; }
    String getStatus() { return status; } void setStatus(String v) { status = v; }
    String getSummary() { return summary; } void setSummary(String v) { summary = v; }
    String getTimeInfo() { return timeInfo; } void setTimeInfo(String v) { timeInfo = v; }
    String getContentJson() { return contentJson; } void setContentJson(String v) { contentJson = v; }
    String getPlotProgress() { return plotProgress; } void setPlotProgress(String v) { plotProgress = v; }
    String getImportance() { return importance; } void setImportance(String v) { importance = v; }
    String getProviderMode() { return providerMode; } void setProviderMode(String v) { providerMode = v; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
