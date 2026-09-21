package com.abhishek.banking.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abhishek.banking.domain.enums.AccountType;
import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.model.Account;
import com.abhishek.banking.domain.model.CurrentAccount;
import com.abhishek.banking.domain.model.SavingsAccount;
import com.abhishek.banking.domain.value.Money;
import com.abhishek.banking.infrastructure.database.DatabaseConnectionManager;
import com.abhishek.banking.repository.interfaces.AccountRepository;

public class AccountRepositoryImpl implements AccountRepository {
    private static final Logger logger = LoggerFactory.getLogger(AccountRepositoryImpl.class);
    private final DatabaseConnectionManager dbManager;

    public AccountRepositoryImpl(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(Account account) {
        String sql = "INSERT INTO accounts(id, customer_id, type, balance, is_active, created_at, overdraft_limit) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, account.getId());
            pstmt.setString(2, account.getCustomerId());
            pstmt.setString(3, account.getType().name());
            pstmt.setString(4, account.getBalance().toString());
            pstmt.setInt(5, account.isActive() ? 1 : 0);
            pstmt.setString(6, account.getCreatedAt().toString());
            
            if (account instanceof CurrentAccount ca) {
                pstmt.setString(7, ca.getOverdraftLimit().toString());
            } else {
                pstmt.setNull(7, java.sql.Types.VARCHAR);
            }
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save account {}", account.getId(), e);
            throw new BankingException("Database error saving account", e);
        }
    }

    @Override
    public void update(Account account) {
        String sql = "UPDATE accounts SET balance = ?, is_active = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, account.getBalance().toString());
            pstmt.setInt(2, account.isActive() ? 1 : 0);
            pstmt.setString(3, account.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update account {}", account.getId(), e);
            throw new BankingException("Database error updating account", e);
        }
    }

    @Override
    public Optional<Account> findById(String id) {
        String sql = "SELECT id, customer_id, type, balance, is_active, created_at, overdraft_limit FROM accounts WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAccount(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find account {}", id, e);
            throw new BankingException("Database error finding account", e);
        }
        return Optional.empty();
    }
    
    private Account mapRowToAccount(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String customerId = rs.getString("customer_id");
        AccountType type = AccountType.valueOf(rs.getString("type"));
        Money balance = Money.of(rs.getString("balance"));
        boolean isActive = rs.getInt("is_active") == 1;
        Instant createdAt = Instant.parse(rs.getString("created_at"));
        
        if (type == AccountType.CURRENT) {
            Money overdraftLimit = Money.of(rs.getString("overdraft_limit"));
            return new CurrentAccount(id, customerId, balance, overdraftLimit, createdAt, isActive);
        } else {
            return new SavingsAccount(id, customerId, balance, createdAt, isActive);
        }
    }
}
