package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface StoryBibleMessageReferenceRepository extends JpaRepository<StoryBibleMessageReferenceEntity, Long> {
    List<StoryBibleMessageReferenceEntity> findByMessageIdIn(Collection<Long> messageIds);
    List<StoryBibleMessageReferenceEntity> findByEntityTypeAndEntityIdIn(String entityType, Collection<Long> entityIds);
}
