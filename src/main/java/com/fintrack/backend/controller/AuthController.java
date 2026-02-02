package com.fintrack.backend.controller;

import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.UserRepository;
import com.fintrack.backend.security.JwtUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Import Slf4j
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j 
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        log.info("Attempting to register user with email: {}", user.getEmail());

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            log.warn("Registration failed: Email {} already exists", user.getEmail());
            return ResponseEntity.badRequest().body("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setBalance(BigDecimal.ZERO);
        
        userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());
        
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            log.debug("Authentication successful for {}", request.getEmail());

            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            final String token = jwtUtils.generateToken(userDetails);
            
            User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

            log.info("Token generated successfully for user ID: {}", user.getId());
            return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getUsername()));

        } catch (AuthenticationException e) {
            log.error("Login failed for {}: Invalid credentials", request.getEmail());
            return ResponseEntity.status(403).body("Invalid email or password");
        }
    }
}

@Data
class LoginRequest {
    private String email;
    private String password;
}

@Data
class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    
    public AuthResponse(String token, Long userId, String username) {
        this.token = token;
        this.userId = userId;
        this.username = username;
    }
}