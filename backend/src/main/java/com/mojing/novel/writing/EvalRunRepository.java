package com.mojing.novel.writing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface EvalRunRepository extends JpaRepository<EvalRunEntity,Long>{ List<EvalRunEntity> findByCaseIdOrderByCreatedAtDesc(Long caseId); }
