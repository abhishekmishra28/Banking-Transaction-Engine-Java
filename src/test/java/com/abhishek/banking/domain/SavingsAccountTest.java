package com.abhishek.banking.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.abhishek.banking.domain.exception.InsufficientFundsException;
import com.abhishek.banking.domain.exception.InvalidAmountException;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.value.Money;

class SavingsAccountTest {

    @Test
    void testDeposit_positiveAmount_increasesBalance() {
        SavingsAccount account = SavingsAccount.createNew("CUST-1", Money.of("100.00"));
        account.deposit(Money.of("50.00"));
        assertEquals(Money.of("150.00"), account.getBalance());
    }

    @Test
    void testDeposit_negativeAmount_throwsException() {
        SavingsAccount account = SavingsAccount.createNew("CUST-1", Money.of("100.00"));
        assertThrows(InvalidAmountException.class, () -> account.deposit(Money.of("-10.00")));
    }

    @Test
    void testWithdraw_sufficientFunds_decreasesBalance() {
        SavingsAccount account = SavingsAccount.createNew("CUST-1", Money.of("100.00"));
        account.withdraw(Money.of("50.00"));
        assertEquals(Money.of("50.00"), account.getBalance());
    }

    @Test
    void testWithdraw_insufficientFunds_throwsException() {
        SavingsAccount account = SavingsAccount.createNew("CUST-1", Money.of("100.00"));
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(Money.of("150.00")));
        assertEquals(Money.of("100.00"), account.getBalance());
    }
}
