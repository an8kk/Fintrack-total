package com.fintrack.backend.controller;

import com.fintrack.backend.dto.*;
import com.fintrack.backend.security.JwtUtils;
import com.fintrack.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @GetMapping
    public ResponseEntity<java.util.List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponseDto> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserBlockStatus(id));
    }

    @PutMapping("/{id}/details")
    public ResponseEntity<UserResponseDto> updateUserDetails(@PathVariable Long id,
            @RequestBody UserUpdateDto updateDto) {
        return ResponseEntity.ok(userService.updateUserByAdmin(id, updateDto));
    }
}