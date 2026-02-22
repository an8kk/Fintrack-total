package com.fintrack.backend.service;

import com.fintrack.backend.dto.MonthlyStatsDto;
import com.fintrack.backend.entity.Notification;
import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.exception.InsufficientBalanceException;
import com.fintrack.backend.exception.ResourceNotFoundException;
import com.fintrack.backend.repository.NotificationRepository;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public Transaction saveTransaction(Long userId, Transaction transaction) {
        log.info("Attempting to save transaction for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (transaction.getDate() == null) {
            transaction.setDate(LocalDateTime.now());
        }

        // Use Ground Truth balance for validation
        BigDecimal groundTruthBalance = transactionRepository.calculateBalanceByUserId(userId);
        if (transaction.getType() == Transaction.TransactionType.EXPENSE) {
            if (groundTruthBalance.compareTo(transaction.getAmount()) < 0) {
                log.warn("Transaction failed: Insufficient balance for user ID {}. Current: {}, Required: {}",
                        userId, groundTruthBalance, transaction.getAmount());
                throw new InsufficientBalanceException("Insufficient balance");
            }

            if (transaction.getAmount().compareTo(new BigDecimal("500")) > 0) {
                Notification notif = Notification.builder()
                        .user(user)
                        .title("High Expense Alert")
                        .message("You just spent $" + transaction.getAmount() + " on " + transaction.getCategory())
                        .date(LocalDateTime.now())
                        .isRead(false)
                        .build();
                notificationRepository.save(notif);
            }
        }

        transaction.setUser(user);
        Transaction saved = transactionRepository.save(transaction);

        // Final sync: recalculate after save and update user entity
        user.setBalance(transactionRepository.calculateBalanceByUserId(userId));
        userRepository.save(user);

        log.info("Transaction saved successfully with ID: {}. New balance: {}", saved.getId(), user.getBalance());
        return saved;
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        log.info("Attempting to delete transaction ID: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        User user = transaction.getUser();
        transactionRepository.delete(transaction);

        // Recalculate and sync balance
        user.setBalance(transactionRepository.calculateBalanceByUserId(user.getId()));
        userRepository.save(user);

        log.info("Transaction {} deleted. New balance: {}", transactionId, user.getBalance());
    }

    @Transactional
    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        log.info("Updating transaction ID: {}", id);

        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        User user = existing.getUser();

        // Update fields
        existing.setAmount(updatedTransaction.getAmount());
        existing.setCategory(updatedTransaction.getCategory());
        existing.setDescription(updatedTransaction.getDescription());
        existing.setDate(updatedTransaction.getDate());
        existing.setType(updatedTransaction.getType());

        // Save update
        Transaction saved = transactionRepository.save(existing);

        // Recalculate and sync balance
        BigDecimal finalBalance = transactionRepository.calculateBalanceByUserId(user.getId());
        user.setBalance(finalBalance);
        userRepository.save(user);

        return saved;
    }

    public List<Transaction> getTransactionsByUserId(Long userId) {
        log.debug("Fetching transactions for user ID: {}", userId);
        return transactionRepository.findByUserId(userId);
    }

    public MonthlyStatsDto getMonthlyStats(Long userId, int month, int year) {
        log.info("Calculating monthly stats for user ID: {} (Date: {}/{})", userId, month, year);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        log.debug("Filtering transactions between {} and {}", start, end);
        List<Transaction> filtered = transactionRepository.findByUserIdAndDateBetween(userId, start, end);
        log.debug("Found {} transactions in the specified range", filtered.size());

        BigDecimal totalIncome = filtered.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = filtered.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> categoryBreakdown = filtered.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        log.info("Monthly stats calculated. Income: {}, Expenses: {}", totalIncome, totalExpense);
        return new MonthlyStatsDto(totalIncome, totalExpense, categoryBreakdown);
    }
}