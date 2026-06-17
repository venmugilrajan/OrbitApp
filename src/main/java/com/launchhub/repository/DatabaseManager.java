package com.launchhub.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".launchhub";
    private static final String DB_FILE = DB_DIR + File.separator + "launchhub.db";
    private static final String CONNECTION_URL = "jdbc:sqlite:" + DB_FILE;

    static {
        // Initialize DB directory and connection
        try {
            File dir = new File(DB_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            initializeSchema();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECTION_URL);
    }

    private static void initializeSchema() {
        String createAppsTable = """
            CREATE TABLE IF NOT EXISTS applications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                publisher TEXT,
                version TEXT,
                install_location TEXT,
                install_date TEXT,
                size_bytes INTEGER,
                executable_path TEXT UNIQUE,
                icon_path TEXT,
                category TEXT,
                is_favorite INTEGER DEFAULT 0,
                launch_count INTEGER DEFAULT 0,
                last_used TEXT
            );
        """;

        String createUsageHistoryTable = """
            CREATE TABLE IF NOT EXISTS usage_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                app_id INTEGER,
                launched_at TEXT NOT NULL,
                FOREIGN KEY (app_id) REFERENCES applications (id) ON DELETE CASCADE
            );
        """;

        String createSettingsTable = """
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );
        """;

        String createDisabledStartupsTable = """
            CREATE TABLE IF NOT EXISTS disabled_startups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                command TEXT NOT NULL,
                type TEXT NOT NULL,
                registry_key_name TEXT,
                original_path TEXT NOT NULL
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createAppsTable);
            stmt.execute(createUsageHistoryTable);
            stmt.execute(createSettingsTable);
            stmt.execute(createDisabledStartupsTable);
            
            // Insert default settings if not exists
            stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('theme', 'dark');");
            stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('auto_scan', 'true');");
            stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('grid_size', 'medium');");
        } catch (SQLException e) {
            System.err.println("Error initializing SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getDbDirectory() {
        return DB_DIR;
    }
}
