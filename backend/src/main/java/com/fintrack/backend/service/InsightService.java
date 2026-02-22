package com.fintrack.backend.service;

import com.fintrack.backend.dto.InsightDto;
import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

        private final TransactionRepository transactionRepository;

        /**
         * "Pulse" insight engine: Compares last 7 days against the previous 30 days.
         * Falls back to all-time data if no recent transactions exist.
         */
        public List<InsightDto> generateInsights(Long userId) {
                log.info("Generating spending insights for userId={}", userId);
                List<InsightDto> insights = new ArrayList<>();

                LocalDateTime now = LocalDateTime.now();

                // Last 7 days vs previous 30 days
                LocalDateTime weekStart = now.minusDays(7);
                LocalDateTime prevPeriodStart = now.minusDays(37);

                List<Transaction> recentTxns = transactionRepository.findByUserIdAndDateBetween(userId, weekStart, now);
                List<Transaction> prevTxns = transactionRepository.findByUserIdAndDateBetween(userId, prevPeriodStart,
                                weekStart);

                // Fallback: if no comparative data, generate from ALL transactions
                if (recentTxns.isEmpty() && prevTxns.isEmpty()) {
                        return generateFallbackInsights(userId);
                }

                // Category breakdown: recent week vs previous period weekly average
                Map<String, BigDecimal> weekByCategory = groupByCategory(recentTxns);
                Map<String, BigDecimal> prevByCategory = groupByCategory(prevTxns);

                double weeksInPrevPeriod = 30.0 / 7.0;

                Set<String> allCategories = new HashSet<>();
                allCategories.addAll(weekByCategory.keySet());
                allCategories.addAll(prevByCategory.keySet());

                for (String category : allCategories) {
                        BigDecimal weekAmount = weekByCategory.getOrDefault(category, BigDecimal.ZERO);
                        BigDecimal prevWeeklyAvg = prevByCategory.getOrDefault(category, BigDecimal.ZERO)
                                        .divide(BigDecimal.valueOf(weeksInPrevPeriod), 2, RoundingMode.HALF_UP);

                        if (prevWeeklyAvg.compareTo(BigDecimal.ZERO) == 0) {
                                if (weekAmount.compareTo(new BigDecimal("500")) > 0) {
                                        insights.add(InsightDto.builder()
                                                        .title("New Spending: " + category)
                                                        .description(String.format(
                                                                        "You spent %s on %s this week — a new category for you.",
                                                                        formatAmount(weekAmount), category))
                                                        .suggestedAction(
                                                                        "Track this category to see if it becomes a habit.")
                                                        .type(InsightDto.InsightType.INFO)
                                                        .percentageChange(100.0)
                                                        .build());
                                }
                                continue;
                        }

                        double percentChange = weekAmount.subtract(prevWeeklyAvg)
                                        .divide(prevWeeklyAvg, 4, RoundingMode.HALF_UP)
                                        .doubleValue() * 100;

                        if (percentChange > 20) {
                                insights.add(InsightDto.builder()
                                                .title(category + " Spending Up")
                                                .description(String.format(
                                                                "You spent %.0f%% more on %s this week (%s) vs your average (%s/week).",
                                                                percentChange, category, formatAmount(weekAmount),
                                                                formatAmount(prevWeeklyAvg)))
                                                .suggestedAction("Consider setting a weekly budget limit for "
                                                                + category + ".")
                                                .type(percentChange > 50 ? InsightDto.InsightType.WARNING
                                                                : InsightDto.InsightType.ALERT)
                                                .percentageChange(percentChange)
                                                .build());
                        } else if (percentChange < -20) {
                                insights.add(InsightDto.builder()
                                                .title(category + " Savings!")
                                                .description(String.format(
                                                                "Great job! You spent %.0f%% less on %s this week.",
                                                                Math.abs(percentChange), category))
                                                .suggestedAction("Keep it up! You could save "
                                                                + formatAmount(prevWeeklyAvg.subtract(weekAmount))
                                                                + " per week at this rate.")
                                                .type(InsightDto.InsightType.TIP)
                                                .percentageChange(percentChange)
                                                .build());
                        }
                }

                // Total spending insight
                BigDecimal totalWeek = recentTxns.stream()
                                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalPrevAvg = prevTxns.stream()
                                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(weeksInPrevPeriod), 2, RoundingMode.HALF_UP);

                if (totalPrevAvg.compareTo(BigDecimal.ZERO) > 0) {
                        double totalChange = totalWeek.subtract(totalPrevAvg)
                                        .divide(totalPrevAvg, 4, RoundingMode.HALF_UP)
                                        .doubleValue() * 100;

                        if (Math.abs(totalChange) > 10) {
                                insights.add(InsightDto.builder()
                                                .title("Weekly Overview")
                                                .description(String.format(
                                                                "Total spending this week: %s (%s%.0f%% vs avg of %s/week).",
                                                                formatAmount(totalWeek),
                                                                totalChange > 0 ? "+" : "",
                                                                totalChange,
                                                                formatAmount(totalPrevAvg)))
                                                .suggestedAction(totalChange > 0
                                                                ? "Review your top spending categories to find potential savings."
                                                                : "You're trending below your usual spending — great discipline!")
                                                .type(totalChange > 0 ? InsightDto.InsightType.ALERT
                                                                : InsightDto.InsightType.TIP)
                                                .percentageChange(totalChange)
                                                .build());
                        }
                }

                // If comparative analysis found nothing interesting, fallback
                if (insights.isEmpty()) {
                        return generateFallbackInsights(userId);
                }

                // Sort by absolute percentage change (most impactful first)
                insights.sort((a, b) -> Double.compare(
                                Math.abs(b.getPercentageChange() != null ? b.getPercentageChange() : 0),
                                Math.abs(a.getPercentageChange() != null ? a.getPercentageChange() : 0)));

                log.debug("Generated {} insights for userId={}", insights.size(), userId);
                return insights;
        }

        /**
         * Fallback: generates basic category breakdown insights from ALL user
         * transactions.
         */
        private List<InsightDto> generateFallbackInsights(Long userId) {
                log.debug("Using fallback insights for userId={}", userId);
                List<InsightDto> insights = new ArrayList<>();

                List<Transaction> allTxns = transactionRepository.findByUserId(userId);
                if (allTxns.isEmpty()) {
                        insights.add(InsightDto.builder()
                                        .title("No Data Yet")
                                        .description("Start adding transactions to get personalized spending insights.")
                                        .suggestedAction(
                                                        "Add your first transaction or connect your bank via Salt Edge.")
                                        .type(InsightDto.InsightType.INFO)
                                        .percentageChange(0.0)
                                        .build());
                        return insights;
                }

                // Top spending categories
                Map<String, BigDecimal> byCategory = groupByCategory(allTxns);
                BigDecimal totalSpending = byCategory.values().stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalSpending.compareTo(BigDecimal.ZERO) > 0) {
                        // Sort categories by amount descending
                        byCategory.entrySet().stream()
                                        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                                        .limit(3)
                                        .forEach(entry -> {
                                                double pct = entry.getValue()
                                                                .divide(totalSpending, 4, RoundingMode.HALF_UP)
                                                                .doubleValue() * 100;
                                                insights.add(InsightDto.builder()
                                                                .title("Top Category: " + entry.getKey())
                                                                .description(String.format(
                                                                                "%s accounts for %.0f%% of your total spending (%s).",
                                                                                entry.getKey(), pct,
                                                                                formatAmount(entry.getValue())))
                                                                .suggestedAction("Set a budget limit for "
                                                                                + entry.getKey()
                                                                                + " to control spending.")
                                                                .type(pct > 40 ? InsightDto.InsightType.WARNING
                                                                                : InsightDto.InsightType.INFO)
                                                                .percentageChange(pct)
                                                                .build());
                                        });
                }

                // Income vs expense summary
                BigDecimal totalIncome = allTxns.stream()
                                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalIncome.compareTo(BigDecimal.ZERO) > 0 && totalSpending.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal savingsRate = totalIncome.subtract(totalSpending)
                                        .divide(totalIncome, 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                        insights.add(InsightDto.builder()
                                        .title("Savings Rate")
                                        .description(String.format(
                                                        "Your savings rate is %.0f%% (income: %s, spending: %s).",
                                                        savingsRate.doubleValue(), formatAmount(totalIncome),
                                                        formatAmount(totalSpending)))
                                        .suggestedAction(savingsRate.doubleValue() > 20
                                                        ? "Great savings rate! Keep it up."
                                                        : "Try to increase your savings rate to at least 20%.")
                                        .type(savingsRate.doubleValue() > 20 ? InsightDto.InsightType.TIP
                                                        : InsightDto.InsightType.WARNING)
                                        .percentageChange(savingsRate.doubleValue())
                                        .build());
                }

                return insights;
        }

        private Map<String, BigDecimal> groupByCategory(List<Transaction> transactions) {
                return transactions.stream()
                                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                                .filter(t -> t.getCategory() != null)
                                .collect(Collectors.groupingBy(
                                                Transaction::getCategory,
                                                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount,
                                                                BigDecimal::add)));
        }

        private String formatAmount(BigDecimal amount) {
                return amount.setScale(0, RoundingMode.HALF_UP).toString();
        }
}
