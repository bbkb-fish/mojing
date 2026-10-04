package com.mojing.novel.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthTokenValidatorTest {
    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final AuthTokenValidator validator = new AuthTokenValidator(repository);

    @Test
    void acceptsOnlyTheCurrentActiveSessionVersion() {
        AppUserEntity user = mock(AppUserEntity.class);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn("ACTIVE");
        when(user.getTokenVersion()).thenReturn(2L);

        assertThat(validator.validate(token(2L)).hasErrors()).isFalse();
        assertThat(validator.validate(token(1L)).hasErrors()).isTrue();
    }

    @Test
    void rejectsTokensForMissingOrDisabledUsers() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThat(validator.validate(token(0L)).hasErrors()).isTrue();
    }

    private Jwt token(long version) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token").header("alg", "HS256").subject("1")
                .issuedAt(now).expiresAt(now.plusSeconds(60)).claim("tokenVersion", version).build();
    }
}
