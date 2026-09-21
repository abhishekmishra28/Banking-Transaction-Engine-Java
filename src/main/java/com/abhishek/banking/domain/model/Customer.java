package com.abhishek.banking.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Customer {
    private final String id;
    private final String name;
    private final Instant createdAt;

    public Customer(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        this.id = UUID.randomUUID().toString();
        this.name = name.trim();
        this.createdAt = Instant.now();
    }
    
    // Constructor for reconstruction from DB
    public Customer(String id, String name, Instant createdAt) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return id.equals(customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
