package com.dailybanking.transaction.service;

import com.dailybanking.transaction.dto.DepositRequest;
import com.dailybanking.transaction.dto.TransactionEvent;
import com.dailybanking.transaction.dto.TransactionResponse;
import com.dailybanking.transaction.dto.TransferRequest;
import com.dailybanking.transaction.dto.WithdrawalRequest;
import com.dailybanking.transaction.exception.DuplicateTransactionException;
import com.dailybanking.transaction.exception.TransactionNotFoundException;
import com.dailybanking.transaction.model.Transaction;
import com.dailybanking.transaction.model.TransactionStatus;
import com.dailybanking.transaction.model.TransactionType;
import com.dailybanking.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final String topicName;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountService accountService,
                              KafkaTemplate<String, TransactionEvent> kafkaTemplate,
                              @Value("${app.kafka.transaction-topic:banking.transaction.events}") String topicName) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        String idempotencyKey = normalizeIdempotency(request.getIdempotencyKey());
        ensureIdempotency(idempotencyKey);

        accountService.deposit(request.getAccountId(), request.getAmount());
        Transaction saved = saveTransaction(idempotencyKey, TransactionType.DEPOSIT, request.getAccountId(), null,
                request.getAmount(), request.getCurrency(), request.getDescription());
        publishEvent(saved, "transaction.completed");
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawalRequest request) {
        String idempotencyKey = normalizeIdempotency(request.getIdempotencyKey());
        ensureIdempotency(idempotencyKey);

        accountService.withdraw(request.getAccountId(), request.getAmount());
        Transaction saved = saveTransaction(idempotencyKey, TransactionType.WITHDRAWAL, request.getAccountId(), null,
                request.getAmount(), request.getCurrency(), request.getDescription());
        publishEvent(saved, "transaction.completed");
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        String idempotencyKey = normalizeIdempotency(request.getIdempotencyKey());
        ensureIdempotency(idempotencyKey);

        accountService.withdraw(request.getSourceAccountId(), request.getAmount());
        accountService.deposit(request.getTargetAccountId(), request.getAmount());

        Transaction saved = saveTransaction(idempotencyKey, TransactionType.TRANSFER,
                request.getSourceAccountId(), request.getTargetAccountId(),
                request.getAmount(), request.getCurrency(), request.getDescription());
        publishEvent(saved, "transaction.completed");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByAccount(String accountId) {
        return transactionRepository
                .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Transaction saveTransaction(String idempotencyKey,
                                        TransactionType type,
                                        String sourceAccountId,
                                        String targetAccountId,
                                        BigDecimal amount,
                                        String currency,
                                        String description) {
        Transaction transaction = new Transaction();
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setSourceAccountId(sourceAccountId);
        transaction.setTargetAccountId(targetAccountId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency.toUpperCase());
        transaction.setDescription(description);
        transaction.setCompletedAt(Instant.now());
        return transactionRepository.save(transaction);
    }

    private String normalizeIdempotency(String key) {
        return (key == null || key.isBlank()) ? UUID.randomUUID().toString() : key;
    }

    private void ensureIdempotency(String idempotencyKey) {
        if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new DuplicateTransactionException(idempotencyKey);
        }
    }

    private void publishEvent(Transaction transaction, String eventType) {
        TransactionEvent event = new TransactionEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setTransactionId(transaction.getId());
        event.setSourceAccountId(transaction.getSourceAccountId());
        event.setTargetAccountId(transaction.getTargetAccountId());
        event.setAmount(transaction.getAmount());
        event.setCurrency(transaction.getCurrency());
        event.setOccurredAt(Instant.now());
        kafkaTemplate.send(topicName, transaction.getId(), event);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(transaction.getId());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setSourceAccountId(transaction.getSourceAccountId());
        response.setTargetAccountId(transaction.getTargetAccountId());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());
        return response;
    }
}
