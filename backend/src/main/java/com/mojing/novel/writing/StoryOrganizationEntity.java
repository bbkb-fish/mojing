package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_organization")
class StoryOrganizationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "novel_id", nullable = false)
    private Long novelId;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 500)
    private String aliases;
    @Column(name = "organization_type", length = 100)
    private String type;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String goal;
    @Column(name = "organization_structure", columnDefinition = "TEXT")
    private String structure;
    @Column(columnDefinition = "TEXT")
    private String relationships;
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
    String getType() { return type; }
    void setType(String type) { this.type = type; }
    String getDescription() { return description; }
    void setDescription(String description) { this.description = description; }
    String getGoal() { return goal; }
    void setGoal(String goal) { this.goal = goal; }
    String getStructure() { return structure; }
    void setStructure(String structure) { this.structure = structure; }
    String getRelationships() { return relationships; }
    void setRelationships(String relationships) { this.relationships = relationships; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
