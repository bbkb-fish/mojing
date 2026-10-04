package com.mojing.novel.auth;

import com.mojing.novel.style.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.mojing.novel.auth.AuthDtos.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final CurrentUserProvider currentUserProvider;
    private final LoginAttemptGuard loginAttemptGuard;

    public AuthController(AuthService service, CurrentUserProvider currentUserProvider,
                          LoginAttemptGuard loginAttemptGuard) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String attemptKey = loginAttemptGuard.key(request.username(), servletRequest.getRemoteAddr());
        loginAttemptGuard.check(attemptKey);
        try {
            LoginResponse response = service.login(request);
            loginAttemptGuard.success(attemptKey);
            return response;
        } catch (AuthFailureException exception) {
            loginAttemptGuard.failure(attemptKey);
            throw exception;
        }
    }

    @GetMapping("/me")
    public UserResponse me() { return service.current(currentUserProvider.currentUserId()); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return service.updateProfile(currentUserProvider.currentUserId(), request);
    }

    @PutMapping("/password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(currentUserProvider.currentUserId(), request);
    }

    @PostMapping("/sessions/revoke")
    public void revokeSessions() { service.revokeSessions(currentUserProvider.currentUserId()); }
}
