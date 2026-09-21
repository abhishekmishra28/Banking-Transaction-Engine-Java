package com.abhishek.banking.application.service;

import java.util.Optional;

import com.abhishek.banking.application.dto.CustomerDto;
import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.model.Customer;
import com.abhishek.banking.repository.interfaces.CustomerRepository;

public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerDto createCustomer(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        
        Customer customer = new Customer(name);
        customerRepository.save(customer);
        return mapToDto(customer);
    }
    
    public CustomerDto getCustomer(String id) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new BankingException("Customer not found: " + id));
        return mapToDto(customer);
    }
    
    public Optional<Customer> findCustomer(String id) {
        return customerRepository.findById(id);
    }

    private CustomerDto mapToDto(Customer customer) {
        return new CustomerDto(customer.getId(), customer.getName(), customer.getCreatedAt());
    }
}
