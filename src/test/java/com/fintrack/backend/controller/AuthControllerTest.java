package com.fintrack.backend.controller;

import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.UserRepository;
import com.fintrack.backend.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthController authController;

    // --- REGISTRATION TESTS ---

    @Test
    void register_NewUser_Success() {
        // Arrange
        User newUser = new User();
        newUser.setEmail("anuar@test.com");
        newUser.setPassword("rawPassword");
        newUser.setUsername("Anuar");

        when(userRepository.findByEmail("anuar@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        // Act
        ResponseEntity<?> response = authController.register(newUser);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody());
        
        // Verify password was hashed and balance set to zero
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(argThat(user -> 
            user.getPassword().equals("encodedPassword") && 
            user.getBalance().equals(BigDecimal.ZERO)
        ));
    }

    @Test
    void register_ExistingEmail_ReturnsBadRequest() {
        // Arrange
        User existingUser = new User();
        existingUser.setEmail("anuar@test.com");

        when(userRepository.findByEmail("anuar@test.com")).thenReturn(Optional.of(new User()));

        // Act
        ResponseEntity<?> response = authController.register(existingUser);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email already exists", response.getBody());
        verify(userRepository, never()).save(any());
    }

    // --- LOGIN TESTS ---

    @Test
    void login_Success_ReturnsToken() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("anuar@test.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("Anuar");
        mockUser.setEmail("anuar@test.com");

        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername("anuar@test.com")
                .password("encodedPass")
                .authorities("USER")
                .build();

        // Mock the AuthenticationManager (successful login)
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // Return value ignored in controller, just ensures no exception thrown

        // Mock UserDetails loading
        when(userDetailsService.loadUserByUsername("anuar@test.com")).thenReturn(mockUserDetails);

        // Mock Token Generation
        when(jwtUtils.generateToken(mockUserDetails)).thenReturn("fake-jwt-token");

        // Mock User Retrieval for ID
        when(userRepository.findByEmail("anuar@test.com")).thenReturn(Optional.of(mockUser));

        // Act
        ResponseEntity<?> response = authController.login(loginRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof AuthResponse);
        
        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertEquals("fake-jwt-token", authResponse.getToken());
        assertEquals(1L, authResponse.getUserId());
        assertEquals("Anuar", authResponse.getUsername());
    }

    @Test
    void login_InvalidCredentials_ThrowsException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrong@test.com");
        loginRequest.setPassword("wrongPass");

        // Simulate Auth Failure
        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Bad Credentials"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authController.login(loginRequest);
        });
    }
}