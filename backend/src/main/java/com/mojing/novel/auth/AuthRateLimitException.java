package com.mojing.novel.auth;

public class AuthRateLimitException extends RuntimeException {
    public AuthRateLimitException(String message) { super(message); }
}
