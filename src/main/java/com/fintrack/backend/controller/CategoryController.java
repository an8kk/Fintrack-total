package com.fintrack.backend.controller;

import com.fintrack.backend.entity.Category;
import com.fintrack.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/data/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Category>> getCategories(@PathVariable Long userId) {
        log.info("GET /api/data/categories/{}", userId);
        List<Category> cats = categoryService.getUserCategories(userId);
        log.debug("Returning {} categories for userId={}", cats.size(), userId);
        return ResponseEntity.ok(cats);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Category> addCategory(@PathVariable Long userId, @RequestBody Category category) {
        log.info("POST /api/data/categories/{} — name={}, type={}", userId, category.getName(), category.getType());
        return ResponseEntity.ok(categoryService.addCategory(userId, category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        log.info("PUT /api/data/categories/{} — name={}", id, category.getName());
        return ResponseEntity.ok(categoryService.updateCategory(id, category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        log.info("DELETE /api/data/categories/{}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }
}
