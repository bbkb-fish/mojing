package com.mojing.novel.writing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {
    List<ChapterEntity> findByNovelIdOrderByChapterNoAsc(Long novelId);

    @Query("select coalesce(max(c.chapterNo), 0) from ChapterEntity c where c.novelId = :novelId")
    Integer findMaxChapterNo(@Param("novelId") Long novelId);

    @Query("select coalesce(sum(c.wordCount), 0) from ChapterEntity c where c.novelId = :novelId")
    Long sumWords(@Param("novelId") Long novelId);
}
