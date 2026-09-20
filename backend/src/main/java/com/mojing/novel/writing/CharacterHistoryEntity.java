package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "character_history")
class CharacterHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "novel_id", nullable = false) private Long novelId;
    @Column(name = "character_id", nullable = false) private Long characterId;
    @Column(name = "character_name", nullable = false, length = 100) private String characterName;
    @Column(name = "chapter_id", nullable = false) private Long chapterId;
    @Column(name = "chapter_no", nullable = false) private Integer chapterNo;
    @Column(name = "source_chapter_version", nullable = false) private Long sourceChapterVersion;
    @Column(columnDefinition = "TEXT") private String actions;
    @Column(name = "state_change", columnDefinition = "TEXT") private String stateChange;
    @Column(name = "new_knowledge", columnDefinition = "TEXT") private String newKnowledge;
    @Column(name = "foreshadowings_json", columnDefinition = "TEXT") private String foreshadowingsJson;
    @Column(name = "profile_changes_json", columnDefinition = "LONGTEXT") private String profileChangesJson;
    @Column(nullable = false, length = 30) private String status;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; } void setNovelId(Long value) { novelId = value; }
    Long getCharacterId() { return characterId; } void setCharacterId(Long value) { characterId = value; }
    String getCharacterName() { return characterName; } void setCharacterName(String value) { characterName = value; }
    Long getChapterId() { return chapterId; } void setChapterId(Long value) { chapterId = value; }
    Integer getChapterNo() { return chapterNo; } void setChapterNo(Integer value) { chapterNo = value; }
    Long getSourceChapterVersion() { return sourceChapterVersion; } void setSourceChapterVersion(Long value) { sourceChapterVersion = value; }
    String getActions() { return actions; } void setActions(String value) { actions = value; }
    String getStateChange() { return stateChange; } void setStateChange(String value) { stateChange = value; }
    String getNewKnowledge() { return newKnowledge; } void setNewKnowledge(String value) { newKnowledge = value; }
    String getForeshadowingsJson() { return foreshadowingsJson; } void setForeshadowingsJson(String value) { foreshadowingsJson = value; }
    String getProfileChangesJson() { return profileChangesJson; } void setProfileChangesJson(String value) { profileChangesJson = value; }
    String getStatus() { return status; } void setStatus(String value) { status = value; }
    LocalDateTime getCreatedAt() { return createdAt; }
}
