package com.abhishek.banking.repository.sqlite;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhishek.banking.domain.model.Customer;
import com.abhishek.banking.infrastructure.database.DatabaseConnectionManager;
import com.abhishek.banking.repository.interfaces.CustomerRepository;

class CustomerRepositoryImplTest {

    private DatabaseConnectionManager dbManager;
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        dbManager = new DatabaseConnectionManager("jdbc:sqlite::memory:");
        dbManager.initializeSchema();
        customerRepository = new CustomerRepositoryImpl(dbManager);
    }

    @Test
    void testSaveAndFindCustomer() {
        Customer customer = new Customer("Test Customer");
        customerRepository.save(customer);
        
        Optional<Customer> retrieved = customerRepository.findById(customer.getId());
        
        assertTrue(retrieved.isPresent());
        assertEquals("Test Customer", retrieved.get().getName());
        assertEquals(customer.getId(), retrieved.get().getId());
        assertEquals(customer.getCreatedAt(), retrieved.get().getCreatedAt());
    }
}
