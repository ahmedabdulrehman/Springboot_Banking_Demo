package com.dailybanking.transaction.repository;

import com.dailybanking.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
            String sourceAccountId,
            String targetAccountId);

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
