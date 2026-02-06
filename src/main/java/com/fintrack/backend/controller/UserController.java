package com.fintrack.backend.controller;

import com.fintrack.backend.dto.*;
import com.fintrack.backend.security.JwtUtils;
import com.fintrack.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils; 
    private final UserDetailsService userDetailsService; 

    // Endpoint 1: Update Profile 
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateDto updateDto) {
        try {
            // 1. Update the data in DB
            UserResponseDto userDto = userService.updateUser(id, updateDto);

            // 2. Load the updated user details (using the NEW email if it changed)
            UserDetails userDetails = userDetailsService.loadUserByUsername(userDto.getEmail());

            // 3. Generate a NEW Token
            String newToken = jwtUtils.generateToken(userDetails);

            // 4. Return the new token and updated info
            return ResponseEntity.ok(new AuthResponse(
                newToken, 
                userDto.getId(), 
                userDto.getUsername(), 
                userDto.getEmail()
            ));

        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint 2: Change Password -> Returns NEW TOKEN
    @PutMapping("/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody PasswordChangeDto passDto) {
        try {
            // 1. Change password in DB
            userService.changePassword(id, passDto);

            // 2. Get the user profile to find the email (needed to generate token)
            UserResponseDto userDto = userService.getUserProfile(id);
            
            // 3. Load UserDetails
            UserDetails userDetails = userDetailsService.loadUserByUsername(userDto.getEmail());

            // 4. Generate NEW Token
            String newToken = jwtUtils.generateToken(userDetails);

            // 5. Return new token
            return ResponseEntity.ok(new AuthResponse(
                newToken, 
                userDto.getId(), 
                userDto.getUsername(), 
                userDto.getEmail()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }
}