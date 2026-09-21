package com.abhishek.banking.application.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.AuditLog;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.repository.interfaces.AccountRepository;
import com.abhishek.banking.repository.interfaces.AuditLogRepository;
import com.abhishek.banking.repository.interfaces.TransactionRepository;

class TransactionServiceTest {

    private TransactionService transactionService;
    private AccountService accountService;
    private InMemoryAccountRepository accountRepository;
    private InMemoryTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        transactionRepository = new InMemoryTransactionRepository();
        AuditService auditService = new AuditService(new InMemoryAuditLogRepository());
        
        // Dummy CustomerService since we just bypass it for account creation in this test
        accountService = new AccountService(accountRepository, null); 
        
        transactionService = new TransactionService(transactionRepository, accountService, auditService);
    }

    @Test
    void testDeposit_success() {
        Account acc = SavingsAccount.createNew("CUST-1", Money.ZERO);
        accountRepository.save(acc);
        
        transactionService.deposit(acc.getId(), Money.of("100.00"));
        
        assertEquals(Money.of("100.00"), acc.getBalance());
        assertEquals(1, transactionRepository.transactions.size());
    }

    @Test
    void testTransfer_success() {
        Account source = SavingsAccount.createNew("CUST-1", Money.of("500.00"));
        Account dest = SavingsAccount.createNew("CUST-2", Money.ZERO);
        accountRepository.save(source);
        accountRepository.save(dest);
        
        transactionService.transfer(source.getId(), dest.getId(), Money.of("200.00"));
        
        assertEquals(Money.of("300.00"), source.getBalance());
        assertEquals(Money.of("200.00"), dest.getBalance());
    }
    
    // In-memory stubs
    static class InMemoryAccountRepository implements AccountRepository {
        Map<String, Account> db = new HashMap<>();
        @Override public void save(Account account) { db.put(account.getId(), account); }
        @Override public void update(Account account) { db.put(account.getId(), account); }
        @Override public Optional<Account> findById(String id) { return Optional.ofNullable(db.get(id)); }
    }
    
    static class InMemoryTransactionRepository implements TransactionRepository {
        Map<String, Transaction> transactions = new HashMap<>();
        @Override public void save(Transaction transaction) { transactions.put(transaction.getId(), transaction); }
        @Override public void update(Transaction transaction) { transactions.put(transaction.getId(), transaction); }
        @Override public Optional<Transaction> findById(String id) { return Optional.ofNullable(transactions.get(id)); }
        @Override public List<Transaction> findByAccountId(String accountId, int limit) { return List.of(); }
    }
    
    static class InMemoryAuditLogRepository implements AuditLogRepository {
        @Override public void save(AuditLog log) {}
    }
}
