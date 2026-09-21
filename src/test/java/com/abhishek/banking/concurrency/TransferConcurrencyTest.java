package com.abhishek.banking.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhishek.banking.application.service.AccountService;
import com.abhishek.banking.application.service.AuditService;
import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.application.service.TransactionServiceTest;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.value.Money;

/**
 * Note: Uses the InMemory repositories from TransactionServiceTest for isolation.
 */
class TransferConcurrencyTest {

    private TransactionService transactionService;
    private TransactionServiceTest.InMemoryAccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository = new TransactionServiceTest.InMemoryAccountRepository();
        AccountService accountService = new AccountService(accountRepository, null);
        AuditService auditService = new AuditService(new TransactionServiceTest.InMemoryAuditLogRepository());
        transactionService = new TransactionService(new TransactionServiceTest.InMemoryTransactionRepository(), accountService, auditService);
    }

    @Test
    void testConcurrentTransfers_preventDeadlock() throws InterruptedException {
        Account acc1 = SavingsAccount.createNew("CUST-1", Money.of("1000.00"));
        Account acc2 = SavingsAccount.createNew("CUST-2", Money.of("1000.00"));
        accountRepository.save(acc1);
        accountRepository.save(acc2);
        
        int numThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(numThreads * 2);
        
        // 100 threads transfer 10 from acc1 to acc2
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    transactionService.transfer(acc1.getId(), acc2.getId(), Money.of("10.00"));
                } catch (Exception e) {
                    // Ignore expected insufficient funds if they happen
                } finally {
                    done.countDown();
                }
            });
        }
        
        // 100 threads transfer 10 from acc2 to acc1
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    transactionService.transfer(acc2.getId(), acc1.getId(), Money.of("10.00"));
                } catch (Exception e) {
                    // Ignore expected insufficient funds if they happen
                } finally {
                    done.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        latch.countDown();
        
        // Wait for all to finish
        assertTrue(done.await(30, TimeUnit.SECONDS), "Deadlock occurred, timeout waiting for transfers");
        executor.shutdown();
        
        // Total balance should remain 2000
        Money finalTotal = acc1.getBalance().add(acc2.getBalance());
        assertEquals(Money.of("2000.00"), finalTotal);
    }
}
