package com.abhishek.banking.network.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abhishek.banking.application.service.AccountService;
import com.abhishek.banking.application.service.CustomerService;
import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.concurrency.TransactionEngine;
import com.abhishek.banking.network.handler.ClientHandler;

public class BankingServer {
    private static final Logger logger = LoggerFactory.getLogger(BankingServer.class);
    
    private final int port;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final TransactionEngine transactionEngine;
    
    private ServerSocket serverSocket;
    private final ExecutorService clientExecutor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public BankingServer(int port, int maxClients, CustomerService customerService, AccountService accountService, TransactionService transactionService, TransactionEngine transactionEngine) {
        this.port = port;
        this.customerService = customerService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.transactionEngine = transactionEngine;
        this.clientExecutor = Executors.newFixedThreadPool(maxClients, runnable -> new Thread(runnable, "ClientHandler-" + System.nanoTime()));
    }

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            try {
                serverSocket = new ServerSocket(port);
                logger.info("Banking Server started on port {}", port);
                
                while (isRunning.get()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logger.info("New client connected: {}", clientSocket.getRemoteSocketAddress());
                        
                        ClientHandler handler = new ClientHandler(clientSocket, customerService, accountService, transactionService, transactionEngine);
                        clientExecutor.submit(handler);
                    } catch (IOException e) {
                        if (isRunning.get()) {
                            logger.error("Error accepting client connection", e);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to start server on port {}", port, e);
            }
        }
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping Banking Server...");
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                logger.error("Error closing server socket", e);
            }
            
            clientExecutor.shutdown();
            try {
                if (!clientExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    clientExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Banking Server stopped.");
        }
    }
}
