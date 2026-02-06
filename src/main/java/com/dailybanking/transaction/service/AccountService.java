package com.dailybanking.transaction.service;

import com.dailybanking.transaction.exception.InsufficientBalanceException;
import com.dailybanking.transaction.exception.InvalidAccountException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountService {

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    public void ensureExists(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new InvalidAccountException("Account id must not be blank");
        }
        balances.putIfAbsent(accountId, BigDecimal.ZERO);
    }

    public BigDecimal getBalance(String accountId) {
        ensureExists(accountId);
        return balances.get(accountId);
    }

    public void deposit(String accountId, BigDecimal amount) {
        ensureExists(accountId);
        balances.compute(accountId, (key, value) -> value.add(amount));
    }

    public void withdraw(String accountId, BigDecimal amount) {
        ensureExists(accountId);
        balances.compute(accountId, (key, value) -> {
            if (value.compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance for account " + accountId);
            }
            return value.subtract(amount);
        });
    }
}
