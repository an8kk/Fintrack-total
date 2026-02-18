package com.fintrack.backend.service;

import com.fintrack.backend.dto.GeminiDTOs;
import com.fintrack.backend.entity.MerchantCategoryMap;
import com.fintrack.backend.repository.MerchantCategoryMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategorizationService {

    private final MerchantCategoryMapRepository merchantCategoryMapRepository;
    private final RestTemplate restTemplate;

    @Value("${gemini.api-key}")
    private String apiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    /**
     * Hybrid categorization: Local keyword map first, then AI fallback.
     * Returns the category string (e.g. "Food", "Transport").
     */
    public String categorize(String merchantName) {
        if (merchantName == null || merchantName.isBlank()) {
            return "Uncategorized";
        }

        // Level 1: Local keyword map (fast, free)
        String localMatch = matchLocal(merchantName);
        if (localMatch != null) {
            log.debug("Local match for '{}': {}", merchantName, localMatch);
            return localMatch;
        }

        // Level 2: AI fallback (Gemini Flash)
        try {
            return categorizeWithAI(merchantName);
        } catch (Exception e) {
            log.error("AI categorization failed for '{}': {}", merchantName, e.getMessage());
            return "Uncategorized";
        }
    }

    private String matchLocal(String merchantName) {
        String lower = merchantName.toLowerCase();

        List<MerchantCategoryMap> matches = merchantCategoryMapRepository.findMatchingKeywords(merchantName);
        if (!matches.isEmpty()) {
            return matches.get(0).getCategory();
        }

        // Fallback: iterate all keywords for substring match (handles edge cases)
        List<MerchantCategoryMap> all = merchantCategoryMapRepository.findAll();
        for (MerchantCategoryMap map : all) {
            if (lower.contains(map.getKeyword().toLowerCase())) {
                return map.getCategory();
            }
        }

        return null;
    }

    private String categorizeWithAI(String merchantName) {
        String prompt = "Categorize this financial transaction merchant/description into exactly ONE category. " +
                "Choose from: Food, Transport, Shopping, Entertainment, Utilities, Health, Education, Salary, " +
                "Subscriptions, Transfers, Other. " +
                "Respond with ONLY a JSON object: {\"category\":\"CategoryName\",\"confidence\":0.95}\n\n" +
                "Merchant: " + merchantName;

        GeminiDTOs.GeminiRequest request = GeminiDTOs.GeminiRequest.builder()
                .contents(Collections.singletonList(GeminiDTOs.Content.builder()
                        .role("user")
                        .parts(Collections.singletonList(GeminiDTOs.Part.builder()
                                .text(prompt)
                                .build()))
                        .build()))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeminiDTOs.GeminiRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<GeminiDTOs.GeminiResponse> response = restTemplate.postForEntity(
                GEMINI_API_URL + apiKey, entity, GeminiDTOs.GeminiResponse.class);

        GeminiDTOs.GeminiResponse body = response.getBody();
        if (body != null && body.getCandidates() != null && !body.getCandidates().isEmpty()) {
            GeminiDTOs.Candidate candidate = body.getCandidates().get(0);
            if (candidate.getContent() != null && candidate.getContent().getParts() != null
                    && !candidate.getContent().getParts().isEmpty()) {
                String raw = candidate.getContent().getParts().get(0).getText().trim();
                return parseAIResponse(raw, merchantName);
            }
        }

        return "Uncategorized";
    }

    private String parseAIResponse(String raw, String merchantName) {
        try {
            // Strip markdown code fences if present
            String cleaned = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            // Simple JSON parsing
            String category = "Uncategorized";
            double confidence = 0;

            if (cleaned.contains("\"category\"")) {
                int catStart = cleaned.indexOf("\"category\"");
                int colonIdx = cleaned.indexOf(":", catStart);
                int firstQuote = cleaned.indexOf("\"", colonIdx + 1);
                int lastQuote = cleaned.indexOf("\"", firstQuote + 1);
                if (firstQuote > 0 && lastQuote > firstQuote) {
                    category = cleaned.substring(firstQuote + 1, lastQuote);
                }
            }

            if (cleaned.contains("\"confidence\"")) {
                int confStart = cleaned.indexOf("\"confidence\"");
                int colonIdx = cleaned.indexOf(":", confStart);
                String confStr = cleaned.substring(colonIdx + 1).replaceAll("[^0-9.]", "");
                if (!confStr.isEmpty()) {
                    confidence = Double.parseDouble(confStr);
                }
            }

            // Self-learning: if high confidence, save to local map
            if (confidence > 0.9 && !category.equals("Uncategorized") && !category.equals("Other")) {
                selfLearn(merchantName, category);
            }

            log.info("AI categorized '{}' as '{}' (confidence: {})", merchantName, category, confidence);
            return category;
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", raw, e);
            return "Uncategorized";
        }
    }

    private void selfLearn(String merchantName, String category) {
        // Extract a clean keyword from the merchant name
        String keyword = merchantName.trim().toLowerCase();

        // Only add if not already in the map
        if (!merchantCategoryMapRepository.existsByKeywordIgnoreCase(keyword)) {
            try {
                MerchantCategoryMap newMapping = MerchantCategoryMap.builder()
                        .keyword(keyword)
                        .category(category)
                        .source(MerchantCategoryMap.Source.AI_LEARNED)
                        .createdAt(LocalDateTime.now())
                        .build();
                merchantCategoryMapRepository.save(newMapping);
                log.info("Self-learned mapping: '{}' → '{}'", keyword, category);
            } catch (Exception e) {
                log.warn("Failed to save self-learned mapping for '{}': {}", keyword, e.getMessage());
            }
        }
    }

    /**
     * Seed default keyword mappings for common merchants.
     */
    public void seedDefaults() {
        String[][] defaults = {
                { "netflix", "Entertainment" },
                { "spotify", "Entertainment" },
                { "youtube", "Entertainment" },
                { "uber", "Transport" },
                { "bolt", "Transport" },
                { "yandex", "Transport" },
                { "glovo", "Food" },
                { "wolt", "Food" },
                { "mcdonalds", "Food" },
                { "kfc", "Food" },
                { "burger king", "Food" },
                { "starbucks", "Food" },
                { "amazon", "Shopping" },
                { "kaspi", "Shopping" },
                { "salary", "Salary" },
                { "payroll", "Salary" },
                { "rent", "Utilities" },
                { "electricity", "Utilities" },
                { "water", "Utilities" },
                { "pharmacy", "Health" },
                { "hospital", "Health" },
                { "clinic", "Health" },
        };

        for (String[] pair : defaults) {
            if (!merchantCategoryMapRepository.existsByKeywordIgnoreCase(pair[0])) {
                merchantCategoryMapRepository.save(MerchantCategoryMap.builder()
                        .keyword(pair[0])
                        .category(pair[1])
                        .source(MerchantCategoryMap.Source.SEED)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }
        log.info("Seeded {} default merchant category mappings", defaults.length);
    }
}
