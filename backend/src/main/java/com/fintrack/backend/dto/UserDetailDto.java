package com.fintrack.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class UserDetailDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean isBlocked;
    private BigDecimal balance;
    private long transactionCount;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private LocalDateTime createdAt;
}
