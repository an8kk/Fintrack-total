package com.fintrack.backend.service;

import com.fintrack.backend.entity.Category;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.exception.ResourceNotFoundException;
import com.fintrack.backend.repository.CategoryRepository;
import com.fintrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public void createDefaultCategories(User user) {
        log.info("Creating default categories for userId={}", user.getId());
        List<Category> defaults = List.of(
                Category.builder().name("Food").type("EXPENSE").icon("fastfood").color("0xFF2196F3")
                        .budgetLimit(new BigDecimal("1500")).user(user).build(),
                Category.builder().name("Transport").type("EXPENSE").icon("directions_bus").color("0xFF9C27B0")
                        .budgetLimit(new BigDecimal("1000")).user(user).build(),
                Category.builder().name("Shopping").type("EXPENSE").icon("shopping_bag").color("0xFFE91E63")
                        .budgetLimit(new BigDecimal("800")).user(user).build(),
                Category.builder().name("Salary").type("INCOME").icon("attach_money").color("0xFF4CAF50")
                        .budgetLimit(BigDecimal.ZERO).user(user).build());
        categoryRepository.saveAll(defaults);
        log.debug("Created {} default categories for userId={}", defaults.size(), user.getId());
    }

    public List<Category> getUserCategories(Long userId) {
        log.debug("Fetching categories for userId={}", userId);
        return categoryRepository.findByUserId(userId);
    }

    public Category addCategory(Long userId, Category category) {
        log.info("Adding category '{}' for userId={}", category.getName(), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        category.setUser(user);
        Category saved = categoryRepository.save(category);
        log.debug("Category saved: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    public void deleteCategory(Long id) {
        log.info("Deleting category id={}", id);
        if (!categoryRepository.existsById(id)) {
            log.warn("Category not found for deletion: id={}", id);
            throw new ResourceNotFoundException("Category not found with ID: " + id);
        }
        categoryRepository.deleteById(id);
    }

    public Category updateCategory(Long id, Category updated) {
        log.info("Updating category id={} — name={}", id, updated.getName());
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        cat.setName(updated.getName());
        cat.setIcon(updated.getIcon());
        cat.setColor(updated.getColor());
        cat.setBudgetLimit(updated.getBudgetLimit());
        return categoryRepository.save(cat);
    }
}