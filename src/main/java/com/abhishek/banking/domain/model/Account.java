package com.abhishek.banking.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import com.abhishek.banking.domain.enums.AccountType;
import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.exception.InsufficientFundsException;
import com.abhishek.banking.domain.exception.InvalidAmountException;
import com.abhishek.banking.domain.value.Money;

public abstract class Account {
    private final String id;
    private final String customerId;
    private final AccountType type;
    private final Instant createdAt;
    
    protected Money balance;
    private volatile boolean isActive;
    
    // Lock for concurrent in-memory operations on this specific account
    private final ReentrantLock lock = new ReentrantLock();

    protected Account(String id, String customerId, AccountType type, Money initialBalance, Instant createdAt, boolean isActive) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("Account ID required");
        if (customerId == null || customerId.isEmpty()) throw new IllegalArgumentException("Customer ID required");
        if (initialBalance == null) throw new IllegalArgumentException("Initial balance required");
        
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.balance = initialBalance;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.isActive = isActive;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public AccountType getType() {
        return type;
    }

    public Money getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return isActive;
    }
    
    public void deactivate() {
        lock.lock();
        try {
            this.isActive = false;
        } finally {
            lock.unlock();
        }
    }
    
    public void activate() {
        lock.lock();
        try {
            this.isActive = true;
        } finally {
            lock.unlock();
        }
    }

    public void deposit(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidAmountException("Deposit amount must be positive");
        }
        
        lock.lock();
        try {
            checkActive();
            this.balance = this.balance.add(amount);
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidAmountException("Withdrawal amount must be positive");
        }

        lock.lock();
        try {
            checkActive();
            validateWithdrawal(amount);
            this.balance = this.balance.subtract(amount);
        } finally {
            lock.unlock();
        }
    }
    
    protected void checkActive() {
        if (!isActive) {
            throw new BankingException("Account " + id + " is inactive");
        }
    }

    /**
     * Subclasses implement specific withdrawal rules (e.g. overdraft limits, minimum balances).
     */
    protected abstract void validateWithdrawal(Money amount) throws InsufficientFundsException;
    
    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
