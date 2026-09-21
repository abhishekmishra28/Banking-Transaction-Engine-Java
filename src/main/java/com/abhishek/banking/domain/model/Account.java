package com.abhishek.banking.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import com.abhishek.banking.domain.enums.AccountType;
import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.exception.InsufficientFundsException;
import com.abhishek.banking.domain.exception.InvalidAmountException;
import com.abhishek.banking.domain.value.Money;

/**
 * Abstract base for all account types. Provides thread-safe balance operations
 * using a per-account ReentrantLock.
 *
 * <h3>Locking Strategy</h3>
 * <ul>
 *   <li>Single-account operations (deposit, withdraw) acquire the account's own lock.</li>
 *   <li>Cross-account transfers must be performed via TransactionService, which acquires
 *       both account locks in a deterministic lexicographic order to prevent deadlocks,
 *       then calls the internal (no-external-lock) variants {@code creditInternal} and
 *       {@code debitInternal}.</li>
 * </ul>
 */
public abstract class Account {

    private final String id;
    private final String customerId;
    private final AccountType type;
    private final Instant createdAt;

    protected Money balance;
    private volatile boolean isActive;

    /** Per-account fair lock. Exposed so TransactionService can order acquisitions. */
    private final ReentrantLock lock = new ReentrantLock();

    protected Account(String id, String customerId, AccountType type,
                      Money initialBalance, Instant createdAt, boolean isActive) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("Account ID required");
        if (customerId == null || customerId.isEmpty()) throw new IllegalArgumentException("Customer ID required");
        if (initialBalance == null) throw new IllegalArgumentException("Initial balance required");
        if (initialBalance.isNegative()) throw new IllegalArgumentException("Initial balance cannot be negative");

        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.balance = initialBalance;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.isActive = isActive;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public AccountType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }

    public Money getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    public boolean isActive() { return isActive; }

    public void deactivate() {
        lock.lock();
        try { this.isActive = false; } finally { lock.unlock(); }
    }

    public void activate() {
        lock.lock();
        try { this.isActive = true; } finally { lock.unlock(); }
    }

    // -------------------------------------------------------------------------
    // Public (locking) operations — used for single-account deposits / withdrawals
    // -------------------------------------------------------------------------

    /**
     * Thread-safe deposit. Validates amount and account status, then credits balance.
     */
    public void deposit(Money amount) {
        validateAmount(amount, "Deposit");
        lock.lock();
        try {
            checkActive();
            this.balance = this.balance.add(amount);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Thread-safe withdrawal. Validates amount, account status, and subclass rules.
     */
    public void withdraw(Money amount) {
        validateAmount(amount, "Withdrawal");
        lock.lock();
        try {
            checkActive();
            validateWithdrawal(amount);
            this.balance = this.balance.subtract(amount);
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Internal (no-lock) operations — called by TransactionService during transfers
    // while BOTH account locks are already held externally.
    // -------------------------------------------------------------------------

    /**
     * Credits balance without acquiring the lock.
     * <b>MUST only be called while the caller already holds this account's lock.</b>
     */
    public void creditInternal(Money amount) {
        checkActive();
        this.balance = this.balance.add(amount);
    }

    /**
     * Debits balance without acquiring the lock.
     * <b>MUST only be called while the caller already holds this account's lock.</b>
     */
    public void debitInternal(Money amount) {
        checkActive();
        validateWithdrawal(amount);
        this.balance = this.balance.subtract(amount);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    protected void checkActive() {
        if (!isActive) {
            throw new BankingException("Account " + id + " is inactive");
        }
    }

    private void validateAmount(Money amount, String operation) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidAmountException(operation + " amount must be positive");
        }
    }

    /**
     * Subclasses implement specific withdrawal rules (e.g. overdraft limits, minimum balances).
     * Called while the account lock is already held.
     */
    protected abstract void validateWithdrawal(Money amount) throws InsufficientFundsException;

    /** Returns the per-account lock for external ordering in transfers. */
    public ReentrantLock getLock() { return lock; }

    // -------------------------------------------------------------------------
    // Object identity
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id.equals(account.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Account{id='%s', type=%s, balance=%s, active=%b}", id, type, balance, isActive);
    }
}
