package com.fintrack.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminCreateTransactionDto {
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Category is required")
    private String category;

    private String description = "";

    @NotBlank(message = "Type is required (INCOME or EXPENSE)")
    private String type;

    private String currency = "USD";
}
