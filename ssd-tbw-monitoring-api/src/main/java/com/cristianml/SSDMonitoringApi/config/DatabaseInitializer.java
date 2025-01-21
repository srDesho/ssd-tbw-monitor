package com.cristianml.SSDMonitoringApi.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * This class is responsible for initializing the PostgreSQL database.
 * It checks if the specified database exists and creates it if it doesn't.
 */
public class DatabaseInitializer {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/"; // URL without database name
    private static final String DB_NAME = "tbw_monitor";
    private static final String USER = "postgres"; // PostgreSQL user
    private static final String PASSWORD = "myPass"; // PostgreSQL password

    /**
     * Initializes the PostgreSQL database.
     * Checks if the database exists and creates it if it doesn't.
     */
    public static void initializeDatabase() {
        Connection conn = null;
        Statement stmt = null;

        try {
            // Connect to PostgreSQL (without specifying the database initially)
            conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            stmt = conn.createStatement();

            // Check if the database exists
            boolean dbExists = checkIfDatabaseExists(conn, DB_NAME);

            if (!dbExists) {
                // Create the database if it doesn't exist
                stmt.executeUpdate("CREATE DATABASE " + DB_NAME);
                System.out.println("Database '" + DB_NAME + "' created successfully.");
            } else {
                System.out.println("Database '" + DB_NAME + "' already exists.");
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Handle exceptions (log, throw custom exceptions, etc.)
        } finally {
            // Close resources (statement and connection) in a finally block to ensure they are always closed
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace(); // Handle exceptions during resource closing
            }
        }
    }

    /**
     * Checks if a database with the given name exists.
     *
     * @param conn   The database connection.
     * @param dbName The name of the database to check.
     * @return True if the database exists, false otherwise.
     * @throws SQLException If a database error occurs.
     */
    private static boolean checkIfDatabaseExists(Connection conn, String dbName) throws SQLException {
        // Query existing databases using getMetaData().getCatalogs()
        try (ResultSet rs = conn.getMetaData().getCatalogs()) { // Try-with-resources for ResultSet
            while (rs.next()) {
                String existingDbName = rs.getString("TABLE_CAT"); // Get the database name
                if (existingDbName != null && existingDbName.equals(dbName)) { // Check if the name matches
                    return true; // Database exists
                }
            }
        }
        return false; // Database doesn't exist
    }
}