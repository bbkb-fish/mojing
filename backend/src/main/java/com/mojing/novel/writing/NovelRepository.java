package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface NovelRepository extends JpaRepository<NovelEntity, Long> {
    List<NovelEntity> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);
    Optional<NovelEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}
