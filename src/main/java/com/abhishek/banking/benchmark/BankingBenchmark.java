package com.abhishek.banking.benchmark;

import org.openjdk.jmh.annotations.*;
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;

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
        // Use in-memory SQLite for benchmarking to avoid disk I/O bottlenecks in pure throughput tests
        DatabaseConnectionManager dbManager = new DatabaseConnectionManager("jdbc:sqlite::memory:");
        dbManager.initializeSchema();
        
        CustomerService customerService = new CustomerService(new CustomerRepositoryImpl(dbManager));
        AccountRepositoryImpl accountRepository = new AccountRepositoryImpl(dbManager);
        AccountService accountService = new AccountService(accountRepository, customerService);
        AuditService auditService = new AuditService(new AuditLogRepositoryImpl(dbManager));
        
        transactionService = new TransactionService(new TransactionRepositoryImpl(dbManager), accountService, auditService);
        transactionEngine = new TransactionEngine(10, 10000);
        
        // Setup mock data
        Customer c1 = new Customer("Alice Benchmark");
        Customer c2 = new Customer("Bob Benchmark");
        new CustomerRepositoryImpl(dbManager).save(c1);
        new CustomerRepositoryImpl(dbManager).save(c2);
        
        account1 = SavingsAccount.createNew(c1.getId(), Money.of("1000000.00"));
        account2 = SavingsAccount.createNew(c2.getId(), Money.of("1000000.00"));
        accountRepository.save(account1);
        accountRepository.save(account2);
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        transactionEngine.shutdown();
    }

    @Benchmark
    public void benchmarkTransfer() throws ExecutionException, InterruptedException {
        // Direct call to TransactionService to measure pure domain logic + SQLite in-memory overhead
        transactionService.transfer(account1.getId(), account2.getId(), Money.of("1.00"));
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(BankingBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
