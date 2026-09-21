package com.abhishek.banking.repository.interfaces;

import java.util.Optional;

import com.abhishek.banking.domain.model.Account;

public interface AccountRepository {
    void save(Account account);
    void update(Account account);
    Optional<Account> findById(String id);
}
