package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "novel_project_profile")
class NovelProjectProfileEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "novel_id", nullable = false, unique = true) private Long novelId;
    @Column(columnDefinition = "TEXT") private String inspiration;
    @Column(length = 100) private String genre;
    @Column(length = 100) private String channel;
    @Column(name = "target_audience", length = 500) private String targetAudience;
    @Column(length = 100) private String platform;
    @Column(name = "core_selling_point", columnDefinition = "TEXT") private String coreSellingPoint;
    @Column(name = "protagonist_hook", columnDefinition = "TEXT") private String protagonistHook;
    @Column(name = "growth_route", columnDefinition = "TEXT") private String growthRoute;
    @Column(name = "reader_expectations", columnDefinition = "TEXT") private String readerExpectations;
    @Column(name = "opening_three_chapters", columnDefinition = "TEXT") private String openingThreeChapters;
    @Column(name = "expected_words") private Integer expectedWords;
    @Column(name = "expected_volumes") private Integer expectedVolumes;
    @Column(name = "chapter_word_target") private Integer chapterWordTarget;
    @Version @Column(nullable = false) private Long version = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; } void setNovelId(Long value) { novelId = value; }
    String getInspiration() { return inspiration; } void setInspiration(String value) { inspiration = value; }
    String getGenre() { return genre; } void setGenre(String value) { genre = value; }
    String getChannel() { return channel; } void setChannel(String value) { channel = value; }
    String getTargetAudience() { return targetAudience; } void setTargetAudience(String value) { targetAudience = value; }
    String getPlatform() { return platform; } void setPlatform(String value) { platform = value; }
    String getCoreSellingPoint() { return coreSellingPoint; } void setCoreSellingPoint(String value) { coreSellingPoint = value; }
    String getProtagonistHook() { return protagonistHook; } void setProtagonistHook(String value) { protagonistHook = value; }
    String getGrowthRoute() { return growthRoute; } void setGrowthRoute(String value) { growthRoute = value; }
    String getReaderExpectations() { return readerExpectations; } void setReaderExpectations(String value) { readerExpectations = value; }
    String getOpeningThreeChapters() { return openingThreeChapters; } void setOpeningThreeChapters(String value) { openingThreeChapters = value; }
    Integer getExpectedWords() { return expectedWords; } void setExpectedWords(Integer value) { expectedWords = value; }
    Integer getExpectedVolumes() { return expectedVolumes; } void setExpectedVolumes(Integer value) { expectedVolumes = value; }
    Integer getChapterWordTarget() { return chapterWordTarget; } void setChapterWordTarget(Integer value) { chapterWordTarget = value; }
    Long getVersion() { return version; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
