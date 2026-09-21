package com.abhishek.banking.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.abhishek.banking.domain.enums.AccountType;
import com.abhishek.banking.domain.exception.InsufficientFundsException;
import com.abhishek.banking.domain.value.Money;

public class CurrentAccount extends Account {
    
    private final Money overdraftLimit;

    public CurrentAccount(String id, String customerId, Money initialBalance, Money overdraftLimit, Instant createdAt, boolean isActive) {
        super(id, customerId, AccountType.CURRENT, initialBalance, createdAt, isActive);
        if (overdraftLimit == null || overdraftLimit.isNegative()) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative");
        }
        this.overdraftLimit = overdraftLimit;
    }
    
    public static CurrentAccount createNew(String customerId, Money initialBalance, Money overdraftLimit) {
        return new CurrentAccount(
            "CUR-" + UUID.randomUUID().toString().substring(0, 8),
            customerId,
            initialBalance,
            overdraftLimit,
            Instant.now(),
            true
        );
    }

    public Money getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    protected void validateWithdrawal(Money amount) throws InsufficientFundsException {
        // Balance can go negative up to the overdraft limit
        Money balanceAfterWithdrawal = this.balance.subtract(amount);
        // We want: balanceAfterWithdrawal >= -overdraftLimit
        // Which is: balanceAfterWithdrawal.add(overdraftLimit) >= 0
        if (balanceAfterWithdrawal.add(overdraftLimit).isNegative()) {
            throw new InsufficientFundsException("Withdrawal exceeds overdraft limit");
        }
    }
}
