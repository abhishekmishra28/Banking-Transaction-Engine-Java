package com.abhishek.banking.application.dto;

import java.time.Instant;

import com.abhishek.banking.domain.enums.AccountType;

public class AccountDto {
    private final String id;
    private final String customerId;
    private final AccountType type;
    private final String balance;
    private final Instant createdAt;
    private final boolean active;

    public AccountDto(String id, String customerId, AccountType type, String balance, Instant createdAt, boolean active) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.balance = balance;
        this.createdAt = createdAt;
        this.active = active;
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

    public String getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }
}
