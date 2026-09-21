package com.abhishek.banking.network.handler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abhishek.banking.application.service.AccountService;
import com.abhishek.banking.application.service.CustomerService;
import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.concurrency.TransactionEngine;
import com.abhishek.banking.network.protocol.CommandParser;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    private final Socket clientSocket;
    private final CommandParser commandParser;

    public ClientHandler(Socket clientSocket, CustomerService customerService, AccountService accountService, TransactionService transactionService, TransactionEngine transactionEngine) {
        this.clientSocket = clientSocket;
        this.commandParser = new CommandParser(customerService, accountService, transactionService, transactionEngine);
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            sendResponse(out, "OK Connected to Java Banking Server. Commands: CREATE_CUSTOMER, OPEN_ACCOUNT, DEPOSIT, WITHDRAW, TRANSFER, BALANCE, ACCOUNT, HISTORY, QUIT");
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String response = commandParser.processCommand(inputLine);
                sendResponse(out, response);
                
                if ("OK Goodbye".equals(response)) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Error handling client connection {}", clientSocket.getRemoteSocketAddress(), e);
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                logger.error("Error closing client socket", e);
            }
            logger.info("Client disconnected: {}", clientSocket.getRemoteSocketAddress());
        }
    }
    
    private void sendResponse(BufferedWriter out, String response) throws IOException {
        out.write(response);
        out.newLine();
        out.flush();
    }
}
