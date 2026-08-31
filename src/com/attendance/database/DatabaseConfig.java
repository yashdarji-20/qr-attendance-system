package com.attendance.database;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads database configuration from db.properties file.
 * Centralises all DB settings in one place.
 */
public final class DatabaseConfig {

    private static final Properties props = new Properties();
    private static DatabaseConfig instance;

    // Defaults (overridden by db.properties if present)
    private static final String DEFAULT_HOST     = "localhost";
    private static final String DEFAULT_PORT     = "3306";
    private static final String DEFAULT_DB       = "attendance_system";
    private static final String DEFAULT_USER     = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_TIMEZONE = "Asia/Kolkata";

    private DatabaseConfig() {
        loadProperties();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println("[DatabaseConfig] Could not load db.properties, using defaults.");
        }
    }

    public String getUrl() {
        String host     = props.getProperty("db.host",     DEFAULT_HOST);
        String port     = props.getProperty("db.port",     DEFAULT_PORT);
        String db       = props.getProperty("db.name",     DEFAULT_DB);
        String timezone = props.getProperty("db.timezone", DEFAULT_TIMEZONE);
        return String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=%s" +
            "&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8",
            host, port, db, timezone
        );
    }

    public String getUsername() {
        return props.getProperty("db.username", DEFAULT_USER);
    }

    public String getPassword() {
        return props.getProperty("db.password", DEFAULT_PASSWORD);
    }

    public int getMaxPoolSize() {
        return Integer.parseInt(props.getProperty("db.pool.maxSize", "10"));
    }

    public int getMinIdle() {
        return Integer.parseInt(props.getProperty("db.pool.minIdle", "2"));
    }

    public int getConnectionTimeout() {
        return Integer.parseInt(props.getProperty("db.pool.connectionTimeout", "30000"));
    }
}
