package com.fintrack.backend.service;

import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j 
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Transaction saveTransaction(Long userId, Transaction transaction) {
        log.info("Starting transaction processing for User ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Set current date if missing
        if (transaction.getDate() == null) {
            transaction.setDate(LocalDateTime.now());
        }

        // Logic: Check balance if it is an EXPENSE
        if (transaction.getType() == Transaction.TransactionType.EXPENSE) {
            if (user.getBalance().compareTo(transaction.getAmount()) < 0) {
                log.error("Insufficient balance for User ID: {}. Current: {}, Required: {}", 
                        userId, user.getBalance(), transaction.getAmount());
                throw new RuntimeException("Insufficient balance to complete this transaction.");
            }
            user.setBalance(user.getBalance().subtract(transaction.getAmount()));
        } else {
            user.setBalance(user.getBalance().add(transaction.getAmount()));
        }

        // Link transaction to user
        transaction.setUser(user);
        
        // Save logic
        Transaction savedTransaction = transactionRepository.save(transaction);
        userRepository.save(user); 

        log.info("Transaction completed successfully. New Balance: {}", user.getBalance());
        
        return savedTransaction;
    }
}