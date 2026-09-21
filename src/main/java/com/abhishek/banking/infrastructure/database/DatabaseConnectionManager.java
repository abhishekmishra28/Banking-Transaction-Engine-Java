package com.abhishek.banking.infrastructure.database;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    private final String jdbcUrl;

    public DatabaseConnectionManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public Connection getConnection() throws SQLException {
        // Enforce foreign keys and explicit locking
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            // Use WAL mode for better concurrency in SQLite
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA busy_timeout = 5000;"); // 5 seconds timeout to avoid "database is locked" errors
        }
        return conn;
    }
    
    public void initializeSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             InputStream is = getClass().getResourceAsStream("/schema.sql")) {
            
            if (is == null) {
                logger.warn("schema.sql not found in resources, skipping schema initialization");
                return;
            }
            
            String schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            // Execute statements
            String[] sqlStatements = schema.split(";");
            for (String sql : sqlStatements) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
            logger.info("Database schema initialized successfully.");
            
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}
