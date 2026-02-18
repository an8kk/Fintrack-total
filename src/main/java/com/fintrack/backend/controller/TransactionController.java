package com.fintrack.backend.controller;

import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestParam Long userId,
            @RequestBody Transaction transaction) {
        log.info("POST /api/transactions — userId={}, category={}, amount={}", userId, transaction.getCategory(),
                transaction.getAmount());
        Transaction created = transactionService.saveTransaction(userId, transaction);
        log.debug("Transaction created: id={}", created.getId());
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable Long userId) {
        log.info("GET /api/transactions/{}", userId);
        List<Transaction> txs = transactionService.getTransactionsByUserId(userId);
        log.debug("Returning {} transactions for userId={}", txs.size(), userId);
        return ResponseEntity.ok(txs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        log.info("DELETE /api/transactions/{}", id);
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable Long id, @RequestBody Transaction transaction) {
        log.info("PUT /api/transactions/{} — category={}, amount={}", id, transaction.getCategory(),
                transaction.getAmount());
        return ResponseEntity.ok(transactionService.updateTransaction(id, transaction));
    }
}