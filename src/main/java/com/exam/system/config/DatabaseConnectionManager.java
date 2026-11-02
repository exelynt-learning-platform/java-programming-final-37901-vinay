package com.exam.system.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * =============================================================================
 *             DESIGN PATTERN: SINGLETON (Database Connection Manager)
 * =============================================================================
 * Manages the single point of access to JDBC database connectivity and transactions.
 * Uses thread-safe double-checked locking with volatile keyword.
 */
public class DatabaseConnectionManager {

    private static final Logger logger = Logger.getLogger(DatabaseConnectionManager.class.getName());
    private static volatile DatabaseConnectionManager instance;
    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    private DatabaseConnectionManager() {
        String envUrl = System.getenv("DB_URL");
        this.jdbcUrl = (envUrl != null && !envUrl.trim().isEmpty())
                ? envUrl.trim()
                : "jdbc:sqlite:online_exam_system.db";
        this.dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER").trim() : "";
        this.dbPassword = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD").trim() : "";

        logger.info("DatabaseConnectionManager initialized with URL: " + jdbcUrl);
        initSchema();
    }

    public static DatabaseConnectionManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionManager.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionManager();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (dbUser.isEmpty() && dbPassword.isEmpty()) {
            return DriverManager.getConnection(jdbcUrl);
        }
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    /**
     * Initializes database tables if not already present.
     */
    private void initSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Read and execute schema.sql from disk or fallback
            String schemaSql;
            if (Files.exists(Paths.get("schema.sql"))) {
                schemaSql = Files.readString(Paths.get("schema.sql"));
            } else {
                schemaSql = getDefaultSchemaSql();
            }

            for (String statement : schemaSql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            logger.info("Database schema initialized successfully.");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    /**
     * Resets test tables for clean testing.
     */
    public void resetDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM exam_results");
            stmt.executeUpdate("DELETE FROM exam_questions");
            stmt.executeUpdate("DELETE FROM exams");
            stmt.executeUpdate("DELETE FROM questions");
            stmt.executeUpdate("DELETE FROM users");
            logger.info("Database tables reset successfully.");
        } catch (SQLException e) {
            logger.warning("Error resetting database tables: " + e.getMessage());
        }
    }

    private static String getDefaultSchemaSql() {
        return "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username VARCHAR(50) UNIQUE NOT NULL, password_hash VARCHAR(255) NOT NULL, full_name VARCHAR(100) NOT NULL, email VARCHAR(100) UNIQUE NOT NULL, role VARCHAR(20) NOT NULL);"
             + "CREATE TABLE IF NOT EXISTS questions (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, subject VARCHAR(50) NOT NULL, difficulty VARCHAR(20) NOT NULL, question_type VARCHAR(20) NOT NULL, options TEXT, correct_answer VARCHAR(255) NOT NULL, points REAL NOT NULL DEFAULT 1.0);"
             + "CREATE TABLE IF NOT EXISTS exams (id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR(100) NOT NULL, subject VARCHAR(50) NOT NULL, duration_minutes INTEGER NOT NULL, scheduled_time TEXT NOT NULL, total_marks REAL NOT NULL, passing_percentage REAL NOT NULL DEFAULT 40.0);"
             + "CREATE TABLE IF NOT EXISTS exam_questions (exam_id INTEGER NOT NULL, question_id INTEGER NOT NULL, PRIMARY KEY (exam_id, question_id));"
             + "CREATE TABLE IF NOT EXISTS exam_results (id INTEGER PRIMARY KEY AUTOINCREMENT, exam_id INTEGER NOT NULL, student_id INTEGER NOT NULL, score REAL NOT NULL, max_score REAL NOT NULL, percentage REAL NOT NULL, grade VARCHAR(5) NOT NULL, status VARCHAR(20) NOT NULL, evaluated_at TEXT NOT NULL);";
    }
}
