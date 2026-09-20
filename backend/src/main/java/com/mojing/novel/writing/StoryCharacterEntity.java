package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_character")
class StoryCharacterEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "novel_id", nullable = false)
    private Long novelId;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 500)
    private String aliases;
    @Column(length = 100)
    private String role;
    @Column(length = 500)
    private String affiliations;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String personality;
    @Column(columnDefinition = "TEXT")
    private String goal;
    @Column(name = "current_state", columnDefinition = "TEXT")
    private String currentState;
    @Column(columnDefinition = "TEXT")
    private String relationships;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(name = "merged_into_character_id")
    private Long mergedIntoCharacterId;
    @Version @Column(nullable = false)
    private Long version = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }
    Long getNovelId() { return novelId; }
    void setNovelId(Long novelId) { this.novelId = novelId; }
    String getName() { return name; }
    void setName(String name) { this.name = name; }
    String getAliases() { return aliases; }
    void setAliases(String aliases) { this.aliases = aliases; }
    String getRole() { return role; }
    void setRole(String role) { this.role = role; }
    String getAffiliations() { return affiliations; }
    void setAffiliations(String affiliations) { this.affiliations = affiliations; }
    String getDescription() { return description; }
    void setDescription(String description) { this.description = description; }
    String getPersonality() { return personality; }
    void setPersonality(String personality) { this.personality = personality; }
    String getGoal() { return goal; }
    void setGoal(String goal) { this.goal = goal; }
    String getCurrentState() { return currentState; }
    void setCurrentState(String currentState) { this.currentState = currentState; }
    String getRelationships() { return relationships; }
    void setRelationships(String relationships) { this.relationships = relationships; }
    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }
    Long getMergedIntoCharacterId() { return mergedIntoCharacterId; }
    void setMergedIntoCharacterId(Long mergedIntoCharacterId) { this.mergedIntoCharacterId = mergedIntoCharacterId; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
