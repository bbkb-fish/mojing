package com.mojing.novel.auth;

public class AuthFailureException extends RuntimeException {
    public AuthFailureException(String message) { super(message); }
}
