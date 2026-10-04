package com.mojing.novel.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
class LoginAttemptGuard {
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration attemptWindow;
    private final Duration blockDuration;

    LoginAttemptGuard(@Value("${mojing.security.login-max-attempts:5}") int maxAttempts,
                      @Value("${mojing.security.login-attempt-window:10m}") Duration attemptWindow,
                      @Value("${mojing.security.login-block-duration:15m}") Duration blockDuration) {
        this.maxAttempts = maxAttempts;
        this.attemptWindow = attemptWindow;
        this.blockDuration = blockDuration;
    }

    String key(String username, String remoteAddress) {
        return username.trim().toLowerCase(Locale.ROOT) + "|" + remoteAddress;
    }

    void check(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt != null && attempt.blockedUntil() != null && attempt.blockedUntil().isAfter(Instant.now()))
            throw blocked(attempt.blockedUntil());
    }

    void failure(String key) {
        Instant now = Instant.now();
        Attempt updated = attempts.compute(key, (ignored, previous) -> {
            if (previous == null || previous.firstFailure().plus(attemptWindow).isBefore(now))
                return new Attempt(1, now, null);
            int failures = previous.failures() + 1;
            return failures >= maxAttempts
                    ? new Attempt(failures, previous.firstFailure(), now.plus(blockDuration))
                    : new Attempt(failures, previous.firstFailure(), null);
        });
        if (updated.blockedUntil() != null) throw blocked(updated.blockedUntil());
        if (attempts.size() > 10_000) attempts.entrySet().removeIf(entry ->
                entry.getValue().firstFailure().plus(attemptWindow).plus(blockDuration).isBefore(now));
    }

    void success(String key) { attempts.remove(key); }

    private AuthRateLimitException blocked(Instant blockedUntil) {
        long minutes = Math.max(1, Duration.between(Instant.now(), blockedUntil).toMinutes() + 1);
        return new AuthRateLimitException("登录失败次数过多，请约 " + minutes + " 分钟后再试");
    }

    private record Attempt(int failures, Instant firstFailure, Instant blockedUntil) {}
}
