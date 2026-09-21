package com.abhishek.banking.application.dto;

import java.time.Instant;

public class CustomerDto {
    private final String id;
    private final String name;
    private final Instant createdAt;

    public CustomerDto(String id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
