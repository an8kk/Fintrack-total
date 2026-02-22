package com.fintrack.backend.controller;

import com.fintrack.backend.dto.AdminCreateTransactionDto;
import com.fintrack.backend.dto.AdminCreateUserDto;
import com.fintrack.backend.dto.AdminStatsDto;
import com.fintrack.backend.dto.AdminUpdateUserDto;
import com.fintrack.backend.dto.TransactionDto;
import com.fintrack.backend.dto.UserDetailDto;
import com.fintrack.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        log.info("GET /api/admin/stats");
        return ResponseEntity.ok(adminService.getSystemStats());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserDetailDto>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/admin/users — search={}, page={}, size={}", search, page, size);
        return ResponseEntity.ok(
                adminService.getUsersWithSearch(search, PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @PostMapping("/users")
    public ResponseEntity<UserDetailDto> createUser(@Valid @RequestBody AdminCreateUserDto dto) {
        log.info("POST /api/admin/users — email={}", dto.getEmail());
        return ResponseEntity.ok(adminService.createUser(dto));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDetailDto> getUserDetail(@PathVariable Long id) {
        log.info("GET /api/admin/users/{}", id);
        return ResponseEntity.ok(adminService.getUserDetail(id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDetailDto> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUpdateUserDto dto) {
        log.info("PUT /api/admin/users/{}", id);
        return ResponseEntity.ok(adminService.updateUser(id, dto));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDetailDto> changeUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("PUT /api/admin/users/{}/role — newRole={}", id, body.get("role"));
        return ResponseEntity.ok(adminService.changeUserRole(id, body.get("role")));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/admin/users/{}", id);
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("status", "User deleted"));
    }

    @GetMapping("/users/{id}/categories")
    public ResponseEntity<?> getUserCategories(@PathVariable Long id) {
        log.info("GET /api/admin/users/{}/categories", id);
        return ResponseEntity.ok(adminService.getUserCategories(id));
    }

    @GetMapping("/users/{id}/transactions")
    public ResponseEntity<List<TransactionDto>> getUserTransactions(@PathVariable Long id) {
        log.info("GET /api/admin/users/{}/transactions", id);
        return ResponseEntity.ok(adminService.getUserTransactions(id));
    }

    @PostMapping("/users/{id}/transactions")
    public ResponseEntity<TransactionDto> createTransaction(
            @PathVariable Long id,
            @Valid @RequestBody AdminCreateTransactionDto dto) {
        log.info("POST /api/admin/users/{}/transactions", id);
        return ResponseEntity.ok(adminService.createTransactionForUser(id, dto));
    }

    // ─── Salt Edge ──────────────────────────────────────────────

    @PostMapping("/users/{id}/saltedge/session")
    public ResponseEntity<?> createSaltEdgeSession(@PathVariable Long id) {
        log.info("POST /api/admin/users/{}/saltedge/session", id);
        return ResponseEntity.ok(adminService.createSaltEdgeSession(id));
    }

    // ─── Audit Logs ────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("GET /api/admin/audit-logs — page={}, size={}", page, size);
        return ResponseEntity.ok(adminService.getAuditLogs(page, size));
    }

    // ─── AI System Insights ────────────────────────────────────

    @GetMapping("/ai/system-insights")
    public ResponseEntity<Map<String, String>> getSystemInsights() {
        log.info("GET /api/admin/ai/system-insights");
        String insights = adminService.getSystemInsights();
        return ResponseEntity.ok(Map.of("insights", insights));
    }

    // ─── Global Notifications ──────────────────────────────────

    @PostMapping("/notifications/global")
    public ResponseEntity<?> sendGlobalNotification(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "System Notification");
        String message = body.getOrDefault("message", "");
        log.info("POST /api/admin/notifications/global — title={}", title);
        int count = adminService.sendGlobalNotification(title, message);
        return ResponseEntity.ok(Map.of("status", "Notification sent", "recipientCount", count));
    }

    // ─── Merchant Category Mappings ────────────────────────────

    @GetMapping("/merchant-maps")
    public ResponseEntity<?> getMerchantMappings() {
        log.info("GET /api/admin/merchant-maps");
        return ResponseEntity.ok(adminService.getAllMerchantMappings());
    }

    @PostMapping("/merchant-maps")
    public ResponseEntity<?> createMerchantMapping(@RequestBody Map<String, String> body) {
        log.info("POST /api/admin/merchant-maps — keyword={}, category={}", body.get("keyword"), body.get("category"));
        return ResponseEntity.ok(adminService.createMerchantMapping(body.get("keyword"), body.get("category")));
    }

    @DeleteMapping("/merchant-maps/{id}")
    public ResponseEntity<?> deleteMerchantMapping(@PathVariable Long id) {
        log.info("DELETE /api/admin/merchant-maps/{}", id);
        adminService.deleteMerchantMapping(id);
        return ResponseEntity.ok(Map.of("status", "Mapping deleted"));
    }

    // ─── Health ────────────────────────────────────────────────

    @GetMapping("/health/external-services")
    public ResponseEntity<?> getExternalServicesHealth() {
        log.info("GET /api/admin/health/external-services");
        return ResponseEntity.ok(adminService.getExternalServicesHealth());
    }
}
