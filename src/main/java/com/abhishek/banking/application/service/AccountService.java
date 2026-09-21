package com.abhishek.banking.application.service;

import com.abhishek.banking.application.dto.AccountDto;
import com.abhishek.banking.domain.enums.AccountType;
import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.CurrentAccount;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.repository.interfaces.AccountRepository;

public class AccountService {
    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    public AccountService(AccountRepository accountRepository, CustomerService customerService) {
        this.accountRepository = accountRepository;
        this.customerService = customerService;
    }

    public AccountDto openAccount(String customerId, AccountType type, Money initialDeposit) {
        // Validate customer exists
        customerService.findCustomer(customerId)
            .orElseThrow(() -> new BankingException("Customer not found: " + customerId));

        if (initialDeposit == null || initialDeposit.isNegative()) {
            throw new IllegalArgumentException("Initial deposit cannot be negative");
        }

        Account account;
        if (type == AccountType.SAVINGS) {
            account = SavingsAccount.createNew(customerId, initialDeposit);
        } else if (type == AccountType.CURRENT) {
            // Default overdraft limit for current account, e.g., 1000
            account = CurrentAccount.createNew(customerId, initialDeposit, Money.of("1000.00"));
        } else {
            throw new IllegalArgumentException("Unsupported account type");
        }

        accountRepository.save(account);
        return mapToDto(account);
    }

    public AccountDto getAccountDetails(String accountId) {
        Account account = getAccountEntity(accountId);
        return mapToDto(account);
    }

    public String getBalance(String accountId) {
        Account account = getAccountEntity(accountId);
        return account.getBalance().toString();
    }
    
    public Account getAccountEntity(String accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new BankingException("Account not found: " + accountId));
    }
    
    public void updateAccount(Account account) {
        accountRepository.update(account);
    }

    private AccountDto mapToDto(Account account) {
        return new AccountDto(
            account.getId(),
            account.getCustomerId(),
            account.getType(),
            account.getBalance().toString(),
            account.getCreatedAt(),
            account.isActive()
        );
    }
}
