package com.dailybanking.transaction.service;

import com.dailybanking.transaction.dto.DepositRequest;
import com.dailybanking.transaction.dto.TransactionEvent;
import com.dailybanking.transaction.dto.TransferRequest;
import com.dailybanking.transaction.exception.DuplicateTransactionException;
import com.dailybanking.transaction.exception.InsufficientBalanceException;
import com.dailybanking.transaction.model.Transaction;
import com.dailybanking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setup() {
        accountService = new AccountService();
        transactionService = new TransactionService(
                transactionRepository,
                accountService,
                kafkaTemplate,
                "banking.transaction.events");
    }

    @Test
    void shouldDepositMoney() {
        DepositRequest request = new DepositRequest();
        request.setAccountId("acc-1");
        request.setAmount(BigDecimal.valueOf(120));
        request.setCurrency("EUR");
        request.setIdempotencyKey("dep-1");

        when(transactionRepository.findByIdempotencyKey("dep-1")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = transactionService.deposit(request);

        assertEquals(BigDecimal.valueOf(120), response.getAmount());
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }

    @Test
    void shouldRejectDuplicateIdempotencyKey() {
        DepositRequest request = new DepositRequest();
        request.setAccountId("acc-1");
        request.setAmount(BigDecimal.valueOf(10));
        request.setCurrency("EUR");
        request.setIdempotencyKey("dup-key");

        when(transactionRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(new Transaction()));

        assertThrows(DuplicateTransactionException.class, () -> transactionService.deposit(request));
    }

    @Test
    void shouldFailTransferWhenBalanceIsInsufficient() {
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId("source");
        request.setTargetAccountId("target");
        request.setAmount(BigDecimal.valueOf(200));
        request.setCurrency("EUR");
        request.setIdempotencyKey("tx-1");

        accountService.deposit("source", BigDecimal.valueOf(100));
        when(transactionRepository.findByIdempotencyKey("tx-1")).thenReturn(Optional.empty());

        assertThrows(InsufficientBalanceException.class, () -> transactionService.transfer(request));
    }
}
