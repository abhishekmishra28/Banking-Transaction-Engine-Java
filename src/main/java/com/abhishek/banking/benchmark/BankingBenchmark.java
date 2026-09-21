package com.abhishek.banking.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.abhishek.banking.application.service.AccountService;
import com.abhishek.banking.application.service.AuditService;
import com.abhishek.banking.application.service.CustomerService;
import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.concurrency.TransactionEngine;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.Customer;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.infrastructure.database.DatabaseConnectionManager;
import com.abhishek.banking.repository.sqlite.AccountRepositoryImpl;
import com.abhishek.banking.repository.sqlite.AuditLogRepositoryImpl;
import com.abhishek.banking.repository.sqlite.CustomerRepositoryImpl;
import com.abhishek.banking.repository.sqlite.TransactionRepositoryImpl;

/**
 * JMH microbenchmark for the Banking Transaction Engine.
 *
 * <h3>Methodology</h3>
 * <ul>
 *   <li>Uses an in-memory SQLite database to isolate CPU/locking costs from disk I/O.</li>
 *   <li>2 warmup iterations × 2 s, 3 measurement iterations × 3 s, 1 JVM fork.</li>
 *   <li>Each invocation performs a complete transfer: lock acquisition, balance update,
 *       repository update, and audit log.</li>
 * </ul>
 *
 * <p><b>NOTE:</b> Results are environment-specific. Do not compare across machines
 * without identical hardware/JVM settings. Run {@code java -jar ... benchmark} to
 * obtain actual numbers.</p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class BankingBenchmark {

    private TransactionService transactionService;
    private TransactionEngine transactionEngine;
    private Account account1;
    private Account account2;

    @Setup(Level.Trial)
    public void setup() {
        DatabaseConnectionManager dbManager = new DatabaseConnectionManager("jdbc:sqlite::memory:");
        dbManager.initializeSchema();

        CustomerRepositoryImpl customerRepository = new CustomerRepositoryImpl(dbManager);
        CustomerService customerService = new CustomerService(customerRepository);

        AccountRepositoryImpl accountRepository = new AccountRepositoryImpl(dbManager);
        AccountService accountService = new AccountService(accountRepository, customerService);

        AuditService auditService = new AuditService(new AuditLogRepositoryImpl(dbManager));
        transactionService = new TransactionService(
                new TransactionRepositoryImpl(dbManager), accountService, auditService);

        transactionEngine = new TransactionEngine(10, 10_000);

        // Pre-create customers and accounts using the SAME repository instance
        Customer c1 = new Customer("Benchmark-Alice");
        Customer c2 = new Customer("Benchmark-Bob");
        customerRepository.save(c1);
        customerRepository.save(c2);

        account1 = SavingsAccount.createNew(c1.getId(), Money.of("1000000.00"));
        account2 = SavingsAccount.createNew(c2.getId(), Money.of("1000000.00"));
        accountRepository.save(account1);
        accountRepository.save(account2);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        transactionEngine.shutdown();
    }

    /**
     * Measures synchronous transfer throughput (ops/sec) using in-memory SQLite.
     * Alternates direction to avoid one account draining to zero.
     */
    @Benchmark
    public void benchmarkTransfer() {
        // Alternate direction to prevent balance starvation
        transactionService.transfer(account1.getId(), account2.getId(), Money.of("1.00"));
        transactionService.transfer(account2.getId(), account1.getId(), Money.of("1.00"));
    }

    @Benchmark
    public void benchmarkDeposit() {
        transactionService.deposit(account1.getId(), Money.of("1.00"));
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(BankingBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
