package com.mojing.novel.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_TOKEN =
            new OAuth2Error("invalid_token", "登录凭证已失效", null);
    private final AppUserRepository repository;

    AuthTokenValidator(AppUserRepository repository) { this.repository = repository; }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        try {
            long userId = Long.parseLong(token.getSubject());
            Object rawTokenVersion = token.getClaim("tokenVersion");
            if (!(rawTokenVersion instanceof Number number))
                return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
            long tokenVersion = number.longValue();
            return repository.findById(userId)
                    .filter(user -> "ACTIVE".equals(user.getStatus()))
                    .filter(user -> user.getTokenVersion() == tokenVersion)
                    .map(user -> OAuth2TokenValidatorResult.success())
                    .orElseGet(() -> OAuth2TokenValidatorResult.failure(INVALID_TOKEN));
        } catch (RuntimeException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
    }
}
