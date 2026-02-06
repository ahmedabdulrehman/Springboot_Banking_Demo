package com.dailybanking.transaction.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String transactionId) {
        super("Transaction not found: " + transactionId);
    }
}
