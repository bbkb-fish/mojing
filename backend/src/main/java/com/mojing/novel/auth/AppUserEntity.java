package com.mojing.novel.auth;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
class AppUserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false, length = 100)
    private String nickname;
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    @Column(name = "token_version", nullable = false)
    private Long tokenVersion = 0L;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    Long getId() { return id; }
    String getUsername() { return username; }
    String getPasswordHash() { return passwordHash; }
    String getNickname() { return nickname; }
    String getAvatarUrl() { return avatarUrl; }
    String getStatus() { return status; }
    LocalDateTime getLastLoginAt() { return lastLoginAt; }
    LocalDateTime getCreatedAt() { return createdAt; }
    Long getTokenVersion() { return tokenVersion == null ? 0L : tokenVersion; }
    void setUsername(String value) { username = value; }
    void setLastLoginAt(LocalDateTime value) { lastLoginAt = value; }
    void setPasswordHash(String value) { passwordHash = value; }
    void setNickname(String value) { nickname = value; }
    void revokeTokens() { tokenVersion = getTokenVersion() + 1; }
}
