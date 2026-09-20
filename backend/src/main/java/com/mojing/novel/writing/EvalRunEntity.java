package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name="eval_run")
class EvalRunEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="case_id",nullable=false) private Long caseId;
    @Column(nullable=false,length=30) private String status;
    @Column(name="generator_model",length=100) private String generatorModel;
    @Column(name="generator_prompt_version",nullable=false,length=50) private String generatorPromptVersion;
    @Column(name="judge_prompt_version",nullable=false,length=50) private String judgePromptVersion;
    @Column(name="generated_text",columnDefinition="LONGTEXT") private String generatedText;
    @Column(name="rule_score") private Integer ruleScore;
    @Column(name="judge_score") private Integer judgeScore;
    @Column(name="overall_score") private Integer overallScore;
    private Boolean passed;
    @Column(name="violations_json",columnDefinition="TEXT") private String violationsJson;
    @Column(name="judge_feedback",length=4000) private String judgeFeedback;
    @Column(name="prompt_tokens",nullable=false) private Integer promptTokens=0;
    @Column(name="completion_tokens",nullable=false) private Integer completionTokens=0;
    @Column(name="total_tokens",nullable=false) private Integer totalTokens=0;
    @Column(name="duration_ms") private Long durationMs;
    @Column(name="error_message",length=2000) private String errorMessage;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    Long getId(){return id;} Long getCaseId(){return caseId;} void setCaseId(Long v){caseId=v;} String getStatus(){return status;} void setStatus(String v){status=v;}
    String getGeneratorModel(){return generatorModel;} void setGeneratorModel(String v){generatorModel=v;} String getGeneratorPromptVersion(){return generatorPromptVersion;} void setGeneratorPromptVersion(String v){generatorPromptVersion=v;}
    String getJudgePromptVersion(){return judgePromptVersion;} void setJudgePromptVersion(String v){judgePromptVersion=v;} String getGeneratedText(){return generatedText;} void setGeneratedText(String v){generatedText=v;}
    Integer getRuleScore(){return ruleScore;} void setRuleScore(Integer v){ruleScore=v;} Integer getJudgeScore(){return judgeScore;} void setJudgeScore(Integer v){judgeScore=v;}
    Integer getOverallScore(){return overallScore;} void setOverallScore(Integer v){overallScore=v;} Boolean getPassed(){return passed;} void setPassed(Boolean v){passed=v;}
    String getViolationsJson(){return violationsJson;} void setViolationsJson(String v){violationsJson=v;} String getJudgeFeedback(){return judgeFeedback;} void setJudgeFeedback(String v){judgeFeedback=v;}
    Integer getPromptTokens(){return promptTokens;} void setPromptTokens(Integer v){promptTokens=v;} Integer getCompletionTokens(){return completionTokens;} void setCompletionTokens(Integer v){completionTokens=v;}
    Integer getTotalTokens(){return totalTokens;} void setTotalTokens(Integer v){totalTokens=v;} Long getDurationMs(){return durationMs;} void setDurationMs(Long v){durationMs=v;}
    String getErrorMessage(){return errorMessage;} void setErrorMessage(String v){errorMessage=v;} LocalDateTime getCreatedAt(){return createdAt;}
}
