package com.abhishek.banking.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.abhishek.banking.domain.exception.InvalidAmountException;
import com.abhishek.banking.domain.value.Money;

class MoneyTest {

    @Test
    void testCreation_fromBigDecimal_scalesCorrectly() {
        Money money = Money.of(new BigDecimal("10.5"));
        assertEquals("10.50", money.getAmount().toString());
    }

    @Test
    void testCreation_fromString_scalesCorrectly() {
        Money money = Money.of("10.555");
        // HALF_EVEN rounding: 10.555 -> 10.56
        assertEquals("10.56", money.getAmount().toString());
    }

    @Test
    void testCreation_invalidString_throwsException() {
        assertThrows(InvalidAmountException.class, () -> Money.of("abc"));
    }

    @Test
    void testAddition() {
        Money m1 = Money.of("10.50");
        Money m2 = Money.of("20.25");
        Money result = m1.add(m2);
        assertEquals("30.75", result.getAmount().toString());
    }

    @Test
    void testSubtraction() {
        Money m1 = Money.of("20.25");
        Money m2 = Money.of("10.50");
        Money result = m1.subtract(m2);
        assertEquals("9.75", result.getAmount().toString());
    }

    @Test
    void testComparisons() {
        Money m1 = Money.of("10.00");
        Money m2 = Money.of("20.00");
        Money m3 = Money.of("10.00");

        assertTrue(m2.isGreaterThan(m1));
        assertFalse(m1.isGreaterThan(m2));
        assertTrue(m1.isGreaterThanOrEqual(m3));
        
        assertTrue(m1.isPositive());
        assertTrue(Money.of("-5.00").isNegative());
        assertFalse(Money.ZERO.isPositive());
        assertTrue(Money.ZERO.isPositiveOrZero());
        
        assertEquals(m1, m3);
        assertNotEquals(m1, m2);
    }
}
