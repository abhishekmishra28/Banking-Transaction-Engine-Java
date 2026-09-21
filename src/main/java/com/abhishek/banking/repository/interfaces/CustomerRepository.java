package com.abhishek.banking.repository.interfaces;

import java.util.Optional;

import com.abhishek.banking.domain.model.Customer;

public interface CustomerRepository {
    void save(Customer customer);
    Optional<Customer> findById(String id);
}
