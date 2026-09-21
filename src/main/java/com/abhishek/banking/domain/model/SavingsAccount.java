package com.abhishek.banking.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.abhishek.banking.domain.enums.AccountType;
import com.abhishek.banking.domain.exception.InsufficientFundsException;
import com.abhishek.banking.domain.value.Money;

public class SavingsAccount extends Account {
    
    // Savings accounts cannot have a negative balance
    public static final Money MIN_BALANCE = Money.ZERO;

    public SavingsAccount(String id, String customerId, Money initialBalance, Instant createdAt, boolean isActive) {
        super(id, customerId, AccountType.SAVINGS, initialBalance, createdAt, isActive);
    }
    
    public static SavingsAccount createNew(String customerId, Money initialBalance) {
        return new SavingsAccount(
            "SAV-" + UUID.randomUUID().toString().substring(0, 8),
            customerId,
            initialBalance,
            Instant.now(),
            true
        );
    }

    @Override
    protected void validateWithdrawal(Money amount) throws InsufficientFundsException {
        if (this.balance.subtract(amount).isNegative()) {
            throw new InsufficientFundsException("Insufficient funds for savings account withdrawal");
        }
    }
}
