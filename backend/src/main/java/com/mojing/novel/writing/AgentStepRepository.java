package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface AgentStepRepository extends JpaRepository<AgentStepEntity, Long> {
    List<AgentStepEntity> findByRunIdOrderByStepNoAsc(Long runId);
}
