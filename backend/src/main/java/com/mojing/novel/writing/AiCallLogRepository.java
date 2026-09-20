package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface AiCallLogRepository extends JpaRepository<AiCallLogEntity, Long> {
    List<AiCallLogEntity> findByRunIdOrderByIdAsc(Long runId);
}
