package com.dailybanking.transaction.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String idempotencyKey) {
        super("Duplicate transaction for idempotency key: " + idempotencyKey);
    }
}
