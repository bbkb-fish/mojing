package com.mojing.novel.writing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter")
class ChapterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(name = "chapter_no", nullable = false)
    private Integer chapterNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content = "";

    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    @Column(nullable = false, length = 30)
    private String status = "UNPLANNED";

    @Column(name = "content_version", nullable = false)
    private Long contentVersion = 0L;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; }
    void setNovelId(Long novelId) { this.novelId = novelId; }
    Integer getChapterNo() { return chapterNo; }
    void setChapterNo(Integer chapterNo) { this.chapterNo = chapterNo; }
    String getTitle() { return title; }
    void setTitle(String title) { this.title = title; }
    String getContent() { return content; }
    void setContent(String content) { this.content = content; }
    Integer getWordCount() { return wordCount; }
    void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }
    Long getContentVersion() { return contentVersion; }
    void setContentVersion(Long contentVersion) { this.contentVersion = contentVersion; }
    LocalDateTime getCompletedAt() { return completedAt; }
    void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
