package com.mojing.novel.writing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "novel", indexes = @Index(name = "idx_novel_owner_updated", columnList = "owner_user_id,updated_at"))
class NovelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "LONGTEXT")
    private String outline;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "total_words", nullable = false)
    private Integer totalWords = 0;

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
    Long getOwnerUserId() { return ownerUserId; }
    void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    String getTitle() { return title; }
    void setTitle(String title) { this.title = title; }
    String getDescription() { return description; }
    void setDescription(String description) { this.description = description; }
    String getOutline() { return outline; }
    void setOutline(String outline) { this.outline = outline; }
    String getCoverUrl() { return coverUrl; }
    void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }
    Integer getTotalWords() { return totalWords; }
    void setTotalWords(Integer totalWords) { this.totalWords = totalWords; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
