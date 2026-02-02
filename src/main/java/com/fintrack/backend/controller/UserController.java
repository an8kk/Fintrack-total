package com.fintrack.backend.controller;

import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedData) {
        log.info("Request to update user ID: {}", id);
        
        return userRepository.findById(id).map(user -> {
            if (updatedData.getUsername() != null && !updatedData.getUsername().isEmpty()) {
                user.setUsername(updatedData.getUsername());
            }
            if (updatedData.getEmail() != null && !updatedData.getEmail().isEmpty()) {
                user.setEmail(updatedData.getEmail());
            }
            
            User savedUser = userRepository.save(user);
            log.info("User ID {} updated successfully", id);
            return ResponseEntity.ok(savedUser);
        }).orElse(ResponseEntity.notFound().build());
    }
}