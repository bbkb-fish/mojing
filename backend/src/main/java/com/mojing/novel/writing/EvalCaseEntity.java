package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "eval_case")
class EvalCaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="novel_id", nullable=false) private Long novelId;
    @Column(name="chapter_id") private Long chapterId;
    @Column(nullable=false, length=200) private String name;
    @Column(name="input_context", nullable=false, columnDefinition="LONGTEXT") private String inputContext;
    @Column(length=2000) private String instruction;
    @Column(name="expected_characters", length=2000) private String expectedCharacters;
    @Column(name="required_terms", length=2000) private String requiredTerms;
    @Column(name="forbidden_terms", length=2000) private String forbiddenTerms;
    @Column(name="min_length", nullable=false) private Integer minLength;
    @Column(name="max_length", nullable=false) private Integer maxLength;
    @Column(nullable=false) private Boolean enabled = true;
    @Version @Column(nullable=false) private Long version = 0L;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    Long getId(){return id;} Long getNovelId(){return novelId;} void setNovelId(Long v){novelId=v;}
    Long getChapterId(){return chapterId;} void setChapterId(Long v){chapterId=v;} String getName(){return name;} void setName(String v){name=v;}
    String getInputContext(){return inputContext;} void setInputContext(String v){inputContext=v;} String getInstruction(){return instruction;} void setInstruction(String v){instruction=v;}
    String getExpectedCharacters(){return expectedCharacters;} void setExpectedCharacters(String v){expectedCharacters=v;} String getRequiredTerms(){return requiredTerms;} void setRequiredTerms(String v){requiredTerms=v;}
    String getForbiddenTerms(){return forbiddenTerms;} void setForbiddenTerms(String v){forbiddenTerms=v;} Integer getMinLength(){return minLength;} void setMinLength(Integer v){minLength=v;}
    Integer getMaxLength(){return maxLength;} void setMaxLength(Integer v){maxLength=v;} Boolean getEnabled(){return enabled;} void setEnabled(Boolean v){enabled=v;}
    Long getVersion(){return version;} LocalDateTime getCreatedAt(){return createdAt;} LocalDateTime getUpdatedAt(){return updatedAt;}
}
