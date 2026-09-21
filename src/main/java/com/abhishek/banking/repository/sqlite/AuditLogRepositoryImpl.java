package com.abhishek.banking.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abhishek.banking.domain.exception.BankingException;
import com.abhishek.banking.domain.model.AuditLog;
import com.abhishek.banking.infrastructure.database.DatabaseConnectionManager;
import com.abhishek.banking.repository.interfaces.AuditLogRepository;

public class AuditLogRepositoryImpl implements AuditLogRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogRepositoryImpl.class);
    private final DatabaseConnectionManager dbManager;

    public AuditLogRepositoryImpl(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(AuditLog log) {
        String sql = "INSERT INTO audit_logs(id, event_type, description, entity_id, timestamp) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, log.getId());
            pstmt.setString(2, log.getEventType());
            pstmt.setString(3, log.getDescription());
            pstmt.setString(4, log.getEntityId());
            pstmt.setString(5, log.getTimestamp().toString());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save audit log {}", log.getId(), e);
            // We usually don't want to throw an exception that fails a transaction just for an audit log failure,
            // but for simplicity and strictness in this assignment we will.
            throw new BankingException("Database error saving audit log", e);
        }
    }
}
