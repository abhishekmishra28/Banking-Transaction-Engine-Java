package com.abhishek.banking;

import java.util.Scanner;

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

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java -jar banking-transaction-engine-java.jar <mode>");
            System.out.println("Modes: in-memory-demo, db-demo, server, benchmark");
            return;
        }

        String mode = args[0].toLowerCase();
        
        switch (mode) {
            case "server":
                runServer();
                break;
            case "benchmark":
                runBenchmark(args);
                break;
            case "in-memory-demo":
                System.out.println("In-memory demo not fully implemented as standalone. Run server with in-memory DB.");
                runServer("jdbc:sqlite::memory:");
                break;
            case "db-demo":
                System.out.println("DB demo. Run server with file DB.");
                runServer("jdbc:sqlite:banking.db");
                break;
            default:
                System.out.println("Unknown mode: " + mode);
        }
    }
    
    private static void runServer() {
        runServer("jdbc:sqlite:banking.db"); // default to file db
    }

    private static void runServer(String jdbcUrl) {
        DatabaseConnectionManager dbManager = new DatabaseConnectionManager(jdbcUrl);
        dbManager.initializeSchema();
        
        CustomerService customerService = new CustomerService(new CustomerRepositoryImpl(dbManager));
        AccountService accountService = new AccountService(new AccountRepositoryImpl(dbManager), customerService);
        AuditService auditService = new AuditService(new AuditLogRepositoryImpl(dbManager));
        TransactionService transactionService = new TransactionService(new TransactionRepositoryImpl(dbManager), accountService, auditService);
        
        TransactionEngine engine = new TransactionEngine(10, 1000);
        BankingServer server = new BankingServer(9090, 50, customerService, accountService, transactionService, engine);
        
        server.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            engine.shutdown();
        }));
    }
    
    private static void runBenchmark(String[] args) throws Exception {
        BankingBenchmark.main(args);
    }
}
