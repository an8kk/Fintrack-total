package com.fintrack.backend.service;

import com.fintrack.backend.dto.SaltEdgeDTOs;
import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.entity.Transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaltEdgeService {

    private final RestTemplate restTemplate;
    private final com.fintrack.backend.repository.TransactionRepository transactionRepository;
    private final com.fintrack.backend.repository.UserRepository userRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final CategorizationService categorizationService;

    @Value("${saltedge.app-id}")
    private String appId;

    @Value("${saltedge.secret}")
    private String secret;

    private static final String BASE_URL = "https://www.saltedge.com/api/v6";

    public String createConnectSession(String customerId) {
        String url = BASE_URL + "/connections/connect";
        log.info("Creating Connect Session for customer: {} with return_to: {}", customerId,
                "http://localhost:8001/");

        HttpHeaders headers = new HttpHeaders();
        headers.set("App-id", appId);
        headers.set("Secret", secret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        SaltEdgeDTOs.ConnectRequestData requestData = SaltEdgeDTOs.ConnectRequestData.builder()
                .customerId(customerId)
                .consent(SaltEdgeDTOs.Consent.builder()
                        .scopes(List.of("accounts", "transactions"))
                        .fromDate(java.time.LocalDate.now().minusDays(90))
                        .build())
                .attempt(SaltEdgeDTOs.Attempt.builder()
                        .fetchScopes(List.of("accounts", "transactions"))
                        .returnTo("http://localhost:8001/")
                        .build())
                .build();

        SaltEdgeDTOs.SaltEdgeConnectRequest requestBody = SaltEdgeDTOs.SaltEdgeConnectRequest.builder()
                .data(requestData)
                .build();

        HttpEntity<SaltEdgeDTOs.SaltEdgeConnectRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<SaltEdgeDTOs.SaltEdgeConnectResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, SaltEdgeDTOs.SaltEdgeConnectResponse.class);

            SaltEdgeDTOs.SaltEdgeConnectResponse body = response.getBody();
            if (body != null && body.getData() != null) {
                return body.getData().getConnectUrl();
            }
        } catch (Exception e) {
            log.error("Error creating connect session", e);
            throw new RuntimeException("Failed to create connect session", e);
        }
        return null;
    }

    public void handleCallback(Map<String, Object> callbackData) {
        log.info("Received Salt Edge callback: {}", callbackData);

        if (callbackData != null && callbackData.containsKey("data")) {
            Map<String, Object> data = (Map<String, Object>) callbackData.get("data");

            if (data.containsKey("stage") && "finish".equals(data.get("stage"))) {
                String connectionId = (String) data.get("connection_id");
                String customerId = (String) data.get("customer_id");

                log.info("Connection finished. Connection ID: {}, Customer ID: {}", connectionId, customerId);

                User user = userRepository.findBySaltEdgeCustomerId(customerId)
                        .orElseThrow(
                                () -> new RuntimeException("User not found for Salt Edge Customer ID: " + customerId));

                // Persist the connection_id for future syncs
                user.setSaltEdgeConnectionId(connectionId);
                userRepository.save(user);
                log.info("Saved connection_id '{}' for user '{}'", connectionId, user.getEmail());

                fetchTransactions(connectionId, user);
            }
        }
    }

    public List<Transaction> fetchTransactions(String connectionId, User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("App-id", appId);
        headers.set("Secret", secret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        List<Transaction> allSaved = new java.util.ArrayList<>();

        try {
            String url = BASE_URL + "/transactions?connection_id=" + connectionId;

            while (url != null) {
                log.info("Fetching transactions page: {}", url);

                ResponseEntity<SaltEdgeDTOs.SaltEdgeTransactionResponse> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, SaltEdgeDTOs.SaltEdgeTransactionResponse.class);

                SaltEdgeDTOs.SaltEdgeTransactionResponse body = response.getBody();
                if (body == null || body.getData() == null || body.getData().isEmpty()) {
                    break;
                }

                List<SaltEdgeDTOs.SaltEdgeTransactionData> transactionDataList = body.getData();
                List<String> externalIds = transactionDataList.stream()
                        .map(SaltEdgeDTOs.SaltEdgeTransactionData::getId)
                        .collect(Collectors.toList());

                Set<String> existingExternalIds = transactionRepository.findByExternalIdIn(externalIds).stream()
                        .map(Transaction::getExternalId)
                        .collect(Collectors.toSet());

                List<Transaction> saved = transactionDataList.stream()
                        .filter(data -> !existingExternalIds.contains(data.getId()))
                        .map(data -> mapToTransaction(data, user))
                        .map(transactionRepository::save)
                        .collect(Collectors.toList());
                allSaved.addAll(saved);

                // Check for next page
                if (body.getMeta() != null && body.getMeta().getNextId() != null) {
                    url = BASE_URL + "/transactions?connection_id=" + connectionId
                            + "&from_id=" + body.getMeta().getNextId();
                } else {
                    url = null;
                }
            }
        } catch (Exception e) {
            log.error("Error fetching transactions", e);
            throw new RuntimeException("Failed to fetch transactions", e);
        }

        // Notify Frontend via WebSocket
        if (!allSaved.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/transactions", "Transactions updated");
        }

        return allSaved;
    }

    public void importDataForCustomer(User user) {
        String customerId = user.getSaltEdgeCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            return;
        }

        String url = BASE_URL + "/connections?customer_id=" + customerId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("App-id", appId);
        headers.set("Secret", secret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SaltEdgeDTOs.SaltEdgeConnectionResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, SaltEdgeDTOs.SaltEdgeConnectionResponse.class);

            SaltEdgeDTOs.SaltEdgeConnectionResponse connBody = response.getBody();
            if (connBody != null && connBody.getData() != null) {
                for (SaltEdgeDTOs.ConnectionData connection : connBody.getData()) {
                    log.info("Fetching transactions for connection: {}", connection.getId());
                    fetchTransactions(connection.getId(), user);
                }
            }
        } catch (Exception e) {
            log.error("Error importing data for customer: {}", customerId, e);
            // Don't throw exception to avoid blocking registration if sync fails
        }
    }

    private Transaction mapToTransaction(SaltEdgeDTOs.SaltEdgeTransactionData data, User user) {
        TransactionType type = data.getAmount().signum() > 0 ? TransactionType.INCOME : TransactionType.EXPENSE;

        // Use hybrid categorization: local keyword map → AI fallback
        String category = categorizationService.categorize(data.getDescription());
        // Fall back to Salt Edge category if our categorizer returns Uncategorized
        if ("Uncategorized".equals(category) && data.getCategory() != null && !data.getCategory().isBlank()) {
            category = data.getCategory();
        }

        return Transaction.builder()
                .user(user)
                .externalId(data.getId())
                .amount(data.getAmount().abs())
                .currency(data.getCurrencyCode())
                .description(data.getDescription())
                .date(data.getMadeOn().atStartOfDay())
                .category(category)
                .type(type)
                .build();
    }
}
