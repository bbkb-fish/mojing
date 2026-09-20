package com.mojing.novel.writing;

import jakarta.persistence.*;

@Entity
@Table(name = "story_bible_message_reference",
        uniqueConstraints = @UniqueConstraint(name = "uk_story_bible_message_reference",
                columnNames = {"message_id", "entity_type", "entity_id"}))
class StoryBibleMessageReferenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "message_id", nullable = false) private Long messageId;
    @Column(name = "entity_type", nullable = false, length = 30) private String entityType;
    @Column(name = "entity_id", nullable = false) private Long entityId;
    @Column(name = "reference_type", nullable = false, length = 20) private String referenceType;

    Long getMessageId() { return messageId; }
    void setMessageId(Long messageId) { this.messageId = messageId; }
    String getEntityType() { return entityType; }
    void setEntityType(String entityType) { this.entityType = entityType; }
    Long getEntityId() { return entityId; }
    void setEntityId(Long entityId) { this.entityId = entityId; }
    String getReferenceType() { return referenceType; }
    void setReferenceType(String referenceType) { this.referenceType = referenceType; }
}
