package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

interface NovelRepository extends JpaRepository<NovelEntity, Long> {
}
