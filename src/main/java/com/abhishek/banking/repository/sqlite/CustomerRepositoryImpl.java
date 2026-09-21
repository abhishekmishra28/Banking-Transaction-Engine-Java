package com.abhishek.banking.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.model.Customer;
import com.abhishek.banking.infrastructure.database.DatabaseConnectionManager;
import com.abhishek.banking.repository.interfaces.CustomerRepository;

public class CustomerRepositoryImpl implements CustomerRepository {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRepositoryImpl.class);
    private final DatabaseConnectionManager dbManager;

    public CustomerRepositoryImpl(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(Customer customer) {
        String sql = "INSERT INTO customers(id, name, created_at) VALUES(?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, customer.getId());
            pstmt.setString(2, customer.getName());
            pstmt.setString(3, customer.getCreatedAt().toString());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save customer {}", customer.getId(), e);
            throw new BankingException("Database error saving customer", e);
        }
    }

    @Override
    public Optional<Customer> findById(String id) {
        String sql = "SELECT id, name, created_at FROM customers WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Customer customer = new Customer(
                        rs.getString("id"),
                        rs.getString("name"),
                        Instant.parse(rs.getString("created_at"))
                    );
                    return Optional.of(customer);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find customer {}", id, e);
            throw new BankingException("Database error finding customer", e);
        }
        return Optional.empty();
    }
}
