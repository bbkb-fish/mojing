package com.mojing.novel.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

import static com.mojing.novel.auth.AuthDtos.*;

@Service
public class AuthService {
    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration jwtTtl;

    public AuthService(AppUserRepository repository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       @Value("${mojing.security.jwt-ttl:12h}") Duration jwtTtl) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtTtl = jwtTtl;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUserEntity user = repository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new AuthFailureException("用户名或密码错误"));
        if (!"ACTIVE".equals(user.getStatus()) || !passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new AuthFailureException("用户名或密码错误");
        user.setLastLoginAt(LocalDateTime.now());
        repository.saveAndFlush(user);
        return issueToken(user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String username = request.username().toLowerCase(Locale.ROOT);
        String nickname = request.nickname().trim();
        if (nickname.isEmpty()) throw new AuthRequestException("请输入昵称");
        if (repository.existsByUsernameIgnoreCase(username)) throw new AuthConflictException("用户名已被使用");
        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setLastLoginAt(LocalDateTime.now());
        try {
            repository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AuthConflictException("用户名已被使用");
        }
        return issueToken(user);
    }

    private LoginResponse issueToken(AppUserEntity user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("mojing-novel").issuedAt(issuedAt).expiresAt(expiresAt)
                .subject(user.getId().toString()).claim("username", user.getUsername())
                .claim("nickname", user.getNickname()).claim("tokenVersion", user.getTokenVersion()).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, jwtTtl.toSeconds(), toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse current(long userId) {
        return toResponse(activeUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(long userId, UpdateProfileRequest request) {
        AppUserEntity user = activeUser(userId);
        user.setNickname(request.nickname().trim());
        return toResponse(repository.saveAndFlush(user));
    }

    @Transactional
    public void changePassword(long userId, ChangePasswordRequest request) {
        AppUserEntity user = activeUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))
            throw new AuthRequestException("当前密码不正确");
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash()))
            throw new AuthRequestException("新密码不能与当前密码相同");
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.revokeTokens();
        repository.saveAndFlush(user);
    }

    @Transactional
    public void revokeSessions(long userId) {
        AppUserEntity user = activeUser(userId);
        user.revokeTokens();
        repository.saveAndFlush(user);
    }

    private AppUserEntity activeUser(long userId) {
        AppUserEntity user = repository.findById(userId)
                .orElseThrow(() -> new AuthFailureException("登录用户不存在"));
        if (!"ACTIVE".equals(user.getStatus())) throw new AuthFailureException("当前账号已停用");
        return user;
    }

    private UserResponse toResponse(AppUserEntity user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl(),
                user.getLastLoginAt(), user.getCreatedAt());
    }
}
