package com.fintrack.backend.service;

import com.fintrack.backend.dto.MonthlyStatsDto;
import com.fintrack.backend.entity.Notification;
import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.NotificationRepository;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (transaction.getDate() == null) transaction.setDate(LocalDateTime.now());

        if (transaction.getType() == Transaction.TransactionType.EXPENSE) {
            if (user.getBalance().compareTo(transaction.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
            user.setBalance(user.getBalance().subtract(transaction.getAmount()));
            
            // If expense > 500, trigger alert
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
            // --------------------------
        } else {
            user.setBalance(user.getBalance().add(transaction.getAmount()));
        }

        transaction.setUser(user);
        userRepository.save(user);
        return transactionRepository.save(transaction);
    }
    @Transactional
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        User user = transaction.getUser();

        if (transaction.getType() == Transaction.TransactionType.EXPENSE) {
            user.setBalance(user.getBalance().add(transaction.getAmount()));
        } else {
            user.setBalance(user.getBalance().subtract(transaction.getAmount()));
        }

        userRepository.save(user);
        transactionRepository.delete(transaction);
        
        log.info("Transaction {} deleted. Balance refunded for user {}", transactionId, user.getId());
    }

    public MonthlyStatsDto getMonthlyStats(Long userId, int month, int year) {
        List<Transaction> all = transactionRepository.findByUserId(userId);
        
        // Filter by Month/Year
        List<Transaction> filtered = all.stream()
                .filter(t -> t.getDate().getMonthValue() == month && t.getDate().getYear() == year)
                .toList();

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
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        return new MonthlyStatsDto(totalIncome, totalExpense, categoryBreakdown);
    }
}