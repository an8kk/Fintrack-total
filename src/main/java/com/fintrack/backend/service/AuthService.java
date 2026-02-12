package com.fintrack.backend.service;

import com.fintrack.backend.dto.AuthResponse;
import com.fintrack.backend.dto.LoginRequest;
import com.fintrack.backend.dto.ResetPasswordDto;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.exception.EmailAlreadyExistsException;
import com.fintrack.backend.exception.ResourceNotFoundException;
import com.fintrack.backend.repository.UserRepository;
import com.fintrack.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final CategoryService categoryService;

    @Transactional
    public String register(User user) {
        log.info("Attempting to register user with email: {}", user.getEmail());
        log.debug("User details: {}", user);

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            log.warn("Registration failed: Email {} already exists", user.getEmail());
            throw new EmailAlreadyExistsException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setBalance(BigDecimal.ZERO);

        User savedUser = userRepository.save(user);
        categoryService.createDefaultCategories(savedUser);

        log.info("User registered successfully with ID: {}", savedUser.getId());
        return "User registered successfully";
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        log.debug("Authenticating...");

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            log.error("Authentication failed for email: {} - {}", request.getEmail(), e.getMessage());
            throw e;
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        final String token = jwtUtils.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
            log.error("User not found after successful authentication: {}", request.getEmail());
            return new ResourceNotFoundException("User not found");
        });

        log.info("Login successful for user ID: {}", user.getId());
        log.debug("JWT Token generated.");

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(),
                user.isBlocked());
    }

    public String requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String mockToken = "reset-" + user.getId();
        log.info("Password reset token generated for user {}: {}", user.getId(), mockToken);
        log.debug("Sending reset email to {}...", email);

        return "Password reset link sent to your email (Demo token logged in backend)";
    }

    @Transactional
    public String resetPassword(ResetPasswordDto dto) {
        log.info("Attempting to reset password with token: {}", dto.getToken());

        if (!dto.getToken().startsWith("reset-")) {
            log.warn("Invalid reset token format: {}", dto.getToken());
            throw new IllegalArgumentException("Invalid reset token");
        }

        Long userId = Long.parseLong(dto.getToken().substring(6));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for token"));

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setBalance(BigDecimal.ZERO);
        userRepository.save(user);

        log.info("Password successfully reset for user ID: {}", userId);
        return "Password reset successfully";
    }
}
