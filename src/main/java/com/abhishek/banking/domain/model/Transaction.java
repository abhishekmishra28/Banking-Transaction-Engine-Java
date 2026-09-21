package com.abhishek.banking.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.abhishek.banking.domain.enums.TransactionStatus;
import com.abhishek.banking.domain.enums.TransactionType;
import com.abhishek.banking.domain.value.Money;

public class Transaction {
    private final String id;
    private final TransactionType type;
    private final String sourceAccountId; // Can be null for deposits
    private final String destinationAccountId; // Can be null for withdrawals
    private final Money amount;
    private final Instant timestamp;
    private TransactionStatus status;
    private String failureReason;

    public Transaction(String id, TransactionType type, String sourceAccountId, String destinationAccountId, 
                       Money amount, Instant timestamp, TransactionStatus status, String failureReason) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("Transaction ID required");
        if (type == null) throw new IllegalArgumentException("Transaction type required");
        if (amount == null || !amount.isPositive()) throw new com.abhishek.banking.domain.exception.InvalidAmountException("Transaction amount must be positive");
        
        this.id = id;
        this.type = type;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.status = status != null ? status : TransactionStatus.PENDING;
        this.failureReason = failureReason;
    }
    
    public static Transaction createDeposit(String accountId, Money amount) {
        return new Transaction(UUID.randomUUID().toString(), TransactionType.DEPOSIT, null, accountId, amount, Instant.now(), TransactionStatus.PENDING, null);
    }

    public static Transaction createWithdrawal(String accountId, Money amount) {
        return new Transaction(UUID.randomUUID().toString(), TransactionType.WITHDRAWAL, accountId, null, amount, Instant.now(), TransactionStatus.PENDING, null);
    }
    
    public static Transaction createTransfer(String sourceAccountId, String destinationAccountId, Money amount) {
        return new Transaction(UUID.randomUUID().toString(), TransactionType.TRANSFER, sourceAccountId, destinationAccountId, amount, Instant.now(), TransactionStatus.PENDING, null);
    }

    public String getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public Money getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }
    
    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
    }
    
    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
    }
    
    public void markRejected(String reason) {
        this.status = TransactionStatus.REJECTED;
        this.failureReason = reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
