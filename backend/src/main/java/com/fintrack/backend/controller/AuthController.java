package com.fintrack.backend.controller;

import com.fintrack.backend.dto.AuthResponse;
import com.fintrack.backend.dto.ForgotPasswordRequest;
import com.fintrack.backend.dto.LoginRequest;
import com.fintrack.backend.dto.ResetPasswordDto;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody User user) {
        log.info("REST request to register user: {}", user.getEmail());
        return ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to login user: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("REST request for forgot password for email: {}", request.getEmail());
        return ResponseEntity.ok(authService.requestPasswordReset(request.getEmail()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        log.info("REST request to reset password with token");
        return ResponseEntity.ok(authService.resetPassword(dto));
    }
}
