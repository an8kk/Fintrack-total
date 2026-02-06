package com.fintrack.backend.controller;

import com.fintrack.backend.dto.MonthlyStatsDto;
import com.fintrack.backend.entity.Category;
import com.fintrack.backend.entity.Notification;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.NotificationRepository;
import com.fintrack.backend.repository.UserRepository;
import com.fintrack.backend.service.CategoryService;
import com.fintrack.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Categories
    @GetMapping("/categories/{userId}")
    public ResponseEntity<List<Category>> getCategories(@PathVariable Long userId) {
        return ResponseEntity.ok(categoryService.getUserCategories(userId));
    }

    @PostMapping("/categories/{userId}")
    public ResponseEntity<Category> addCategory(@PathVariable Long userId, @RequestBody Category category) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(categoryService.addCategory(userId, category, user));
    }

    // Notifications
    @GetMapping("/notifications/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByDateDesc(userId));
    }

    // Stats
    @GetMapping("/stats/{userId}")
    public ResponseEntity<MonthlyStatsDto> getStats(
            @PathVariable Long userId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(transactionService.getMonthlyStats(userId, month, year));
    }

    // DELETE CATEGORY
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    // UPDATE CATEGORY
    @PutMapping("/categories/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return ResponseEntity.ok(categoryService.updateCategory(id, category));
    }
    
   @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok("Transaction deleted and balance updated"); 
    }
}