package com.mojing.novel.writing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "consistency_issue")
class ConsistencyIssueEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "report_id", nullable = false) private Long reportId;
    @Column(nullable = false, length = 50) private String type;
    @Column(nullable = false, length = 20) private String severity;
    @Column(name = "quote_text", nullable = false, columnDefinition = "TEXT") private String quoteText;
    @Column(nullable = false, length = 2000) private String message;
    @Column(nullable = false, length = 2000) private String suggestion;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "revision_id") private Long revisionId;
    @Version @Column(nullable = false) private Long version = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    Long getId() { return id; }
    Long getReportId() { return reportId; } void setReportId(Long v) { reportId = v; }
    String getType() { return type; } void setType(String v) { type = v; }
    String getSeverity() { return severity; } void setSeverity(String v) { severity = v; }
    String getQuoteText() { return quoteText; } void setQuoteText(String v) { quoteText = v; }
    String getMessage() { return message; } void setMessage(String v) { message = v; }
    String getSuggestion() { return suggestion; } void setSuggestion(String v) { suggestion = v; }
    String getStatus() { return status; } void setStatus(String v) { status = v; }
    Long getRevisionId() { return revisionId; } void setRevisionId(Long v) { revisionId = v; }
    Long getVersion() { return version; }
    LocalDateTime getCreatedAt() { return createdAt; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
}
