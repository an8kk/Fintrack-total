package com.fintrack.backend.service;

import com.fintrack.backend.dto.MonthlyStatsDto;
import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.exception.InsufficientBalanceException;
import com.fintrack.backend.exception.ResourceNotFoundException;
import com.fintrack.backend.repository.NotificationRepository;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setBalance(new BigDecimal("1000"));
    }

    @Test
    void saveTransaction_income_addsToBalance() {
        Transaction tx = Transaction.builder()
                .amount(new BigDecimal("500"))
                .type(Transaction.TransactionType.INCOME)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.saveTransaction(1L, tx);

        assertEquals(new BigDecimal("1500"), testUser.getBalance());
        assertNotNull(result);
        verify(userRepository).save(testUser);
    }

    @Test
    void saveTransaction_expense_subtractsFromBalance() {
        Transaction tx = Transaction.builder()
                .amount(new BigDecimal("300"))
                .type(Transaction.TransactionType.EXPENSE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transactionService.saveTransaction(1L, tx);

        assertEquals(new BigDecimal("700"), testUser.getBalance());
    }

    @Test
    void saveTransaction_expense_insufficientBalance_throws() {
        Transaction tx = Transaction.builder()
                .amount(new BigDecimal("2000"))
                .type(Transaction.TransactionType.EXPENSE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.saveTransaction(1L, tx));
    }

    @Test
    void saveTransaction_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.saveTransaction(99L, new Transaction()));
    }

    @Test
    void deleteTransaction_refundsExpense() {
        Transaction tx = Transaction.builder()
                .id(10L)
                .amount(new BigDecimal("200"))
                .type(Transaction.TransactionType.EXPENSE)
                .user(testUser)
                .build();

        when(transactionRepository.findById(10L)).thenReturn(Optional.of(tx));

        transactionService.deleteTransaction(10L);

        assertEquals(new BigDecimal("1200"), testUser.getBalance());
        verify(transactionRepository).delete(tx);
    }

    @Test
    void getMonthlyStats_calculatesCorrectly() {
        Transaction income = Transaction.builder()
                .amount(new BigDecimal("1000"))
                .type(Transaction.TransactionType.INCOME)
                .category("Salary")
                .build();
        Transaction expense = Transaction.builder()
                .amount(new BigDecimal("300"))
                .type(Transaction.TransactionType.EXPENSE)
                .category("Food")
                .build();

        when(transactionRepository.findByUserIdAndDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(income, expense));

        MonthlyStatsDto stats = transactionService.getMonthlyStats(1L, 1, 2026);

        assertEquals(new BigDecimal("1000"), stats.getTotalIncome());
        assertEquals(new BigDecimal("300"), stats.getTotalExpense());
        assertEquals(new BigDecimal("300"), stats.getCategoryBreakdown().get("Food"));
    }

    @Test
    void getTransactionsByUserId() {
        when(transactionRepository.findByUserId(1L)).thenReturn(List.of());

        List<Transaction> result = transactionService.getTransactionsByUserId(1L);

        assertTrue(result.isEmpty());
        verify(transactionRepository).findByUserId(1L);
    }
}
