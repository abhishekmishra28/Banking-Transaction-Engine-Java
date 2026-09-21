package com.abhishek.banking.application.service;

import java.util.List;

import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.repository.interfaces.TransactionRepository;

public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final AuditService auditService;

    public TransactionService(TransactionRepository transactionRepository, AccountService accountService, AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.auditService = auditService;
    }

    public Transaction deposit(String accountId, Money amount) {
        Account account = accountService.getAccountEntity(accountId);
        Transaction tx = Transaction.createDeposit(accountId, amount);
        
        try {
            account.deposit(amount);
            tx.markCompleted();
            accountService.updateAccount(account);
            auditService.logEvent("DEPOSIT", "Deposited " + amount + " to " + accountId, accountId);
        } catch (Exception e) {
            tx.markFailed(e.getMessage());
            throw e;
        } finally {
            transactionRepository.save(tx);
        }
        return tx;
    }

    public Transaction withdraw(String accountId, Money amount) {
        Account account = accountService.getAccountEntity(accountId);
        Transaction tx = Transaction.createWithdrawal(accountId, amount);
        
        try {
            account.withdraw(amount);
            tx.markCompleted();
            accountService.updateAccount(account);
            auditService.logEvent("WITHDRAWAL", "Withdrew " + amount + " from " + accountId, accountId);
        } catch (Exception e) {
            tx.markFailed(e.getMessage());
            throw e;
        } finally {
            transactionRepository.save(tx);
        }
        return tx;
    }

    public Transaction transfer(String sourceAccountId, String destinationAccountId, Money amount) {
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new BankingException("Cannot transfer to the same account");
        }

        Account source = accountService.getAccountEntity(sourceAccountId);
        Account destination = accountService.getAccountEntity(destinationAccountId);
        
        Transaction tx = Transaction.createTransfer(sourceAccountId, destinationAccountId, amount);

        // Deterministic lock ordering to prevent deadlocks
        Account firstLock = sourceAccountId.compareTo(destinationAccountId) < 0 ? source : destination;
        Account secondLock = sourceAccountId.compareTo(destinationAccountId) < 0 ? destination : source;

        firstLock.getLock().lock();
        try {
            secondLock.getLock().lock();
            try {
                source.withdraw(amount);
                try {
                    destination.deposit(amount);
                } catch (Exception e) {
                    // This shouldn't happen for valid deposits, but if it does, rollback withdrawal
                    source.deposit(amount); // rollback
                    throw e;
                }
                
                tx.markCompleted();
                accountService.updateAccount(source);
                accountService.updateAccount(destination);
                auditService.logEvent("TRANSFER", "Transferred " + amount + " from " + sourceAccountId + " to " + destinationAccountId, tx.getId());
            } catch (Exception e) {
                tx.markFailed(e.getMessage());
                throw e;
            } finally {
                secondLock.getLock().unlock();
            }
        } finally {
            firstLock.getLock().unlock();
            transactionRepository.save(tx);
        }
        return tx;
    }
    
    public List<Transaction> getHistory(String accountId, int limit) {
        return transactionRepository.findByAccountId(accountId, limit);
    }
}
