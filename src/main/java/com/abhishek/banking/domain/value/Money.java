package com.abhishek.banking.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.abhishek.banking.domain.exception.InvalidAmountException;

/**
 * Immutable value object representing money.
 * Encapsulates BigDecimal to ensure consistent scaling and rounding.
 * Uses scale 2 and RoundingMode.HALF_EVEN for financial calculations.
 */
public final class Money implements Comparable<Money> {
    
    public static final Money ZERO = new Money(BigDecimal.ZERO);
    
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        // Enforce exact scale of 2
        this.amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        return new Money(amount);
    }

    public static Money of(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount string cannot be null or empty");
        }
        try {
            return new Money(new BigDecimal(amountStr));
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Invalid amount format: " + amountStr);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isPositiveOrZero() {
        return this.amount.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isGreaterThan(Money other) {
        return this.compareTo(other) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.compareTo(other) >= 0;
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
