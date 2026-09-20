package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_volume")
class StoryVolumeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "novel_id", nullable = false)
    private Long novelId;
    @Column(name = "volume_no", nullable = false)
    private Integer volumeNo;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(name = "chapter_start", nullable = false)
    private Integer chapterStart;
    @Column(name = "chapter_end")
    private Integer chapterEnd;
    @Column(columnDefinition = "TEXT")
    private String objective;
    @Column(columnDefinition = "LONGTEXT")
    private String retrospective;
    @Column(name = "chapter_facts_json", columnDefinition = "LONGTEXT")
    private String chapterFactsJson;
    @Column(name = "future_plan", columnDefinition = "LONGTEXT")
    private String futurePlan;
    @Column(name = "key_turning_points", columnDefinition = "TEXT")
    private String keyTurningPoints;
    @Column(columnDefinition = "TEXT")
    private String climax;
    @Column(name = "ending_hook", columnDefinition = "TEXT")
    private String endingHook;
    @Column(columnDefinition = "TEXT")
    private String foreshadows;
    @Column(name = "locked_beats", columnDefinition = "TEXT")
    private String lockedBeats;
    @Column(name = "analyzed_through_chapter_no")
    private Integer analyzedThroughChapterNo;
    @Column(name = "source_content_version_sum")
    private Long sourceContentVersionSum;
    @Column(nullable = false, length = 20)
    private String status = "GENERATED";
    @Version @Column(nullable = false)
    private Long version = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getNovelId() { return novelId; }
    void setNovelId(Long novelId) { this.novelId = novelId; }
    Integer getVolumeNo() { return volumeNo; }
    void setVolumeNo(Integer volumeNo) { this.volumeNo = volumeNo; }
    String getTitle() { return title; }
    void setTitle(String title) { this.title = title; }
    Integer getChapterStart() { return chapterStart; }
    void setChapterStart(Integer chapterStart) { this.chapterStart = chapterStart; }
    Integer getChapterEnd() { return chapterEnd; }
    void setChapterEnd(Integer chapterEnd) { this.chapterEnd = chapterEnd; }
    String getObjective() { return objective; }
    void setObjective(String objective) { this.objective = objective; }
    String getRetrospective() { return retrospective; }
    void setRetrospective(String retrospective) { this.retrospective = retrospective; }
    String getChapterFactsJson() { return chapterFactsJson; }
    void setChapterFactsJson(String chapterFactsJson) { this.chapterFactsJson = chapterFactsJson; }
    String getFuturePlan() { return futurePlan; }
    void setFuturePlan(String futurePlan) { this.futurePlan = futurePlan; }
    String getKeyTurningPoints() { return keyTurningPoints; }
    void setKeyTurningPoints(String keyTurningPoints) { this.keyTurningPoints = keyTurningPoints; }
    String getClimax() { return climax; }
    void setClimax(String climax) { this.climax = climax; }
    String getEndingHook() { return endingHook; }
    void setEndingHook(String endingHook) { this.endingHook = endingHook; }
    String getForeshadows() { return foreshadows; }
    void setForeshadows(String foreshadows) { this.foreshadows = foreshadows; }
    String getLockedBeats() { return lockedBeats; }
    void setLockedBeats(String lockedBeats) { this.lockedBeats = lockedBeats; }
    Integer getAnalyzedThroughChapterNo() { return analyzedThroughChapterNo; }
    void setAnalyzedThroughChapterNo(Integer value) { this.analyzedThroughChapterNo = value; }
    Long getSourceContentVersionSum() { return sourceContentVersionSum; }
    void setSourceContentVersionSum(Long value) { this.sourceContentVersionSum = value; }
    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
