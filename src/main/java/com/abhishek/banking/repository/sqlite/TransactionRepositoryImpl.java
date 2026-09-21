package com.abhishek.banking.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abhishek.banking.domain.enums.TransactionStatus;
import com.abhishek.banking.domain.enums.TransactionType;
import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.model.Transaction;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.infrastructure.database.DatabaseConnectionManager;
import com.abhishek.banking.repository.interfaces.TransactionRepository;

public class TransactionRepositoryImpl implements TransactionRepository {
    private static final Logger logger = LoggerFactory.getLogger(TransactionRepositoryImpl.class);
    private final DatabaseConnectionManager dbManager;

    public TransactionRepositoryImpl(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(Transaction transaction) {
        String sql = "INSERT INTO transactions(id, type, source_account_id, destination_account_id, amount, status, failure_reason, timestamp) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, transaction.getId());
            pstmt.setString(2, transaction.getType().name());
            pstmt.setString(3, transaction.getSourceAccountId());
            pstmt.setString(4, transaction.getDestinationAccountId());
            pstmt.setString(5, transaction.getAmount().toString());
            pstmt.setString(6, transaction.getStatus().name());
            pstmt.setString(7, transaction.getFailureReason());
            pstmt.setString(8, transaction.getTimestamp().toString());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save transaction {}", transaction.getId(), e);
            throw new BankingException("Database error saving transaction", e);
        }
    }

    @Override
    public void update(Transaction transaction) {
        String sql = "UPDATE transactions SET status = ?, failure_reason = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, transaction.getStatus().name());
            pstmt.setString(2, transaction.getFailureReason());
            pstmt.setString(3, transaction.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update transaction {}", transaction.getId(), e);
            throw new BankingException("Database error updating transaction", e);
        }
    }

    @Override
    public Optional<Transaction> findById(String id) {
        String sql = "SELECT id, type, source_account_id, destination_account_id, amount, status, failure_reason, timestamp FROM transactions WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find transaction {}", id, e);
            throw new BankingException("Database error finding transaction", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Transaction> findByAccountId(String accountId, int limit) {
        String sql = "SELECT id, type, source_account_id, destination_account_id, amount, status, failure_reason, timestamp FROM transactions " +
                     "WHERE source_account_id = ? OR destination_account_id = ? ORDER BY timestamp DESC LIMIT ?";
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, accountId);
            pstmt.setString(2, accountId);
            pstmt.setInt(3, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find transactions for account {}", accountId, e);
            throw new BankingException("Database error finding transactions", e);
        }
        return transactions;
    }
    
    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
            rs.getString("id"),
            TransactionType.valueOf(rs.getString("type")),
            rs.getString("source_account_id"),
            rs.getString("destination_account_id"),
            Money.of(rs.getString("amount")),
            Instant.parse(rs.getString("timestamp")),
            TransactionStatus.valueOf(rs.getString("status")),
            rs.getString("failure_reason")
        );
    }
}
