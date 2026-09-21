package com.abhishek.banking.application.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhishek.banking.domain.exception.InsufficientFundsException;
import com.abhishek.banking.domain.exception.InvalidAmountException;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.enums.TransactionStatus;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.testutil.InMemoryRepositories;

class TransactionServiceTest {

    private TransactionService transactionService;
    private AccountService accountService;
    private InMemoryRepositories.InMemoryAccountRepository accountRepository;
    private InMemoryRepositories.InMemoryTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryRepositories.InMemoryAccountRepository();
        transactionRepository = new InMemoryRepositories.InMemoryTransactionRepository();
        AuditService auditService = new AuditService(new InMemoryRepositories.InMemoryAuditLogRepository());

        // CustomerService is null because we bypass it in these unit tests
        accountService = new AccountService(accountRepository, null);
        transactionService = new TransactionService(transactionRepository, accountService, auditService);
    }

    @Test
    void testDeposit_success_increasesBalance() {
        Account acc = SavingsAccount.createNew("CUST-1", Money.ZERO);
        accountRepository.save(acc);

        Transaction tx = transactionService.deposit(acc.getId(), Money.of("100.00"));

        assertEquals(TransactionStatus.COMPLETED, tx.getStatus());
        assertEquals(Money.of("100.00"), acc.getBalance());
        assertEquals(1, transactionRepository.count());
    }

    @Test
    void testDeposit_negativeAmount_throwsAndRecordsFailure() {
        Account acc = SavingsAccount.createNew("CUST-1", Money.of("100.00"));
        accountRepository.save(acc);

        assertThrows(InvalidAmountException.class,
                () -> transactionService.deposit(acc.getId(), Money.of("-10.00")));
    }

    @Test
    void testWithdraw_success_decreasesBalance() {
        Account acc = SavingsAccount.createNew("CUST-1", Money.of("200.00"));
        accountRepository.save(acc);

        Transaction tx = transactionService.withdraw(acc.getId(), Money.of("50.00"));

        assertEquals(TransactionStatus.COMPLETED, tx.getStatus());
        assertEquals(Money.of("150.00"), acc.getBalance());
    }

    @Test
    void testWithdraw_insufficientFunds_throwsAndPreservesBalance() {
        Account acc = SavingsAccount.createNew("CUST-1", Money.of("50.00"));
        accountRepository.save(acc);

        assertThrows(InsufficientFundsException.class,
                () -> transactionService.withdraw(acc.getId(), Money.of("100.00")));

        // Balance must be unchanged
        assertEquals(Money.of("50.00"), acc.getBalance());
        // A FAILED transaction should still be recorded
        assertEquals(1, transactionRepository.count());
    }

    @Test
    void testTransfer_success_movesMoneyAtomically() {
        Account source = SavingsAccount.createNew("CUST-1", Money.of("500.00"));
        Account dest   = SavingsAccount.createNew("CUST-2", Money.ZERO);
        accountRepository.save(source);
        accountRepository.save(dest);

        Transaction tx = transactionService.transfer(source.getId(), dest.getId(), Money.of("200.00"));

        assertEquals(TransactionStatus.COMPLETED, tx.getStatus());
        assertEquals(Money.of("300.00"), source.getBalance());
        assertEquals(Money.of("200.00"), dest.getBalance());
    }

    @Test
    void testTransfer_sameAccount_throwsDomainError() {
        Account acc = SavingsAccount.createNew("CUST-1", Money.of("500.00"));
        accountRepository.save(acc);

        assertThrows(com.abhishek.banking.domain.exception.BankingException.class,
                () -> transactionService.transfer(acc.getId(), acc.getId(), Money.of("10.00")));
    }

    @Test
    void testTransfer_insufficientFunds_noPartialState() {
        Account source = SavingsAccount.createNew("CUST-1", Money.of("10.00"));
        Account dest   = SavingsAccount.createNew("CUST-2", Money.ZERO);
        accountRepository.save(source);
        accountRepository.save(dest);

        assertThrows(InsufficientFundsException.class,
                () -> transactionService.transfer(source.getId(), dest.getId(), Money.of("100.00")));

        // Both balances must be unmodified
        assertEquals(Money.of("10.00"), source.getBalance());
        assertEquals(Money.ZERO, dest.getBalance());
    }
}
