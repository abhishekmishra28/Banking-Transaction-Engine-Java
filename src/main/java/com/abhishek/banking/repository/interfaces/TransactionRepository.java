package com.abhishek.banking.repository.interfaces;

import java.util.List;
import java.util.Optional;

import com.abhishek.banking.domain.model.Transaction;

public interface TransactionRepository {
    void save(Transaction transaction);
    void update(Transaction transaction);
    Optional<Transaction> findById(String id);
    List<Transaction> findByAccountId(String accountId, int limit);
}
