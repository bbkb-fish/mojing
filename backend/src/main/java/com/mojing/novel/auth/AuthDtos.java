package com.mojing.novel.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "请输入用户名") @Size(max = 50) String username,
            @NotBlank(message = "请输入密码") @Size(max = 100) String password) {}

    public record RegisterRequest(
            @NotBlank(message = "请输入用户名")
            @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{2,49}", message = "用户名需为3到50位，以字母开头，仅含字母、数字和下划线") String username,
            @NotBlank(message = "请输入昵称") @Size(max = 100, message = "昵称不能超过100个字符") String nickname,
            @NotBlank(message = "请输入密码") @Size(min = 8, max = 100, message = "密码需要8到100个字符") String password) {}

    public record UserResponse(Long id, String username, String nickname, String avatarUrl,
                               LocalDateTime lastLoginAt, LocalDateTime createdAt) {}

    public record LoginResponse(String accessToken, long expiresIn, UserResponse user) {}

    public record UpdateProfileRequest(
            @NotBlank(message = "请输入昵称") @Size(max = 100, message = "昵称不能超过100个字符") String nickname) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "请输入当前密码") @Size(max = 100) String currentPassword,
            @NotBlank(message = "请输入新密码")
            @Size(min = 8, max = 100, message = "新密码需要8到100个字符") String newPassword) {}
}
