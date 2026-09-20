package com.mojing.novel.style;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "writing_style_profile")
class WritingStyleProfileEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;
    @Column(name = "novel_id")
    private Long novelId;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;
    @Column(length = 2000)
    private String description;
    @Column(name = "rules_text", nullable = false, columnDefinition = "TEXT")
    private String rulesText;
    @Column(name = "forbidden_words", columnDefinition = "TEXT")
    private String forbiddenWords;
    @Column(name = "reference_excerpt", columnDefinition = "LONGTEXT")
    private String referenceExcerpt;
    @Version @Column(nullable = false)
    private Long version = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getOwnerUserId() { return ownerUserId; }
    void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    Long getNovelId() { return novelId; }
    void setNovelId(Long novelId) { this.novelId = novelId; }
    String getName() { return name; }
    void setName(String name) { this.name = name; }
    String getSourceType() { return sourceType; }
    void setSourceType(String sourceType) { this.sourceType = sourceType; }
    String getDescription() { return description; }
    void setDescription(String description) { this.description = description; }
    String getRulesText() { return rulesText; }
    void setRulesText(String rulesText) { this.rulesText = rulesText; }
    String getForbiddenWords() { return forbiddenWords; }
    void setForbiddenWords(String forbiddenWords) { this.forbiddenWords = forbiddenWords; }
    String getReferenceExcerpt() { return referenceExcerpt; }
    void setReferenceExcerpt(String referenceExcerpt) { this.referenceExcerpt = referenceExcerpt; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
