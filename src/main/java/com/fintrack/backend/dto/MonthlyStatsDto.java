package com.fintrack.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
public class MonthlyStatsDto {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private Map<String, BigDecimal> categoryBreakdown;
}