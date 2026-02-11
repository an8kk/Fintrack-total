package com.fintrack.backend.controller;

import com.fintrack.backend.dto.MonthlyStatsDto;
import com.fintrack.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data/stats")
@RequiredArgsConstructor
public class StatsController {

    private final TransactionService transactionService;

    @GetMapping("/{userId}")
    public ResponseEntity<MonthlyStatsDto> getStats(
            @PathVariable Long userId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(transactionService.getMonthlyStats(userId, month, year));
    }
}
