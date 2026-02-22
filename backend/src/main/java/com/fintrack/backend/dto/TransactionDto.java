package com.fintrack.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDto {
    private Long id;
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDateTime date;
    private String type;
    private String currency;
    private Long userId;
}
