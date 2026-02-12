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
        log.debug("Transaction details: {}", transaction);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Failed to save transaction: User ID {} not found", userId);
                    return new ResourceNotFoundException("User not found");
                });

        if (transaction.getDate() == null) {
            log.debug("Setting current timestamp for transaction");
            transaction.setDate(LocalDateTime.now());
        }

        if (transaction.getType() == Transaction.TransactionType.EXPENSE) {
            if (user.getBalance().compareTo(transaction.getAmount()) < 0) {
                log.warn("Transaction failed: Insufficient balance for user ID {}. Current: {}, Required: {}",
                        userId, user.getBalance(), transaction.getAmount());
                throw new InsufficientBalanceException("Insufficient balance");
            }
            user.setBalance(user.getBalance().subtract(transaction.getAmount()));

            if (transaction.getAmount().compareTo(new BigDecimal("500")) > 0) {
                log.info("High expense detected ($>500). Creating notification for user ID: {}", userId);
                Notification notif = Notification.builder()
                        .user(user)
                        .title("High Expense Alert")
                        .message("You just spent $" + transaction.getAmount() + " on " + transaction.getCategory())
                        .date(LocalDateTime.now())
                        .isRead(false)
                        .build();
                notificationRepository.save(notif);
            }
        } else {
            log.debug("Processing income transaction. Increasing balance.");
            user.setBalance(user.getBalance().add(transaction.getAmount()));
        }

        transaction.setUser(user);
        userRepository.save(user);
        Transaction saved = transactionRepository.save(transaction);

        log.info("Transaction saved successfully with ID: {}. New balance: {}", saved.getId(), user.getBalance());
        return saved;
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        log.info("Attempting to delete transaction ID: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.error("Failed to delete transaction: ID {} not found", transactionId);
                    return new ResourceNotFoundException("Transaction not found");
                });

        User user = transaction.getUser();
        log.debug("Reversing balance for user ID: {}", user.getId());

        if (transaction.getType() == Transaction.TransactionType.EXPENSE) {
            user.setBalance(user.getBalance().add(transaction.getAmount()));
        } else {
            user.setBalance(user.getBalance().subtract(transaction.getAmount()));
        }

        userRepository.save(user);
        transactionRepository.delete(transaction);

        log.info("Transaction {} deleted. Balance adjusted for user ID: {}. New balance: {}",
                transactionId, user.getId(), user.getBalance());
        log.info("Transaction {} deleted. Balance adjusted for user ID: {}. New balance: {}",
                transactionId, user.getId(), user.getBalance());
    }

    @Transactional
    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        log.info("Updating transaction ID: {}", id);

        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // 1. Revert old balance
        User user = existing.getUser();
        if (existing.getType() == Transaction.TransactionType.EXPENSE) {
            user.setBalance(user.getBalance().add(existing.getAmount()));
        } else {
            user.setBalance(user.getBalance().subtract(existing.getAmount()));
        }

        // 2. Update fields
        existing.setAmount(updatedTransaction.getAmount());
        existing.setCategory(updatedTransaction.getCategory());
        existing.setDescription(updatedTransaction.getDescription());
        existing.setDate(updatedTransaction.getDate());
        existing.setType(updatedTransaction.getType());

        // 3. Apply new balance
        if (existing.getType() == Transaction.TransactionType.EXPENSE) {
            if (user.getBalance().compareTo(existing.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance for update");
            }
            user.setBalance(user.getBalance().subtract(existing.getAmount()));
        } else {
            user.setBalance(user.getBalance().add(existing.getAmount()));
        }

        userRepository.save(user);
        return transactionRepository.save(existing);
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