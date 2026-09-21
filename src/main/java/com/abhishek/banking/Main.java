package com.abhishek.banking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abhishek.banking.application.service.AccountService;
import com.abhishek.banking.application.service.AuditService;
import com.abhishek.banking.application.service.CustomerService;
import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.benchmark.BankingBenchmark;
import com.abhishek.banking.concurrency.TransactionEngine;
import com.abhishek.banking.infrastructure.database.DatabaseConnectionManager;
import com.abhishek.banking.network.server.BankingServer;
import com.abhishek.banking.repository.sqlite.AccountRepositoryImpl;
import com.abhishek.banking.repository.sqlite.AuditLogRepositoryImpl;
import com.abhishek.banking.repository.sqlite.CustomerRepositoryImpl;
import com.abhishek.banking.repository.sqlite.TransactionRepositoryImpl;

/**
 * Entry point. Supports five run modes selected via the first CLI argument.
 *
 * <pre>
 *   java -jar banking.jar demo        # in-memory demo (server on :9090)
 *   java -jar banking.jar server      # persistent SQLite server on :9090
 *   java -jar banking.jar benchmark   # JMH benchmark
 * </pre>
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "server":
                logger.info("Starting Banking Server with SQLite persistence...");
                runServer("jdbc:sqlite:banking.db");
                break;
            case "demo":
            case "in-memory-demo":
                logger.info("Starting Banking Server in IN-MEMORY mode...");
                runServer("jdbc:sqlite::memory:");
                break;
            case "db-demo":
                logger.info("Starting Banking Server in FILE-DB mode...");
                runServer("jdbc:sqlite:banking.db");
                break;
            case "benchmark":
                logger.info("Starting JMH Benchmark...");
                BankingBenchmark.main(args);
                break;
            default:
                System.err.println("Unknown mode: " + mode);
                printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Banking Transaction Engine v1.0.0");
        System.out.println();
        System.out.println("Usage: java -jar banking-transaction-engine-java-1.0.0.jar <mode>");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  server        Start TCP server with SQLite persistence (port 9090)");
        System.out.println("  demo          Start TCP server with in-memory SQLite");
        System.out.println("  benchmark     Run JMH throughput benchmarks");
    }

    private static void runServer(String jdbcUrl) {
        DatabaseConnectionManager dbManager = new DatabaseConnectionManager(jdbcUrl);
        dbManager.initializeSchema();

        CustomerService customerService = new CustomerService(new CustomerRepositoryImpl(dbManager));
        AccountService accountService = new AccountService(new AccountRepositoryImpl(dbManager), customerService);
        AuditService auditService = new AuditService(new AuditLogRepositoryImpl(dbManager));
        TransactionService transactionService = new TransactionService(
                new TransactionRepositoryImpl(dbManager), accountService, auditService);

        TransactionEngine engine = new TransactionEngine(10, 1000);
        BankingServer server = new BankingServer(9090, 50, customerService, accountService, transactionService, engine);

        // Shutdown hook for graceful termination (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received...");
            server.stop();
            engine.shutdown();
        }, "shutdown-hook"));

        logger.info("Banking Server ready. Connect with: telnet localhost 9090");
        server.start();
    }
}
