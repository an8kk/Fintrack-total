package com.fintrack.backend.service;

import com.fintrack.backend.dto.AdminCreateTransactionDto;
import com.fintrack.backend.dto.AdminCreateUserDto;
import com.fintrack.backend.dto.AdminStatsDto;
import com.fintrack.backend.dto.AdminUpdateUserDto;
import com.fintrack.backend.dto.TransactionDto;
import com.fintrack.backend.dto.UserDetailDto;
import com.fintrack.backend.entity.*;
import com.fintrack.backend.exception.ResourceNotFoundException;
import com.fintrack.backend.repository.AuditLogRepository;
import com.fintrack.backend.repository.CategoryRepository;
import com.fintrack.backend.repository.MerchantCategoryMapRepository;
import com.fintrack.backend.repository.NotificationRepository;
import com.fintrack.backend.repository.TransactionRepository;
import com.fintrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final MerchantCategoryMapRepository merchantCategoryMapRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SaltEdgeService saltEdgeService;
    private final GeminiService geminiService;

    public AdminStatsDto getSystemStats() {
        log.info("Fetching admin system stats");
        long totalUsers = userRepository.count();
        long totalTransactions = transactionRepository.count();
        BigDecimal totalVolume = transactionRepository.sumAllAmounts();
        long activeToday = transactionRepository.countActiveUsersSince(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));

        return AdminStatsDto.builder()
                .totalUsers(totalUsers)
                .totalTransactions(totalTransactions)
                .totalVolume(totalVolume != null ? totalVolume : BigDecimal.ZERO)
                .activeToday(activeToday)
                .build();
    }

    public Page<UserDetailDto> getUsersWithSearch(String search, Pageable pageable) {
        log.info("Admin searching users with query='{}', page={}", search, pageable.getPageNumber());
        Page<User> users;
        if (search == null || search.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.searchByUsernameOrEmail(search, pageable);
        }
        return users.map(this::mapToDetailDto);
    }

    public UserDetailDto getUserDetail(Long userId) {
        log.info("Admin fetching detail for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return mapToDetailDto(user);
    }

    @Transactional
    public UserDetailDto createUser(AdminCreateUserDto dto) {
        log.info("Admin creating user: {}", dto.getEmail());

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + dto.getEmail());
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        user.setBalance(BigDecimal.ZERO);

        User saved = userRepository.save(user);
        return mapToDetailDto(saved);
    }

    @Transactional
    public UserDetailDto updateUser(Long userId, AdminUpdateUserDto dto) {
        log.info("Admin updating userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getRole() != null) {
            user.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        }
        if (dto.getBlocked() != null) {
            user.setBlocked(dto.getBlocked());
        }

        User saved = userRepository.save(user);
        return mapToDetailDto(saved);
    }

    @Transactional
    public UserDetailDto changeUserRole(Long userId, String newRole) {
        log.info("Admin changing role for userId={} to {}", userId, newRole);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        try {
            user.setRole(Role.valueOf(newRole.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
        }

        User saved = userRepository.save(user);
        return mapToDetailDto(saved);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Admin deleting userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userRepository.delete(user);
        logAudit("DELETE_USER", "User", userId, "Deleted user: " + user.getEmail());
        log.info("User {} deleted successfully", userId);
    }

    @Transactional
    public TransactionDto createTransactionForUser(Long userId, AdminCreateTransactionDto dto) {
        log.info("Admin creating transaction for userId={}, amount={}, type={}", userId, dto.getAmount(),
                dto.getType());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Transaction tx = Transaction.builder()
                .amount(dto.getAmount())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .date(LocalDateTime.now())
                .type(Transaction.TransactionType.valueOf(dto.getType().toUpperCase()))
                .currency(dto.getCurrency())
                .user(user)
                .build();

        Transaction saved = transactionRepository.save(tx);
        return mapToTransactionDto(saved);
    }

    public List<TransactionDto> getUserTransactions(Long userId) {
        log.info("Admin fetching transactions for userId={}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return transactionRepository.findByUserId(userId).stream()
                .map(this::mapToTransactionDto)
                .toList();
    }

    public List<Category> getUserCategories(Long userId) {
        log.info("Admin fetching categories for userId={}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return categoryRepository.findByUserId(userId);
    }

    // ─── Salt Edge ──────────────────────────────────────────────

    public Map<String, String> createSaltEdgeSession(Long userId) {
        log.info("Admin creating Salt Edge session for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        String connectUrl = saltEdgeService.ensureCustomerAndCreateSession(user);
        logAudit("SALT_EDGE_SESSION", "User", userId, "Created Salt Edge session");
        return Map.of("connectUrl", connectUrl, "customerId",
                user.getSaltEdgeCustomerId() != null ? user.getSaltEdgeCustomerId() : "");
    }

    // ─── Audit Logs ────────────────────────────────────────────

    public Page<AuditLog> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }

    // ─── AI System Insights ────────────────────────────────────

    public String getSystemInsights() {
        log.info("Admin requesting AI system insights");

        // Get recent transactions for analysis (last 50 system-wide)
        List<Transaction> recent = transactionRepository.findAll(
                PageRequest.of(0, 50, org.springframework.data.domain.Sort.by("date").descending())).getContent();

        if (recent.isEmpty()) {
            return "Not enough transaction data to generate insights.";
        }

        return geminiService.analyzeSpending(recent);
    }

    // ─── Global Notifications ──────────────────────────────────

    @Transactional
    public int sendGlobalNotification(String title, String message) {
        log.info("Admin sending global notification: {}", title);
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            Notification notif = Notification.builder()
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .date(LocalDateTime.now())
                    .user(user)
                    .build();
            notificationRepository.save(notif);
        }
        logAudit("GLOBAL_NOTIFICATION", "System", null, "Sent to " + allUsers.size() + " users: " + title);
        return allUsers.size();
    }

    // ─── Merchant Category Mappings ────────────────────────────

    public List<MerchantCategoryMap> getAllMerchantMappings() {
        return merchantCategoryMapRepository.findAll();
    }

    @Transactional
    public MerchantCategoryMap createMerchantMapping(String keyword, String category) {
        MerchantCategoryMap map = MerchantCategoryMap.builder()
                .keyword(keyword)
                .category(category)
                .source(MerchantCategoryMap.Source.SEED)
                .createdAt(LocalDateTime.now())
                .build();
        logAudit("CREATE_MERCHANT_MAP", "MerchantCategoryMap", null, keyword + " → " + category);
        return merchantCategoryMapRepository.save(map);
    }

    @Transactional
    public void deleteMerchantMapping(Long id) {
        merchantCategoryMapRepository.deleteById(id);
        logAudit("DELETE_MERCHANT_MAP", "MerchantCategoryMap", id, "Deleted mapping");
    }

    // ─── Health ────────────────────────────────────────────────

    public Map<String, Object> getExternalServicesHealth() {
        long totalUsers = userRepository.count();
        long linkedUsers = userRepository.findAll().stream()
                .filter(u -> u.getSaltEdgeCustomerId() != null && !u.getSaltEdgeCustomerId().isBlank())
                .count();
        long connectedUsers = userRepository.findAll().stream()
                .filter(u -> u.getSaltEdgeConnectionId() != null && !u.getSaltEdgeConnectionId().isBlank())
                .count();

        return Map.of(
                "totalUsers", totalUsers,
                "saltEdgeLinked", linkedUsers,
                "saltEdgeConnected", connectedUsers);
    }

    // ─── Private Helpers ───────────────────────────────────────

    private void logAudit(String action, String targetType, Long targetId, String details) {
        try {
            String adminEmail = "system";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                adminEmail = auth.getName();
            }
            auditLogRepository.save(AuditLog.builder()
                    .adminEmail(adminEmail)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    private TransactionDto mapToTransactionDto(Transaction tx) {
        return TransactionDto.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .category(tx.getCategory())
                .description(tx.getDescription())
                .date(tx.getDate())
                .type(tx.getType() != null ? tx.getType().name() : null)
                .currency(tx.getCurrency())
                .userId(tx.getUser() != null ? tx.getUser().getId() : null)
                .build();
    }

    private UserDetailDto mapToDetailDto(User user) {
        BigDecimal balance = transactionRepository.calculateBalanceByUserId(user.getId());
        long txnCount = transactionRepository.countByUserId(user.getId());
        BigDecimal totalIncome = transactionRepository.sumIncomeByUserId(user.getId());
        BigDecimal totalExpense = transactionRepository.sumExpenseByUserId(user.getId());

        return UserDetailDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isBlocked(user.isBlocked())
                .balance(balance != null ? balance : BigDecimal.ZERO)
                .transactionCount(txnCount)
                .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .totalExpense(totalExpense != null ? totalExpense : BigDecimal.ZERO)
                .build();
    }
}
