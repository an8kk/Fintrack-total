package com.fintrack.backend.controller;

import com.fintrack.backend.dto.InsightDto;
import com.fintrack.backend.dto.MonthlyStatsDto;
import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.service.InsightService;
import com.fintrack.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
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
    private final TransactionRepository transactionRepository;

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
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
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

    // ─── Granular AI Insights ──────────────────────────────────

    @PostMapping("/{userId}/ai/transaction")
    public ResponseEntity<Map<String, String>> getTransactionInsight(
            @PathVariable Long userId,
            @RequestBody Map<String, Long> body) {
        Long txId = body.get("transactionId");
        log.info("POST /api/data/stats/{}/ai/transaction — txId={}", userId, txId);
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(
                        () -> new com.fintrack.backend.exception.ResourceNotFoundException("Transaction not found"));
        String insight = geminiService.analyzeTransaction(tx);
        return ResponseEntity.ok(Map.of("insight", insight));
    }

    @PostMapping("/{userId}/ai/category")
    public ResponseEntity<Map<String, String>> getCategoryInsight(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String category = body.get("category");
        log.info("POST /api/data/stats/{}/ai/category — category={}", userId, category);
        List<Transaction> txns = transactionRepository.findByUserIdAndCategory(userId, category);
        String insight = geminiService.analyzeCategory(category, txns);
        return ResponseEntity.ok(Map.of("insight", insight));
    }

    @PostMapping("/{userId}/ai/period")
    public ResponseEntity<Map<String, String>> getPeriodInsight(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String period = body.get("period"); // day, week, month, year
        String dateStr = body.get("date"); // yyyy-MM-dd
        log.info("POST /api/data/stats/{}/ai/period — period={}, date={}", userId, period, dateStr);

        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDateTime start;
        LocalDateTime end;
        String label;

        switch (period) {
            case "day":
                start = date.atStartOfDay();
                end = date.atTime(23, 59, 59);
                label = date.toString();
                break;
            case "week":
                LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEnd = weekStart.plusDays(6);
                start = weekStart.atStartOfDay();
                end = weekEnd.atTime(23, 59, 59);
                label = weekStart + " – " + weekEnd;
                break;
            case "month":
                YearMonth ym = YearMonth.from(date);
                start = ym.atDay(1).atStartOfDay();
                end = ym.atEndOfMonth().atTime(23, 59, 59);
                label = ym.toString();
                break;
            case "year":
                start = LocalDate.of(date.getYear(), 1, 1).atStartOfDay();
                end = LocalDate.of(date.getYear(), 12, 31).atTime(23, 59, 59);
                label = String.valueOf(date.getYear());
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of("insight", "Unknown period: " + period));
        }

        List<Transaction> txns = transactionRepository.findByUserIdAndDateBetween(userId, start, end);
        String insight = geminiService.analyzePeriod(period, label, txns);
        return ResponseEntity.ok(Map.of("insight", insight));
    }
}
