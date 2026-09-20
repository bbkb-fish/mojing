package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface StoryVolumeRepository extends JpaRepository<StoryVolumeEntity, Long> {
    List<StoryVolumeEntity> findByNovelIdOrderByVolumeNoAsc(Long novelId);
    boolean existsByNovelIdAndVolumeNoAndIdNot(Long novelId, Integer volumeNo, Long id);
    boolean existsByNovelIdAndVolumeNo(Long novelId, Integer volumeNo);
}
