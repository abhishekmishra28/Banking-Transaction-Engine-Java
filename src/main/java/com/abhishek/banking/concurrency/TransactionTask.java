package com.abhishek.banking.concurrency;

import java.util.concurrent.Callable;

import com.abhishek.banking.application.service.TransactionService;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.value.Money;

public class TransactionTask implements Callable<Transaction> {
    
    public enum TaskType {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }
    
    private final TransactionService transactionService;
    private final TaskType type;
    private final String sourceAccountId;
    private final String destinationAccountId;
    private final Money amount;

    public TransactionTask(TransactionService transactionService, TaskType type, String sourceAccountId, String destinationAccountId, Money amount) {
        this.transactionService = transactionService;
        this.type = type;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
    }

    @Override
    public Transaction call() throws Exception {
        switch (type) {
            case DEPOSIT:
                return transactionService.deposit(destinationAccountId, amount);
            case WITHDRAWAL:
                return transactionService.withdraw(sourceAccountId, amount);
            case TRANSFER:
                return transactionService.transfer(sourceAccountId, destinationAccountId, amount);
            default:
                throw new IllegalArgumentException("Unknown task type: " + type);
        }
    }
}
