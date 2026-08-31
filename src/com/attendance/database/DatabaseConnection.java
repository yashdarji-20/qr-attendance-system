package com.attendance.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simple JDBC connection pool for the Attendance System.
 * Thread-safe singleton that manages a fixed pool of MySQL connections.
 *
 * Usage:
 *   Connection conn = DatabaseConnection.getInstance().getConnection();
 *   // ... use conn ...
 *   DatabaseConnection.getInstance().releaseConnection(conn);
 */
public final class DatabaseConnection {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    private static volatile DatabaseConnection instance;
    private final BlockingQueue<Connection> pool;
    private final DatabaseConfig config;
    private final int poolSize;

    // ----------------------------------------------------------------
    // Singleton
    // ----------------------------------------------------------------

    private DatabaseConnection() {
        config   = DatabaseConfig.getInstance();
        poolSize = config.getMaxPoolSize();
        pool     = new ArrayBlockingQueue<>(poolSize);
        initPool();
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    // ----------------------------------------------------------------
    // Pool management
    // ----------------------------------------------------------------

    private void initPool() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found.", e);
        }

        int minIdle = config.getMinIdle();
        for (int i = 0; i < minIdle; i++) {
            try {
                pool.offer(createConnection());
                log.debug("Created initial connection #{}", i + 1);
            } catch (SQLException e) {
                log.error("Failed to create initial connection #{}", i + 1, e);
            }
        }
        log.info("Connection pool initialised with {} connections.", pool.size());
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(
                config.getUrl(),
                config.getUsername(),
                config.getPassword()
        );
    }

    /**
     * Borrow a connection from the pool (waits up to 5 s).
     */
    public Connection getConnection() throws SQLException {
        // Try to reuse a pooled connection
        Connection conn = pool.poll();

        if (conn != null) {
            try {
                if (!conn.isValid(2)) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                conn = null;
            }
        }

        // If no valid pooled connection, create a new one
        if (conn == null) {
            if (pool.size() < poolSize) {
                conn = createConnection();
                log.debug("Created new connection on demand.");
            } else {
                // Pool full — wait briefly
                try {
                    conn = pool.poll(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for a DB connection.");
                }
                if (conn == null) {
                    throw new SQLException("No database connections available (pool exhausted).");
                }
            }
        }
        return conn;
    }

    /**
     * Return a connection to the pool.
     * If the pool is full, the connection is closed instead.
     */
    public void releaseConnection(Connection conn) {
        if (conn == null) return;
        try {
            if (conn.isClosed()) return;
            conn.setAutoCommit(true); // reset state
            if (!pool.offer(conn)) {
                conn.close(); // pool full
            }
        } catch (SQLException e) {
            log.warn("Error returning connection to pool: {}", e.getMessage());
        }
    }

    /**
     * Test the connection to the database.
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            boolean ok = conn.isValid(3);
            releaseConnection(conn);
            return ok;
        } catch (SQLException e) {
            log.error("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Close all connections in the pool (call on app shutdown).
     */
    public void shutdown() {
        log.info("Shutting down connection pool...");
        Connection conn;
        while ((conn = pool.poll()) != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {}
        }
    }

    // ----------------------------------------------------------------
    // Convenience transaction helpers
    // ----------------------------------------------------------------

    /** Begin a transaction on the given connection. */
    public static void beginTransaction(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
    }

    /** Commit and restore auto-commit. */
    public static void commitTransaction(Connection conn) throws SQLException {
        conn.commit();
        conn.setAutoCommit(true);
    }

    /** Rollback and restore auto-commit. */
    public static void rollbackTransaction(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Rollback failed: {}", e.getMessage());
        }
    }
}
