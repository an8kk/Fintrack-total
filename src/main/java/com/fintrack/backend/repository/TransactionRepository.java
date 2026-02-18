package com.fintrack.backend.repository;

import com.fintrack.backend.entity.Transaction;
import com.fintrack.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDateTime start, LocalDateTime end);

    boolean existsByExternalId(String externalId);

    List<Transaction> findByExternalIdIn(List<String> externalIds);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END), 0) FROM Transaction t WHERE t.user.id = :userId")
    BigDecimal calculateBalanceByUserId(@Param("userId") Long userId);
}