package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface StoryPartRepository extends JpaRepository<StoryPartEntity, Long> {
    List<StoryPartEntity> findByVolumeIdOrderByPartNoAsc(Long volumeId);
    boolean existsByVolumeIdAndPartNo(Long volumeId, Integer partNo);
    boolean existsByVolumeIdAndPartNoAndIdNot(Long volumeId, Integer partNo, Long id);
}
