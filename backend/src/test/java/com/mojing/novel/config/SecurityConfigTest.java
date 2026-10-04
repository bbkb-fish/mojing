package com.mojing.novel.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {
    private final SecurityConfig config = new SecurityConfig();

    @Test
    void productionRejectsMissingOrShortJwtSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> config.jwtSecretKey("", environment))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.jwtSecretKey("short", environment))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productionAcceptsLongJwtSecret() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThat(config.jwtSecretKey("a-secret-with-at-least-32-characters", environment).getEncoded())
                .hasSize(32);
    }
}
