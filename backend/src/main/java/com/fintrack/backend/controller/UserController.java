package com.fintrack.backend.controller;

import com.fintrack.backend.dto.*;
import com.fintrack.backend.security.JwtUtils;
import com.fintrack.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @PutMapping("/{id}")
    public ResponseEntity<AuthResponse> updateUser(@PathVariable("id") Long id,
            @Valid @RequestBody UserUpdateDto updateDto) {
        log.info("PUT /api/users/{} — username={}, email={}", id, updateDto.getUsername(), updateDto.getEmail());
        UserResponseDto userDto = userService.updateUser(id, updateDto);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userDto.getEmail());
        String newToken = jwtUtils.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(
                newToken,
                userDto.getId(),
                userDto.getUsername(),
                userDto.getEmail(),
                userDto.getRole(),
                userDto.isBlocked()));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<AuthResponse> changePassword(@PathVariable("id") Long id,
            @Valid @RequestBody PasswordChangeDto passDto) {
        log.info("PUT /api/users/{}/password", id);
        userService.changePassword(id, passDto);
        UserResponseDto userDto = userService.getUserProfile(id);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userDto.getEmail());
        String newToken = jwtUtils.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(
                newToken,
                userDto.getId(),
                userDto.getUsername(),
                userDto.getEmail(),
                userDto.getRole(),
                userDto.isBlocked()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getProfile(@PathVariable("id") Long id) {
        log.info("GET /api/users/{}", id);
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<UserResponseDto>> getAllUsers() {
        log.info("GET /api/users — listing all users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> toggleUserStatus(@PathVariable Long id) {
        log.info("PUT /api/users/{}/status — toggling block status", id);
        return ResponseEntity.ok(userService.toggleUserBlockStatus(id));
    }

    @PutMapping("/{id}/details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUserDetails(@PathVariable Long id,
            @RequestBody UserUpdateDto updateDto) {
        log.info("PUT /api/users/{}/details — admin update, username={}", id, updateDto.getUsername());
        return ResponseEntity.ok(userService.updateUserByAdmin(id, updateDto));
    }
}