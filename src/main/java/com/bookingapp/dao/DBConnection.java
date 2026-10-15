package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
 
/**
 * Supplies connections to the application's SQLite database.
 */
public class DBConnection {
 
    /**
     * Location of the SQLite database file, relative to the working directory.
     */
    private static final String URL = "jdbc:sqlite:booking_system.db";
 
    /**
     * Opens a new connection to the database.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
 