package com.fintrack.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.backend.controller.TransactionController;
import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private TransactionRepository transactionRepository; // Needed because Controller injects it

    @Autowired
    private ObjectMapper objectMapper;

    // POST request returns 200 OK on success
    @Test
    void createTransaction_Success_Returns200() throws Exception {
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("100"))
                .type(Transaction.TransactionType.INCOME)
                .build();

        when(transactionService.saveTransaction(eq(1L), any(Transaction.class))).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100));
    }

    // POST request returns 400 Bad Request on Service Exception (e.g., Low Balance)
    @Test
    void createTransaction_InsufficientFunds_Returns400() throws Exception {
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("500"))
                .type(Transaction.TransactionType.EXPENSE)
                .build();

        when(transactionService.saveTransaction(eq(1L), any(Transaction.class)))
                .thenThrow(new RuntimeException("Insufficient balance"));

        mockMvc.perform(post("/api/transactions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient balance"));
    }

    // GET request returns list of transactions
    @Test
    void getUserTransactions_ReturnsList() throws Exception {
        Transaction t1 = Transaction.builder().description("Coffee").build();
        Transaction t2 = Transaction.builder().description("Salary").build();
        List<Transaction> list = Arrays.asList(t1, t2);

        when(transactionRepository.findByUserId(1L)).thenReturn(list);

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Coffee"));
    }

    // GET request returns empty list if no data
    @Test
    void getUserTransactions_NoData_ReturnsEmpty() throws Exception {
        when(transactionRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}