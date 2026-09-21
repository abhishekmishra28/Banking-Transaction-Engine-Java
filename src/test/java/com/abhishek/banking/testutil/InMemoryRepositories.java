package com.abhishek.banking.testutil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.AuditLog;
import com.abhishek.banking.domain.model.Customer;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.repository.interfaces.AccountRepository;
import com.abhishek.banking.repository.interfaces.AuditLogRepository;
import com.abhishek.banking.repository.interfaces.CustomerRepository;
import com.abhishek.banking.repository.interfaces.TransactionRepository;

/**
 * Shared in-memory repository stubs for use in unit and concurrency tests.
 * All map implementations use ConcurrentHashMap for thread-safe access.
 */
public final class InMemoryRepositories {

    private InMemoryRepositories() {}

    public static class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<String, Customer> db = new ConcurrentHashMap<>();

        @Override public void save(Customer customer) { db.put(customer.getId(), customer); }
        @Override public Optional<Customer> findById(String id) { return Optional.ofNullable(db.get(id)); }
    }

    public static class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, Account> db = new ConcurrentHashMap<>();

        @Override public void save(Account account) { db.put(account.getId(), account); }
        @Override public void update(Account account) { db.put(account.getId(), account); }
        @Override public Optional<Account> findById(String id) { return Optional.ofNullable(db.get(id)); }
    }

    public static class InMemoryTransactionRepository implements TransactionRepository {
        private final Map<String, Transaction> db = new ConcurrentHashMap<>();

        @Override public void save(Transaction transaction) { db.put(transaction.getId(), transaction); }
        @Override public void update(Transaction transaction) { db.put(transaction.getId(), transaction); }
        @Override public Optional<Transaction> findById(String id) { return Optional.ofNullable(db.get(id)); }
        @Override public List<Transaction> findByAccountId(String accountId, int limit) {
            return db.values().stream()
                    .filter(t -> accountId.equals(t.getSourceAccountId()) || accountId.equals(t.getDestinationAccountId()))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        }

        public int count() { return db.size(); }
    }

    public static class InMemoryAuditLogRepository implements AuditLogRepository {
        private final List<AuditLog> logs = new ArrayList<>();
        @Override public synchronized void save(AuditLog log) { logs.add(log); }
        public synchronized int count() { return logs.size(); }
    }
}
