package com.abhishek.banking.network.protocol;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abhishek.banking.application.dto.AccountDto;
import com.abhishek.banking.application.dto.CustomerDto;
import com.abhishek.banking.application.service.AccountService;
import com.abhishek.banking.application.service.CustomerService;
import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.concurrency.TransactionEngine;
import com.abhishek.banking.concurrency.TransactionTask;
import com.abhishek.banking.domain.enums.AccountType;
import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.value.Money;

public class CommandParser {
    private static final Logger logger = LoggerFactory.getLogger(CommandParser.class);
    
    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final TransactionEngine transactionEngine;

    public CommandParser(CustomerService customerService, AccountService accountService, TransactionService transactionService, TransactionEngine transactionEngine) {
        this.customerService = customerService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.transactionEngine = transactionEngine;
    }

    public String processCommand(String inputLine) {
        if (inputLine == null || inputLine.trim().isEmpty()) {
            return "ERR INVALID_COMMAND Empty command";
        }
        
        String[] parts = inputLine.trim().split("\\s+");
        String command = parts[0].toUpperCase();
        
        try {
            switch (command) {
                case "CREATE_CUSTOMER":
                    return handleCreateCustomer(parts, inputLine);
                case "OPEN_ACCOUNT":
                    return handleOpenAccount(parts);
                case "DEPOSIT":
                    return handleDeposit(parts);
                case "WITHDRAW":
                    return handleWithdraw(parts);
                case "TRANSFER":
                    return handleTransfer(parts);
                case "BALANCE":
                    return handleBalance(parts);
                case "ACCOUNT":
                    return handleAccount(parts);
                case "HISTORY":
                    return handleHistory(parts);
                case "QUIT":
                    return "OK Goodbye";
                default:
                    return "ERR UNKNOWN_COMMAND Command not recognized: " + command;
            }
        } catch (BankingException e) {
            return "ERR DOMAIN_ERROR " + e.getMessage();
        } catch (IllegalArgumentException e) {
            return "ERR BAD_REQUEST " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error processing command: {}", inputLine, e);
            return "ERR INTERNAL_ERROR An unexpected error occurred";
        }
    }

    private String handleCreateCustomer(String[] parts, String inputLine) {
        if (parts.length < 2) return "ERR BAD_FORMAT Usage: CREATE_CUSTOMER <name>";
        // Reconstruct name in case of spaces
        String name = inputLine.substring("CREATE_CUSTOMER".length()).trim();
        CustomerDto dto = customerService.createCustomer(name);
        return "OK Customer created. ID: " + dto.getId();
    }

    private String handleOpenAccount(String[] parts) {
        if (parts.length != 4) return "ERR BAD_FORMAT Usage: OPEN_ACCOUNT <customer_id> <SAVINGS|CURRENT> <initial_deposit>";
        String customerId = parts[1];
        AccountType type = AccountType.valueOf(parts[2].toUpperCase());
        Money initialDeposit = Money.of(parts[3]);
        
        AccountDto dto = accountService.openAccount(customerId, type, initialDeposit);
        return "OK Account opened. ID: " + dto.getId();
    }

    private String handleDeposit(String[] parts) throws Exception {
        if (parts.length != 3) return "ERR BAD_FORMAT Usage: DEPOSIT <account_id> <amount>";
        String accountId = parts[1];
        Money amount = Money.of(parts[2]);
        
        Future<Transaction> future = transactionEngine.submit(new TransactionTask(transactionService, TransactionTask.TaskType.DEPOSIT, null, accountId, amount));
        Transaction tx = future.get(); // Blocking wait for transaction
        return "OK Deposit successful. Transaction ID: " + tx.getId() + " Balance: " + accountService.getBalance(accountId);
    }
    
    private String handleWithdraw(String[] parts) throws Exception {
        if (parts.length != 3) return "ERR BAD_FORMAT Usage: WITHDRAW <account_id> <amount>";
        String accountId = parts[1];
        Money amount = Money.of(parts[2]);
        
        Future<Transaction> future = transactionEngine.submit(new TransactionTask(transactionService, TransactionTask.TaskType.WITHDRAWAL, accountId, null, amount));
        Transaction tx = future.get();
        return "OK Withdrawal successful. Transaction ID: " + tx.getId() + " Balance: " + accountService.getBalance(accountId);
    }

    private String handleTransfer(String[] parts) throws Exception {
        if (parts.length != 4) return "ERR BAD_FORMAT Usage: TRANSFER <from_id> <to_id> <amount>";
        String fromId = parts[1];
        String toId = parts[2];
        Money amount = Money.of(parts[3]);
        
        Future<Transaction> future = transactionEngine.submit(new TransactionTask(transactionService, TransactionTask.TaskType.TRANSFER, fromId, toId, amount));
        Transaction tx = future.get();
        return "OK Transfer completed. Transaction ID: " + tx.getId();
    }

    private String handleBalance(String[] parts) {
        if (parts.length != 2) return "ERR BAD_FORMAT Usage: BALANCE <account_id>";
        String accountId = parts[1];
        String balance = accountService.getBalance(accountId);
        return "OK Balance: " + balance;
    }

    private String handleAccount(String[] parts) {
        if (parts.length != 2) return "ERR BAD_FORMAT Usage: ACCOUNT <account_id>";
        String accountId = parts[1];
        AccountDto dto = accountService.getAccountDetails(accountId);
        return String.format("OK Account %s: Type=%s, Balance=%s, Customer=%s", dto.getId(), dto.getType(), dto.getBalance(), dto.getCustomerId());
    }

    private String handleHistory(String[] parts) {
        if (parts.length != 3) return "ERR BAD_FORMAT Usage: HISTORY <account_id> <limit>";
        String accountId = parts[1];
        int limit;
        try {
            limit = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return "ERR BAD_FORMAT Limit must be a number";
        }
        
        List<Transaction> history = transactionService.getHistory(accountId, limit);
        if (history.isEmpty()) return "OK No transactions found.";
        
        StringBuilder sb = new StringBuilder("OK History:\n");
        for (Transaction t : history) {
            sb.append(String.format("  [%s] %s %s - %s %s\n", t.getTimestamp(), t.getType(), t.getAmount(), t.getStatus(), t.getFailureReason() != null ? t.getFailureReason() : ""));
        }
        return sb.toString().trim();
    }
}
