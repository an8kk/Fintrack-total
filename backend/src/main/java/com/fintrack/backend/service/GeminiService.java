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

import java.math.BigDecimal;
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

        private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

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
                                                                .text("You are a senior financial advisor. Your tone is professional and direct. Analyze the provided transactions for bad spending habits and savings opportunities. Negative amounts are expenses, positive are income. Each transaction is labeled with its type for clarity. Respect the currency provided (e.g., KZT, USD, EUR).")
                                                                .build()))
                                                .build())
                                .contents(Collections.singletonList(GeminiDTOs.Content.builder()
                                                .role("user")
                                                .parts(Collections.singletonList(GeminiDTOs.Part.builder()
                                                                .text(formattedTransactions)
                                                                .build()))
                                                .build()))
                                .generationConfig(GeminiDTOs.GenerationConfig.builder()
                                                .maxOutputTokens(400)
                                                .temperature(0.7)
                                                .build())
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
                boolean isExpense = t.getType() == Transaction.TransactionType.EXPENSE;
                BigDecimal amount = isExpense ? t.getAmount().negate() : t.getAmount();
                String typeLabel = isExpense ? "EXPENSE" : "INCOME";

                return String.format("[%s] %s: %s %s (%s)",
                                t.getDate().format(formatter),
                                t.getDescription() != null ? t.getDescription() : "No description",
                                amount,
                                t.getCurrency() != null ? t.getCurrency() : "USD",
                                typeLabel);
        }

        // ─── Short-form insight helpers ───────────────────────────────

        private String callGemini(String systemPrompt, String userContent) {
                GeminiDTOs.GeminiRequest request = GeminiDTOs.GeminiRequest.builder()
                                .systemInstruction(GeminiDTOs.Content.builder()
                                                .parts(Collections.singletonList(GeminiDTOs.Part.builder()
                                                                .text(systemPrompt).build()))
                                                .build())
                                .contents(Collections.singletonList(GeminiDTOs.Content.builder()
                                                .role("user")
                                                .parts(Collections.singletonList(GeminiDTOs.Part.builder()
                                                                .text(userContent).build()))
                                                .build()))
                                .generationConfig(GeminiDTOs.GenerationConfig.builder()
                                                .maxOutputTokens(150)
                                                .temperature(0.5)
                                                .build())
                                .build();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<GeminiDTOs.GeminiRequest> entity = new HttpEntity<>(request, headers);

                try {
                        ResponseEntity<GeminiDTOs.GeminiResponse> response = restTemplate.postForEntity(
                                        GEMINI_API_URL + apiKey, entity, GeminiDTOs.GeminiResponse.class);
                        GeminiDTOs.GeminiResponse body = response.getBody();
                        if (body != null && body.getCandidates() != null && !body.getCandidates().isEmpty()) {
                                GeminiDTOs.Candidate c = body.getCandidates().get(0);
                                if (c.getContent() != null && c.getContent().getParts() != null
                                                && !c.getContent().getParts().isEmpty()) {
                                        return c.getContent().getParts().get(0).getText();
                                }
                        }
                } catch (Exception e) {
                        log.error("Gemini API error", e);
                }
                return "Unable to generate insight at this time.";
        }

        private static final String SHORT_SYSTEM = "You are a concise financial advisor. Reply in 1-2 short sentences, max 200 characters. Specific and actionable. IMPORTANT: Negative amounts are expenses, positive are income.";

        public String analyzeTransaction(Transaction t) {
                String data = formatTransaction(t);
                return callGemini(SHORT_SYSTEM,
                                "Give a brief insight on this single transaction:\n" + data);
        }

        public String analyzeCategory(String category, List<Transaction> txns) {
                if (txns == null || txns.isEmpty())
                        return "No transactions in this category.";
                String data = txns.stream().limit(20).map(this::formatTransaction)
                                .collect(Collectors.joining("\n"));
                return callGemini(SHORT_SYSTEM,
                                "Give a brief insight on spending in category \"" + category + "\":\n" + data);
        }

        public String analyzePeriod(String period, String periodLabel, List<Transaction> txns) {
                if (txns == null || txns.isEmpty())
                        return "No transactions in this period.";
                String data = txns.stream().limit(30).map(this::formatTransaction)
                                .collect(Collectors.joining("\n"));
                return callGemini(SHORT_SYSTEM,
                                "Give a brief insight on all spending during " + period + " (" + periodLabel + "):\n"
                                                + data);
        }
}
