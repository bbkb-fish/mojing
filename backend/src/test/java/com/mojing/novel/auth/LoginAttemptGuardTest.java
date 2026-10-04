package com.mojing.novel.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptGuardTest {
    @Test
    void blocksAfterConfiguredNumberOfFailuresAndResetsAfterSuccess() {
        LoginAttemptGuard guard = new LoginAttemptGuard(3, Duration.ofMinutes(10), Duration.ofMinutes(15));
        String key = guard.key("BBKB", "127.0.0.1");

        assertThatCode(() -> guard.failure(key)).doesNotThrowAnyException();
        assertThatCode(() -> guard.failure(key)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.failure(key)).isInstanceOf(AuthRateLimitException.class);
        assertThatThrownBy(() -> guard.check(key)).isInstanceOf(AuthRateLimitException.class);

        guard.success(key);
        assertThatCode(() -> guard.check(key)).doesNotThrowAnyException();
    }
}
