package com.fintrack.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    private String category;

    private String description;

    private LocalDateTime date;

    private String currency;

    @Column(unique = true)
    private String externalId;

    // Enum to distinguish Income vs Expense
    @Enumerated(EnumType.STRING)
    private TransactionType type; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // Prevent infinite recursion in JSON
    private User user;

    public enum TransactionType {
        INCOME, EXPENSE
    }
}