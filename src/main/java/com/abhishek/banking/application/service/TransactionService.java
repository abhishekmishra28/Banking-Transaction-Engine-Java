package com.abhishek.banking.application.service;

import java.util.List;

import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.repository.interfaces.TransactionRepository;

/**
 * Orchestrates deposit, withdrawal, and transfer operations.
 *
 * <h3>Concurrency Contract</h3>
 * <ul>
 *   <li>Deposits and withdrawals call the locking methods on Account directly.</li>
 *   <li>Transfers acquire both account locks in a deterministic (lexicographic) order
 *       to prevent deadlocks, then operate on balances via the lock-free internal methods.</li>
 *   <li>Failed transfers roll back the debit before releasing locks.</li>
 *   <li>Transaction records are always persisted (FAILED or COMPLETED) even on exceptions.</li>
 * </ul>
 *
 * <h3>Limitations</h3>
 * <p>In-memory locks and SQLite JDBC transactions are independent. This engine does NOT
 * provide distributed ACID guarantees; it provides correct single-process in-memory
 * semantics. If the JVM crashes mid-transfer, the SQLite state may be inconsistent with
 * the in-memory state.</p>
 */
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final AuditService auditService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountService accountService,
                              AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.auditService = auditService;
    }

    /**
     * Deposits {@code amount} into the account identified by {@code accountId}.
     *
     * @return the completed or failed Transaction record
     */
    public Transaction deposit(String accountId, Money amount) {
        Account account = accountService.getAccountEntity(accountId);
        Transaction tx = Transaction.createDeposit(accountId, amount);

        try {
            account.deposit(amount);
            tx.markCompleted();
            accountService.updateAccount(account);
            auditService.logEvent("DEPOSIT",
                    "Deposited " + amount + " to account " + accountId, accountId);
        } catch (RuntimeException e) {
            tx.markFailed(e.getMessage());
            throw e;
        } finally {
            transactionRepository.save(tx);
        }
        return tx;
    }

    /**
     * Withdraws {@code amount} from the account identified by {@code accountId}.
     *
     * @return the completed or failed Transaction record
     */
    public Transaction withdraw(String accountId, Money amount) {
        Account account = accountService.getAccountEntity(accountId);
        Transaction tx = Transaction.createWithdrawal(accountId, amount);

        try {
            account.withdraw(amount);
            tx.markCompleted();
            accountService.updateAccount(account);
            auditService.logEvent("WITHDRAWAL",
                    "Withdrew " + amount + " from account " + accountId, accountId);
        } catch (RuntimeException e) {
            tx.markFailed(e.getMessage());
            throw e;
        } finally {
            transactionRepository.save(tx);
        }
        return tx;
    }

    /**
     * Transfers {@code amount} from {@code sourceAccountId} to {@code destinationAccountId}.
     *
     * <p><b>Deadlock prevention:</b> Both account locks are acquired in lexicographic order
     * of account ID, regardless of transfer direction. This ensures that any two concurrent
     * transfers between the same pair of accounts (in either direction) always contend on
     * the same lock in the same order.</p>
     *
     * <p><b>Rollback:</b> If the credit fails after a successful debit, the debit is
     * reversed before the lock is released, preserving balance invariants.</p>
     *
     * @return the completed or failed Transaction record
     * @throws BankingException if source equals destination, or domain rules are violated
     */
    public Transaction transfer(String sourceAccountId, String destinationAccountId, Money amount) {
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new BankingException("Cannot transfer to the same account");
        }

        Account source = accountService.getAccountEntity(sourceAccountId);
        Account destination = accountService.getAccountEntity(destinationAccountId);

        Transaction tx = Transaction.createTransfer(sourceAccountId, destinationAccountId, amount);

        // Deterministic lock ordering: always acquire smaller-ID account lock first
        boolean srcFirst = sourceAccountId.compareTo(destinationAccountId) < 0;
        Account firstLock  = srcFirst ? source : destination;
        Account secondLock = srcFirst ? destination : source;

        firstLock.getLock().lock();
        try {
            secondLock.getLock().lock();
            try {
                // Both locks held; use internal (no-lock) methods to avoid re-entry overhead
                source.debitInternal(amount);
                try {
                    destination.creditInternal(amount);
                } catch (RuntimeException creditEx) {
                    // Rollback debit to preserve balance invariant
                    source.creditInternal(amount);
                    throw creditEx;
                }

                tx.markCompleted();
                accountService.updateAccount(source);
                accountService.updateAccount(destination);
                auditService.logEvent("TRANSFER",
                        "Transferred " + amount + " from " + sourceAccountId + " to " + destinationAccountId,
                        tx.getId());
            } catch (RuntimeException e) {
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

    /**
     * Returns the most recent {@code limit} transactions involving the given account.
     */
    public List<Transaction> getHistory(String accountId, int limit) {
        return transactionRepository.findByAccountId(accountId, limit);
    }
}
