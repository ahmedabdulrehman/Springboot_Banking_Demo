package com.dailybanking.transaction.controller;

import com.dailybanking.transaction.dto.DepositRequest;
import com.dailybanking.transaction.dto.TransactionResponse;
import com.dailybanking.transaction.dto.TransferRequest;
import com.dailybanking.transaction.dto.WithdrawalRequest;
import com.dailybanking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "Deposit money into an account")
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.deposit(request));
    }

    @Operation(summary = "Withdraw money from an account")
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.withdraw(request));
    }

    @Operation(summary = "Transfer money between two accounts")
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(request));
    }

    @Operation(summary = "Get a transaction by id")
    @GetMapping("/{transactionId}")
    public TransactionResponse getById(@PathVariable String transactionId) {
        return transactionService.getById(transactionId);
    }

    @Operation(summary = "Get transaction history for an account")
    @GetMapping
    public List<TransactionResponse> byAccount(@RequestParam String accountId) {
        return transactionService.findByAccount(accountId);
    }
}
