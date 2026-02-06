package com.fintrack.backend.service;

import com.fintrack.backend.dto.PasswordChangeDto;
import com.fintrack.backend.dto.UserResponseDto;
import com.fintrack.backend.dto.UserUpdateDto;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. Update Profile Logic
    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateDto request) {
        log.info("Updating profile for User ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        }
        
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }

        User savedUser = userRepository.save(user);
        log.info("User profile updated successfully");
        
        return mapToDto(savedUser);
    }

    // 2. Change Password Logic
    @Transactional
    public void changePassword(Long userId, PasswordChangeDto request) {
        log.info("Password change request for User ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Password change failed: Old password incorrect for User ID: {}", userId);
            throw new IllegalArgumentException("Incorrect old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for User ID: {}", userId);
    }

    // 3. Get Profile Logic
    public UserResponseDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return mapToDto(user);
    }

    private UserResponseDto mapToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .balance(user.getBalance())
                .build();
    }
}