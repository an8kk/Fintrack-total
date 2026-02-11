package com.fintrack.backend.service;

import com.fintrack.backend.entity.Category;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.exception.ResourceNotFoundException;
import com.fintrack.backend.repository.CategoryRepository;
import com.fintrack.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    void createDefaultCategories() {
        categoryService.createDefaultCategories(testUser);

        verify(categoryRepository).saveAll(argThat(iterable -> {
            int count = 0;
            for (Object ignored : iterable)
                count++;
            return count == 4;
        }));
    }

    @Test
    void getUserCategories() {
        Category cat = Category.builder().id(1L).name("Food").build();
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of(cat));

        List<Category> result = categoryService.getUserCategories(1L);

        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).getName());
    }

    @Test
    void addCategory_success() {
        Category cat = Category.builder().name("Test").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.addCategory(1L, cat);

        assertEquals(testUser, result.getUser());
        verify(categoryRepository).save(cat);
    }

    @Test
    void addCategory_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.addCategory(99L, new Category()));
    }

    @Test
    void deleteCategory_success() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_notFound_throws() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(99L));
    }

    @Test
    void updateCategory_success() {
        Category existing = Category.builder()
                .id(1L).name("Old").icon("home").color("0xFF000000")
                .budgetLimit(new BigDecimal("100")).build();
        Category updated = Category.builder()
                .name("New").icon("work").color("0xFFFF0000")
                .budgetLimit(new BigDecimal("500")).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.updateCategory(1L, updated);

        assertEquals("New", result.getName());
        assertEquals("work", result.getIcon());
        assertEquals(new BigDecimal("500"), result.getBudgetLimit());
    }

    @Test
    void updateCategory_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(99L, new Category()));
    }
}
