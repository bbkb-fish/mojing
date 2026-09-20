package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface StoryOrganizationRepository extends JpaRepository<StoryOrganizationEntity, Long> {
    List<StoryOrganizationEntity> findByNovelIdOrderByIdAsc(Long novelId);
}
