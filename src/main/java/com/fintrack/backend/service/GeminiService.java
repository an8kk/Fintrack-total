package com.fintrack.backend.service;

import com.fintrack.backend.dto.GeminiDTOs;
import com.fintrack.backend.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

        private final RestTemplate restTemplate;

        @Value("${gemini.api-key}")
        private String apiKey;

        private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=";

        public String analyzeSpending(List<Transaction> transactions) {
                if (transactions == null || transactions.isEmpty()) {
                        return "No data to analyze yet.";
                }

                // Preprocessing: Format last 15 transactions
                String formattedTransactions = transactions.stream()
                                .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate())) // Sort by date descending
                                .limit(15)
                                .map(this::formatTransaction)
                                .collect(Collectors.joining("\n"));

                // Prompt is constructed within the request object directly

                GeminiDTOs.GeminiRequest request = GeminiDTOs.GeminiRequest.builder()
                                .systemInstruction(GeminiDTOs.Content.builder()
                                                .parts(Collections.singletonList(GeminiDTOs.Part.builder()
                                                                .text("You are a senior financial advisor. Your tone is professional and direct. Analyze the provided transactions for bad spending habits and savings opportunities. Transactions with negative amounts are expenses, positive amounts are income. Respect the currency provided in the transaction details (e.g., KZT, USD, EUR).")
                                                                .build()))
                                                .build())
                                .contents(Collections.singletonList(GeminiDTOs.Content.builder()
                                                .role("user")
                                                .parts(Collections.singletonList(GeminiDTOs.Part.builder()
                                                                .text(formattedTransactions)
                                                                .build()))
                                                .build()))
                                .build();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<GeminiDTOs.GeminiRequest> entity = new HttpEntity<>(request, headers);

                try {
                        ResponseEntity<GeminiDTOs.GeminiResponse> response = restTemplate.postForEntity(
                                        GEMINI_API_URL + apiKey, entity, GeminiDTOs.GeminiResponse.class);

                        GeminiDTOs.GeminiResponse responseBody = response.getBody();
                        if (responseBody != null && responseBody.getCandidates() != null
                                        && !responseBody.getCandidates().isEmpty()) {
                                GeminiDTOs.Candidate candidate = responseBody.getCandidates().get(0);
                                if (candidate.getContent() != null && candidate.getContent().getParts() != null
                                                && !candidate.getContent().getParts().isEmpty()) {
                                        return candidate.getContent().getParts().get(0).getText();
                                }
                        }
                } catch (Exception e) {
                        log.error("Error analyzing spending with Gemini", e);
                        return "Unable to analyze spending at this time.";
                }

                return "No analysis available.";
        }

        private String formatTransaction(Transaction t) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String type = t.getAmount().doubleValue() < 0 ? "EXPENSE" : "INCOME";
                return String.format("[%s] %s: %s %s (%s)",
                                t.getDate().format(formatter),
                                t.getDescription(),
                                t.getAmount(),
                                t.getCurrency() != null ? t.getCurrency() : "USD", // Default to USD or make dynamic
                                type);
        }
}
