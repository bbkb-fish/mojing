package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

interface AgentRunRepository extends JpaRepository<AgentRunEntity, Long> {
    List<AgentRunEntity> findByNovelIdOrderByCreatedAtDesc(Long novelId, Pageable pageable);
}
