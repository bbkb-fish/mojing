package com.mojing.novel.writing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface EvalCaseRepository extends JpaRepository<EvalCaseEntity,Long>{ List<EvalCaseEntity> findByNovelIdOrderByUpdatedAtDesc(Long novelId); }
