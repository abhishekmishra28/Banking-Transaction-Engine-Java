package com.abhishek.banking.network.protocol;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhishek.banking.application.service.AccountService;
import com.abhishek.banking.application.service.AuditService;
import com.abhishek.banking.application.service.CustomerService;
import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.concurrency.TransactionEngine;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.Customer;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.testutil.InMemoryRepositories;

class CommandParserTest {

    private CommandParser commandParser;
    private TransactionEngine transactionEngine;
    private String testAccountId;

    @BeforeEach
    void setUp() {
        InMemoryRepositories.InMemoryCustomerRepository cr = new InMemoryRepositories.InMemoryCustomerRepository();
        CustomerService cs = new CustomerService(cr);

        InMemoryRepositories.InMemoryAccountRepository ar = new InMemoryRepositories.InMemoryAccountRepository();
        AccountService as = new AccountService(ar, cs);

        InMemoryRepositories.InMemoryTransactionRepository tr = new InMemoryRepositories.InMemoryTransactionRepository();
        AuditService auditService = new AuditService(new InMemoryRepositories.InMemoryAuditLogRepository());
        TransactionService ts = new TransactionService(tr, as, auditService);

        transactionEngine = new TransactionEngine(2, 10);
        commandParser = new CommandParser(cs, as, ts, transactionEngine);

        // Seed test data
        Customer c = new Customer("Alice");
        cr.save(c);
        Account a = SavingsAccount.createNew(c.getId(), Money.of("1000.00"));
        ar.save(a);
        testAccountId = a.getId();
    }

    @AfterEach
    void tearDown() {
        transactionEngine.shutdown();
    }

    @Test
    void testBalance_returnsCorrectBalance() {
        String response = commandParser.processCommand("BALANCE " + testAccountId);
        assertTrue(response.startsWith("OK Balance:"), "Expected OK Balance: but got: " + response);
        assertTrue(response.contains("1000.00"));
    }

    @Test
    void testDeposit_success_updatesBalance() {
        String response = commandParser.processCommand("DEPOSIT " + testAccountId + " 500.00");
        assertTrue(response.startsWith("OK Deposit successful"), "Expected OK Deposit but got: " + response);
        assertTrue(response.contains("1500.00"));
    }

    @Test
    void testWithdraw_success() {
        String response = commandParser.processCommand("WITHDRAW " + testAccountId + " 200.00");
        assertTrue(response.startsWith("OK Withdrawal successful"), "Expected OK Withdrawal but got: " + response);
        assertTrue(response.contains("800.00"));
    }

    @Test
    void testWithdraw_insufficientFunds_returnsError() {
        String response = commandParser.processCommand("WITHDRAW " + testAccountId + " 99999.00");
        assertTrue(response.startsWith("ERR"), "Expected ERR but got: " + response);
    }

    @Test
    void testInvalidCommand_returnsUnknownError() {
        String response = commandParser.processCommand("UNKNOWN 123");
        assertTrue(response.startsWith("ERR UNKNOWN_COMMAND"));
    }

    @Test
    void testMalformedDeposit_returnsFormatError() {
        String response = commandParser.processCommand("DEPOSIT 123");
        assertTrue(response.startsWith("ERR BAD_FORMAT"));
    }

    @Test
    void testEmptyCommand_returnsInvalidCommandError() {
        String response = commandParser.processCommand("   ");
        assertTrue(response.startsWith("ERR INVALID_COMMAND"));
    }

    @Test
    void testQuit_returnsGoodbye() {
        String response = commandParser.processCommand("QUIT");
        assertEquals("OK Goodbye", response);
    }

    @Test
    void testCreateCustomer_success() {
        String response = commandParser.processCommand("CREATE_CUSTOMER Bob Builder");
        assertTrue(response.startsWith("OK Customer created. ID:"), "Expected OK Customer but got: " + response);
    }

    @Test
    void testAccountDetails_returnsInfo() {
        String response = commandParser.processCommand("ACCOUNT " + testAccountId);
        assertTrue(response.startsWith("OK Account"), "Expected OK Account but got: " + response);
        assertTrue(response.contains("SAVINGS"));
    }
}
