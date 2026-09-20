package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "character_identity")
class CharacterIdentityEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "novel_id", nullable = false) private Long novelId;
    @Column(name = "canonical_character_id", nullable = false) private Long canonicalCharacterId;
    @Column(name = "source_character_id") private Long sourceCharacterId;
    @Column(name = "identity_name", nullable = false, length = 100) private String identityName;
    @Column(name = "identity_type", nullable = false, length = 30) private String identityType;
    @Column(name = "first_appearance_chapter_no") private Integer firstAppearanceChapterNo;
    @Column(name = "reveal_chapter_no", nullable = false) private Integer revealChapterNo;
    @Column(columnDefinition = "TEXT") private String description;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; } void setNovelId(Long v) { novelId = v; }
    Long getCanonicalCharacterId() { return canonicalCharacterId; } void setCanonicalCharacterId(Long v) { canonicalCharacterId = v; }
    Long getSourceCharacterId() { return sourceCharacterId; } void setSourceCharacterId(Long v) { sourceCharacterId = v; }
    String getIdentityName() { return identityName; } void setIdentityName(String v) { identityName = v; }
    String getIdentityType() { return identityType; } void setIdentityType(String v) { identityType = v; }
    Integer getFirstAppearanceChapterNo() { return firstAppearanceChapterNo; } void setFirstAppearanceChapterNo(Integer v) { firstAppearanceChapterNo = v; }
    Integer getRevealChapterNo() { return revealChapterNo; } void setRevealChapterNo(Integer v) { revealChapterNo = v; }
    String getDescription() { return description; } void setDescription(String v) { description = v; }
}
