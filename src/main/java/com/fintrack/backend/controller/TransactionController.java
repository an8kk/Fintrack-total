package com.fintrack.backend.controller;

import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    // POST endpoint to create a transaction
    // Example usage: POST /api/transactions?userId=1
    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestParam Long userId, @RequestBody Transaction transaction) {
        try {
            Transaction created = transactionService.saveTransaction(userId, transaction);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET endpoint to retrieve all transactions for a specific user
    @GetMapping("/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        return ResponseEntity.ok(transactions);
    }
}