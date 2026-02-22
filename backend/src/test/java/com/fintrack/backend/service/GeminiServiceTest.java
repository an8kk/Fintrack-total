package com.fintrack.backend.service;

import com.fintrack.backend.dto.GeminiDTOs;
import com.fintrack.backend.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class GeminiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        try {
            java.lang.reflect.Field apiKeyField = GeminiService.class.getDeclaredField("apiKey");
            apiKeyField.setAccessible(true);
            apiKeyField.set(geminiService, "test-api-key");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void analyzeSpending_Success() {
        Transaction t1 = Transaction.builder()
                .date(LocalDate.of(2023, 10, 27).atStartOfDay())
                .description("Grocery")
                .amount(new BigDecimal("100"))
                .currency("USD")
                .build();

        GeminiDTOs.Part part = new GeminiDTOs.Part();
        part.setText("Spending looks okay.");
        GeminiDTOs.Content content = new GeminiDTOs.Content();
        content.setParts(Collections.singletonList(part));
        GeminiDTOs.Candidate candidate = new GeminiDTOs.Candidate();
        candidate.setContent(content);
        GeminiDTOs.GeminiResponse response = new GeminiDTOs.GeminiResponse();
        response.setCandidates(Collections.singletonList(candidate));

        when(restTemplate.postForEntity(
                any(String.class),
                any(),
                eq(GeminiDTOs.GeminiResponse.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        String analysis = geminiService.analyzeSpending(Collections.singletonList(t1));

        assertEquals("Spending looks okay.", analysis);
    }

    @Test
    void analyzeSpending_EmptyList() {
        String analysis = geminiService.analyzeSpending(Collections.emptyList());
        assertEquals("No data to analyze yet.", analysis);
    }
}
