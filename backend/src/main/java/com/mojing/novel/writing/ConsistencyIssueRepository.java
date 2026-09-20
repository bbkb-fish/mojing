package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface ConsistencyIssueRepository extends JpaRepository<ConsistencyIssueEntity, Long> {
    List<ConsistencyIssueEntity> findByReportIdOrderByIdAsc(Long reportId);
    Optional<ConsistencyIssueEntity> findByRevisionId(Long revisionId);
}
