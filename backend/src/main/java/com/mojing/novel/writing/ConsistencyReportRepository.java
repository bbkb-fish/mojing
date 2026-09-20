package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface ConsistencyReportRepository extends JpaRepository<ConsistencyReportEntity, Long> {
    List<ConsistencyReportEntity> findByChapterIdOrderByCreatedAtDesc(Long chapterId);
}
