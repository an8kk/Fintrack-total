package com.fintrack.backend.controller;

import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.UserRepository;
import com.fintrack.backend.service.SaltEdgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/saltedge")
@RequiredArgsConstructor
public class SaltEdgeController {

    private final SaltEdgeService saltEdgeService;
    private final UserRepository userRepository;

    @PostMapping("/connect_sessions/create")
    public ResponseEntity<Map<String, String>> createConnectSession(@RequestBody Map<String, String> request) {
        String customerId = request.get("customer_id");
        log.info("POST /api/saltedge/connect_sessions/create — customerId={}", customerId);
        if (customerId == null) {
            log.warn("Missing customer_id in connect session request");
            return ResponseEntity.badRequest().body(Map.of("error", "customer_id is required"));
        }
        String connectUrl = saltEdgeService.createConnectSession(customerId);
        log.info("Connect session created, redirecting to Salt Edge");
        return ResponseEntity.ok(Map.of("connect_url", connectUrl));
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody Map<String, Object> callbackData) {
        log.info("POST /api/saltedge/callback — received callback data keys={}", callbackData.keySet());
        saltEdgeService.handleCallback(callbackData);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> fetchTransactions(@RequestParam("connection_id") String connectionId) {
        log.info("GET /api/saltedge/transactions — connectionId={}", connectionId);
        User user = getAuthenticatedUser();
        List<Transaction> transactions = saltEdgeService.fetchTransactions(connectionId, user);
        log.info("Fetched {} transactions for connectionId={}", transactions.size(), connectionId);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, String>> importTransactions() {
        User user = getAuthenticatedUser();
        log.info("POST /api/saltedge/import — userId={}, customerId={}", user.getId(), user.getSaltEdgeCustomerId());
        saltEdgeService.importDataForCustomer(user);
        return ResponseEntity.ok(Map.of("status", "Import completed"));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {
        User user = getAuthenticatedUser();
        boolean connected = user.getSaltEdgeConnectionId() != null && !user.getSaltEdgeConnectionId().isBlank();
        log.info("GET /api/saltedge/status — userId={}, connected={}", user.getId(), connected);
        return ResponseEntity.ok(Map.of(
                "connected", connected,
                "connection_id", connected ? user.getSaltEdgeConnectionId() : "",
                "customer_id", user.getSaltEdgeCustomerId() != null ? user.getSaltEdgeCustomerId() : ""));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncTransactions() {
        User user = getAuthenticatedUser();
        log.info("POST /api/saltedge/sync — userId={}, connectionId={}", user.getId(), user.getSaltEdgeConnectionId());

        if (user.getSaltEdgeConnectionId() == null || user.getSaltEdgeConnectionId().isBlank()) {
            log.warn("Sync attempted but no connection found for userId={}", user.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No Salt Edge connection found. Please connect your bank first."));
        }

        List<Transaction> newTransactions = saltEdgeService.fetchTransactions(
                user.getSaltEdgeConnectionId(), user);

        log.info("Sync completed: {} new transactions for userId={}", newTransactions.size(), user.getId());
        return ResponseEntity.ok(Map.of(
                "status", "Sync completed",
                "new_transactions", newTransactions.size()));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
