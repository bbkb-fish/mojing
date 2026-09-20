package com.mojing.novel.style;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface WritingStyleProfileRepository extends JpaRepository<WritingStyleProfileEntity, Long> {
    List<WritingStyleProfileEntity> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);
    Optional<WritingStyleProfileEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}
