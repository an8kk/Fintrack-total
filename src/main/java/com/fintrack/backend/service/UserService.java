package com.fintrack.backend.service;

import com.fintrack.backend.dto.PasswordChangeDto;
import com.fintrack.backend.dto.UserResponseDto;
import com.fintrack.backend.dto.UserUpdateDto;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
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
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateDto request) {
        log.info("Updating profile for User ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

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

    @Transactional
    public UserResponseDto updateUserByAdmin(Long userId, UserUpdateDto request) {
        log.info("Admin updating profile for User ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }

        User savedUser = userRepository.save(user);
        log.info("User profile updated by Admin successfully");

        return mapToDto(savedUser);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeDto request) {
        log.info("Password change request for User ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Password change failed: Old password incorrect for User ID: {}", userId);
            throw new IllegalArgumentException("Incorrect old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for User ID: {}", userId);
    }

    public java.util.List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public UserResponseDto toggleUserBlockStatus(Long userId) {
        log.info("Toggling block status for User ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setBlocked(!user.isBlocked());
        User saved = userRepository.save(user);
        return mapToDto(saved);
    }

    public UserResponseDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return mapToDto(user);
    }

    private UserResponseDto mapToDto(User user) {
        BigDecimal calculatedBalance = transactionRepository.calculateBalanceByUserId(user.getId());
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .balance(calculatedBalance)
                .role(user.getRole().name())
                .isBlocked(user.isBlocked())
                .build();
    }
}