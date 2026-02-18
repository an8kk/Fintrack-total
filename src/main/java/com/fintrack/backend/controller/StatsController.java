package com.fintrack.backend.controller;

import com.fintrack.backend.dto.InsightDto;
import com.fintrack.backend.dto.MonthlyStatsDto;
import com.fintrack.backend.service.InsightService;
import com.fintrack.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/data/stats")
@RequiredArgsConstructor
public class StatsController {

    private final TransactionService transactionService;
    private final com.fintrack.backend.service.GeminiService geminiService;
    private final InsightService insightService;

    @GetMapping("/{userId}")
    public ResponseEntity<MonthlyStatsDto> getStats(
            @PathVariable Long userId,
            @RequestParam int month,
            @RequestParam int year) {
        log.info("GET /api/data/stats/{} — month={}, year={}", userId, month, year);
        return ResponseEntity.ok(transactionService.getMonthlyStats(userId, month, year));
    }

    @GetMapping("/{userId}/ai-analysis")
    public ResponseEntity<Map<String, String>> getAiAnalysis(@PathVariable Long userId) {
        log.info("GET /api/data/stats/{}/ai-analysis", userId);
        List<com.fintrack.backend.entity.Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        log.debug("Analyzing {} transactions for userId={}", transactions.size(), userId);
        String analysis = geminiService.analyzeSpending(transactions);
        return ResponseEntity.ok(java.util.Map.of("analysis", analysis));
    }

    @GetMapping("/{userId}/insights")
    public ResponseEntity<List<InsightDto>> getInsights(@PathVariable Long userId) {
        log.info("GET /api/data/stats/{}/insights", userId);
        List<InsightDto> insights = insightService.generateInsights(userId);
        log.debug("Generated {} insights for userId={}", insights.size(), userId);
        return ResponseEntity.ok(insights);
    }
}
