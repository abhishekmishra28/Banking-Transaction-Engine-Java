package com.abhishek.banking.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhishek.banking.application.service.AccountService;
import com.abhishek.banking.application.service.AuditService;
import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.testutil.InMemoryRepositories;

/**
 * Concurrency tests verifying deadlock prevention and balance invariants.
 * Uses CountDownLatch to maximise contention and deterministic barriers.
 */
class TransferConcurrencyTest {

    private TransactionService transactionService;
    private InMemoryRepositories.InMemoryAccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryRepositories.InMemoryAccountRepository();
        AccountService accountService = new AccountService(accountRepository, null);
        AuditService auditService = new AuditService(new InMemoryRepositories.InMemoryAuditLogRepository());
        transactionService = new TransactionService(
                new InMemoryRepositories.InMemoryTransactionRepository(), accountService, auditService);
    }

    /**
     * 100 threads transfer acc1→acc2 and 100 threads transfer acc2→acc1 simultaneously.
     * Verifies: no deadlock, total balance conserved.
     */
    @Test
    void testConcurrentTransfers_noDeadlock_balanceConserved() throws InterruptedException {
        Account acc1 = SavingsAccount.createNew("CUST-1", Money.of("1000.00"));
        Account acc2 = SavingsAccount.createNew("CUST-2", Money.of("1000.00"));
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        final int numThreads = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(20);
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(numThreads * 2);
        final AtomicInteger successCount = new AtomicInteger(0);

        // 100 threads: acc1 → acc2
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    transactionService.transfer(acc1.getId(), acc2.getId(), Money.of("1.00"));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // InsufficientFunds expected when balance hits 0
                } finally {
                    done.countDown();
                }
            });
        }

        // 100 threads: acc2 → acc1
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    transactionService.transfer(acc2.getId(), acc1.getId(), Money.of("1.00"));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // InsufficientFunds expected when balance hits 0
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown(); // release all threads simultaneously

        boolean completed = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Deadlock detected: not all threads completed within 30s");

        // Total balance must equal original total
        Money finalTotal = acc1.getBalance().add(acc2.getBalance());
        assertEquals(Money.of("2000.00"), finalTotal,
                "Balance invariant violated! Expected 2000.00 but got " + finalTotal);

        // At least some transfers must have succeeded
        assertTrue(successCount.get() > 0, "No transfers succeeded at all");
    }

    /**
     * Concurrent deposits from multiple threads must not lose any updates.
     */
    @Test
    void testConcurrentDeposits_noLostUpdates() throws InterruptedException {
        Account acc = SavingsAccount.createNew("CUST-1", Money.ZERO);
        accountRepository.save(acc);

        final int numThreads = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    transactionService.deposit(acc.getId(), Money.of("10.00"));
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(done.await(15, TimeUnit.SECONDS), "Concurrent deposits timed out");
        executor.shutdown();

        // All 50 deposits of 10.00 must be reflected
        assertEquals(Money.of("500.00"), acc.getBalance(),
                "Lost update detected! Expected 500.00 but got " + acc.getBalance());
    }
}
