package com.fintrack.backend.service;

import com.fintrack.backend.dto.SaltEdgeDTOs;
import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SaltEdgeServiceTest {

        @Mock
        private RestTemplate restTemplate;

        @Mock
        private com.fintrack.backend.repository.TransactionRepository transactionRepository;

        @Mock
        private com.fintrack.backend.repository.UserRepository userRepository;

        @Mock
        private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

        @Mock
        private CategorizationService categorizationService;

        @InjectMocks
        private SaltEdgeService saltEdgeService;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                // Reflectively set private fields purely for testing if not using Spring
                // context
                try {
                        java.lang.reflect.Field appIdField = SaltEdgeService.class.getDeclaredField("appId");
                        appIdField.setAccessible(true);
                        appIdField.set(saltEdgeService, "test-app-id");

                        java.lang.reflect.Field secretField = SaltEdgeService.class.getDeclaredField("secret");
                        secretField.setAccessible(true);
                        secretField.set(saltEdgeService, "test-secret");
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }

                // Default: categorization returns "Food" for test descriptions
                when(categorizationService.categorize(any(String.class))).thenReturn("Food");
        }

        @Test
        void createConnectSession_Success() {
                SaltEdgeDTOs.ConnectResponseData responseData = new SaltEdgeDTOs.ConnectResponseData(
                                "http://connect.url",
                                "2024-12-31");
                SaltEdgeDTOs.SaltEdgeConnectResponse response = new SaltEdgeDTOs.SaltEdgeConnectResponse(responseData);

                when(restTemplate.exchange(
                                any(String.class),
                                eq(HttpMethod.POST),
                                any(HttpEntity.class),
                                eq(SaltEdgeDTOs.SaltEdgeConnectResponse.class)))
                                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

                String connectUrl = saltEdgeService.createConnectSession("cust123");
                assertEquals("http://connect.url", connectUrl);
        }

        @Test
        void fetchTransactions_Success() {
                SaltEdgeDTOs.SaltEdgeTransactionData txData = new SaltEdgeDTOs.SaltEdgeTransactionData();
                txData.setId("tx1");
                txData.setAmount(new BigDecimal("100.50"));
                txData.setCurrencyCode("USD");
                txData.setDescription("Grocery");
                txData.setMadeOn(LocalDate.of(2023, 10, 27));
                txData.setCategory("Food");

                SaltEdgeDTOs.SaltEdgeTransactionResponse response = new SaltEdgeDTOs.SaltEdgeTransactionResponse(
                                Collections.singletonList(txData), null);

                when(restTemplate.exchange(
                                any(String.class),
                                eq(HttpMethod.GET),
                                any(HttpEntity.class),
                                eq(SaltEdgeDTOs.SaltEdgeTransactionResponse.class)))
                                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

                // Mock repository calls
                when(transactionRepository.findByExternalIdIn(anyList())).thenReturn(Collections.emptyList());
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

                List<Transaction> transactions = saltEdgeService.fetchTransactions("conn123", new User());

                assertNotNull(transactions);
                assertEquals(1, transactions.size());
                Transaction tx = transactions.get(0);
                assertEquals("tx1", tx.getExternalId());
                assertEquals(new BigDecimal("100.50"), tx.getAmount());
                assertEquals("USD", tx.getCurrency());
                assertEquals("Grocery", tx.getDescription());
                assertEquals(LocalDate.of(2023, 10, 27).atStartOfDay(), tx.getDate());
                assertEquals(Transaction.TransactionType.INCOME, tx.getType()); // Positive amount -> INCOME
        }

        @Test
        void importDataForCustomer_Success() {
                User user = new User();
                user.setSaltEdgeCustomerId("cust123");

                SaltEdgeDTOs.ConnectionData connData = new SaltEdgeDTOs.ConnectionData();
                connData.setId("conn1");

                SaltEdgeDTOs.SaltEdgeConnectionResponse connResponse = new SaltEdgeDTOs.SaltEdgeConnectionResponse(
                                Collections.singletonList(connData));

                when(restTemplate.exchange(
                                eq("https://www.saltedge.com/api/v6/connections?customer_id=cust123"),
                                eq(HttpMethod.GET),
                                any(HttpEntity.class),
                                eq(SaltEdgeDTOs.SaltEdgeConnectionResponse.class)))
                                .thenReturn(new ResponseEntity<>(connResponse, HttpStatus.OK));

                SaltEdgeDTOs.SaltEdgeTransactionResponse txResponse = new SaltEdgeDTOs.SaltEdgeTransactionResponse(
                                Collections.emptyList(), null);
                when(restTemplate.exchange(
                                eq("https://www.saltedge.com/api/v6/transactions?connection_id=conn1"),
                                eq(HttpMethod.GET),
                                any(HttpEntity.class),
                                eq(SaltEdgeDTOs.SaltEdgeTransactionResponse.class)))
                                .thenReturn(new ResponseEntity<>(txResponse, HttpStatus.OK));

                when(transactionRepository.findByExternalIdIn(anyList())).thenReturn(Collections.emptyList());

                saltEdgeService.importDataForCustomer(user);
        }
}
