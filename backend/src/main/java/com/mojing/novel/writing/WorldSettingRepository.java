package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface WorldSettingRepository extends JpaRepository<WorldSettingEntity, Long> {
    List<WorldSettingEntity> findByNovelIdOrderByIdAsc(Long novelId);
}
