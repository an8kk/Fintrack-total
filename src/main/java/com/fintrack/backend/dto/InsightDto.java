package com.fintrack.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightDto {

    private String title;
    private String description;
    private String suggestedAction;
    private InsightType type;
    private Double percentageChange;

    public enum InsightType {
        WARNING, INFO, TIP, ALERT
    }
}
