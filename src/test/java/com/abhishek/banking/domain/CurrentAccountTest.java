package com.abhishek.banking.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.abhishek.banking.domain.exception.InsufficientFundsException;
import com.abhishek.banking.domain.model.CurrentAccount;
import com.abhishek.banking.domain.value.Money;

class CurrentAccountTest {

    @Test
    void testWithdraw_withinOverdraft_succeeds() {
        CurrentAccount account = CurrentAccount.createNew("CUST-1", Money.of("100.00"), Money.of("50.00"));
        account.withdraw(Money.of("130.00"));
        assertEquals(Money.of("-30.00"), account.getBalance());
    }

    @Test
    void testWithdraw_exceedsOverdraft_throwsException() {
        CurrentAccount account = CurrentAccount.createNew("CUST-1", Money.of("100.00"), Money.of("50.00"));
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(Money.of("160.00")));
        assertEquals(Money.of("100.00"), account.getBalance()); // Balance should remain unchanged
    }
}
