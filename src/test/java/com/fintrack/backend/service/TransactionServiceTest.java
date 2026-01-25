package com.fintrack.backend.service;

import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
import com.fintrack.backend.service.TransactionService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    // Verify income increases user balance
    @Test
    void saveTransaction_Income_IncreasesBalance() {
        // Arrange
        User user = new User(1L, "Anuar", "test@mail.com", new BigDecimal("100.00"), null);
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("50.00"))
                .type(Transaction.TransactionType.INCOME)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Transaction result = transactionService.saveTransaction(1L, transaction);

        // Assert
        assertEquals(new BigDecimal("150.00"), user.getBalance()); // 100 + 50
        verify(userRepository).save(user);
    }

    // Verify expense decreases user balance when funds are sufficient
    @Test
    void saveTransaction_Expense_SufficientBalance_DecreasesBalance() {
        // Arrange
        User user = new User(1L, "Anuar", "test@mail.com", new BigDecimal("100.00"), null);
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("40.00"))
                .type(Transaction.TransactionType.EXPENSE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        transactionService.saveTransaction(1L, transaction);

        // Assert
        assertEquals(new BigDecimal("60.00"), user.getBalance()); // 100 - 40
    }

    // Verify expense throws exception when funds are insufficient
    @Test
    void saveTransaction_Expense_InsufficientBalance_ThrowsException() {
        // Arrange
        User user = new User(1L, "Anuar", "test@mail.com", new BigDecimal("10.00"), null);
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("50.00"))
                .type(Transaction.TransactionType.EXPENSE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.saveTransaction(1L, transaction);
        });

        assertEquals("Insufficient balance to complete this transaction.", exception.getMessage());
        verify(transactionRepository, never()).save(any()); // Ensure nothing was saved
    }

    // Verify Exception is thrown if User ID does not exist
    @Test
    void saveTransaction_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        Transaction transaction = new Transaction();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.saveTransaction(99L, transaction);
        });

        assertEquals("User not found", exception.getMessage());
    }

    // Verify current date is set automatically if null
    @Test
    void saveTransaction_SetsDateIfNull() {
        // Arrange
        User user = new User(1L, "Anuar", "test@mail.com", BigDecimal.TEN, null);
        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.ONE)
                .type(Transaction.TransactionType.INCOME)
                .date(null) // Date is missing
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Transaction result = transactionService.saveTransaction(1L, transaction);

        // Assert
        assertNotNull(result.getDate()); // Service should have added LocalDateTime.now()
    }
}