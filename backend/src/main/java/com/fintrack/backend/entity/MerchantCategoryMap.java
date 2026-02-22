package com.fintrack.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_category_map")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantCategoryMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keyword;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Source source = Source.SEED;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Source {
        SEED, AI_LEARNED
    }
}
