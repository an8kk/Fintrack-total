package com.fintrack.backend.service;

import com.fintrack.backend.dto.PasswordChangeDto;
import com.fintrack.backend.dto.UserResponseDto;
import com.fintrack.backend.dto.UserUpdateDto;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.exception.ResourceNotFoundException;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("Test");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedOld");
        testUser.setBalance(new BigDecimal("500"));
    }

    @Test
    void updateUser_success() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setUsername("NewName");
        dto.setEmail("new@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(transactionRepository.calculateBalanceByUserId(1L)).thenReturn(new BigDecimal("500"));

        UserResponseDto result = userService.updateUser(1L, dto);

        assertEquals("NewName", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
    }

    @Test
    void updateUser_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(99L, new UserUpdateDto()));
    }

    @Test
    void changePassword_success() {
        PasswordChangeDto dto = new PasswordChangeDto();
        dto.setOldPassword("oldPass");
        dto.setNewPassword("newPass123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "encodedOld")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNew");

        userService.changePassword(1L, dto);

        assertEquals("encodedNew", testUser.getPassword());
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_wrongOldPassword_throws() {
        PasswordChangeDto dto = new PasswordChangeDto();
        dto.setOldPassword("wrongOld");
        dto.setNewPassword("newPass123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOld", "encodedOld")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(1L, dto));
    }

    @Test
    void getUserProfile_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.calculateBalanceByUserId(1L)).thenReturn(new BigDecimal("500"));

        UserResponseDto result = userService.getUserProfile(1L);

        assertEquals("Test", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(new BigDecimal("500"), result.getBalance());
    }

    @Test
    void getUserProfile_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserProfile(99L));
    }
}
