package com.abhishek.banking.network.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhishek.banking.application.service.AccountService;
import com.abhishek.banking.application.service.AuditService;
import com.abhishek.banking.application.service.CustomerService;
import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.concurrency.TransactionEngine;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.AuditLog;
import com.abhishek.banking.domain.model.Customer;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.repository.interfaces.AccountRepository;
import com.abhishek.banking.repository.interfaces.AuditLogRepository;
import com.abhishek.banking.repository.interfaces.CustomerRepository;
import com.abhishek.banking.repository.interfaces.TransactionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class CommandParserTest {

    private CommandParser commandParser;
    private TransactionEngine transactionEngine;

    @BeforeEach
    void setUp() {
        CustomerRepository cr = new CustomerRepository() {
            Map<String, Customer> db = new HashMap<>();
            @Override public void save(Customer c) { db.put(c.getId(), c); }
            @Override public Optional<Customer> findById(String id) { return Optional.ofNullable(db.get(id)); }
        };
        CustomerService cs = new CustomerService(cr);
        
        AccountRepository ar = new AccountRepository() {
            Map<String, Account> db = new HashMap<>();
            @Override public void save(Account a) { db.put(a.getId(), a); }
            @Override public void update(Account a) { db.put(a.getId(), a); }
            @Override public Optional<Account> findById(String id) { return Optional.ofNullable(db.get(id)); }
        };
        AccountService as = new AccountService(ar, cs);
        
        TransactionRepository tr = new TransactionRepository() {
            @Override public void save(Transaction t) {}
            @Override public void update(Transaction t) {}
            @Override public Optional<Transaction> findById(String id) { return Optional.empty(); }
            @Override public List<Transaction> findByAccountId(String id, int limit) { return List.of(); }
        };
        AuditLogRepository alr = new AuditLogRepository() {
            @Override public void save(AuditLog l) {}
        };
        TransactionService ts = new TransactionService(tr, as, new AuditService(alr));
        
        transactionEngine = new TransactionEngine(2, 10);
        commandParser = new CommandParser(cs, as, ts, transactionEngine);
        
        // Setup some initial data
        Customer c = new Customer("Alice");
        cr.save(c);
        Account a = SavingsAccount.createNew(c.getId(), Money.of("1000.00"));
        ar.save(a);
        
        // Save test IDs for assertions if needed
        System.setProperty("test.customerId", c.getId());
        System.setProperty("test.accountId", a.getId());
    }

    @Test
    void testDeposit_success() {
        String accountId = System.getProperty("test.accountId");
        String response = commandParser.processCommand("DEPOSIT " + accountId + " 500.00");
        assertTrue(response.startsWith("OK Deposit successful"));
        assertTrue(response.contains("1500.00"));
    }

    @Test
    void testInvalidCommand() {
        String response = commandParser.processCommand("UNKNOWN 123");
        assertTrue(response.startsWith("ERR UNKNOWN_COMMAND"));
    }

    @Test
    void testMalformedDeposit() {
        String response = commandParser.processCommand("DEPOSIT 123");
        assertTrue(response.startsWith("ERR BAD_FORMAT"));
    }
}
