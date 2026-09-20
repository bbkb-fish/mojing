package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface NovelProjectProfileRepository extends JpaRepository<NovelProjectProfileEntity, Long> {
    Optional<NovelProjectProfileEntity> findByNovelId(Long novelId);
}
