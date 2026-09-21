package com.abhishek.banking.infrastructure.database;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages JDBC connections to a SQLite database.
 *
 * <h3>In-Memory vs File Mode</h3>
 * <p>When the JDBC URL is {@code jdbc:sqlite::memory:}, SQLite creates a fresh private
 * database per connection. To share one in-memory database across all repositories this
 * manager holds a single persistent connection and returns a <em>non-closeable wrapper</em>
 * so that {@code try-with-resources} blocks in repository code do not accidentally close
 * the shared connection.</p>
 *
 * <p>For file-based databases each call returns a real, independently closeable
 * connection (SQLite WAL mode supports multiple concurrent readers/one writer).</p>
 */
public class DatabaseConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);

    private final String jdbcUrl;
    private final boolean inMemory;

    /** Kept open for the JVM lifetime in in-memory mode. */
    private Connection sharedConnection;

    public DatabaseConnectionManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        this.inMemory = jdbcUrl.contains(":memory:");
    }

    /**
     * Returns a JDBC connection ready for use.
     *
     * <ul>
     *   <li><b>In-memory mode:</b> returns a {@link NonCloseableConnection} wrapping the
     *       single shared connection. Calling {@code close()} on the returned object is a
     *       no-op so {@code try-with-resources} is safe to use in repositories.</li>
     *   <li><b>File mode:</b> opens and returns a new connection each call. The caller is
     *       responsible for closing it (a {@code try-with-resources} does this correctly).</li>
     * </ul>
     */
    public synchronized Connection getConnection() throws SQLException {
        if (inMemory) {
            if (sharedConnection == null || sharedConnection.isClosed()) {
                sharedConnection = openNewConnection();
                logger.debug("Opened shared in-memory SQLite connection");
            }
            return new NonCloseableConnection(sharedConnection);
        }
        return openNewConnection();
    }

    private Connection openNewConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }
        return conn;
    }

    /**
     * Initializes the schema from {@code /schema.sql} on the classpath.
     * Uses {@code CREATE TABLE IF NOT EXISTS} so it is safe to call multiple times.
     */
    public void initializeSchema() {
        try (InputStream is = getClass().getResourceAsStream("/schema.sql")) {
            if (is == null) {
                logger.warn("schema.sql not found in resources – skipping schema initialisation");
                return;
            }
            String schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Connection conn = getConnection();          // shared connection in in-memory mode
            try (Statement stmt = conn.createStatement()) {
                for (String sql : schema.split(";")) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            logger.info("Database schema initialised successfully");
        } catch (Exception e) {
            logger.error("Failed to initialise database schema", e);
            throw new RuntimeException("Database initialisation failed", e);
        }
    }

    /** Closes the underlying shared connection. Call at application shutdown. */
    public synchronized void close() {
        if (inMemory && sharedConnection != null) {
            try {
                sharedConnection.close();
                sharedConnection = null;
            } catch (SQLException e) {
                logger.warn("Error closing shared in-memory connection", e);
            }
        }
    }
}
