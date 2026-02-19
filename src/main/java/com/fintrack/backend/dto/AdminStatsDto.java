package com.fintrack.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminStatsDto {
    private long totalUsers;
    private long totalTransactions;
    private BigDecimal totalVolume;
    private long activeToday;
    private long newUsersThisWeek;
}
