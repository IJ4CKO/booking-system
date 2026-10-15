package com.bookingapp.dao;
 
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
 
import com.bookingapp.security.PasswordHasher;
 
/**
 * Data access object for the {owner_user} table, which holds the staff
 * logins for the administration screens.
 */
public class UserDAO {
 
    /**
     * Authenticates an active owner account and returns its
     * database ID.
     */
    public Integer authenticateAndGetOwnerUserId(
            String username,
            String password
    ) throws Exception {
 
        if (username == null
                || username.isBlank()
                || password == null
                || password.isBlank()) {
 
            return null;
        }
 
        String sql = """
            SELECT
                owner_user_id,
                password_hash,
                active
            FROM owner_user
            WHERE LOWER(username) = LOWER(?)
            LIMIT 1
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    username.trim()
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (!results.next()) {
                    return null;
                }
 
                int ownerUserId =
                        results.getInt(
                                "owner_user_id"
                        );
 
                String storedPassword =
                        results.getString(
                                "password_hash"
                        );
 
                boolean active =
                        results.getInt(
                                "active"
                        ) == 1;
 
                if (!active
                        || storedPassword == null
                        || storedPassword.isBlank()) {
 
                    return null;
                }
 
                if (PasswordHasher.isSecureHash(
                        storedPassword
                )) {
                    boolean passwordMatches =
                            PasswordHasher.verifyPassword(
                                    password,
                                    storedPassword
                            );
 
                    return passwordMatches
                            ? ownerUserId
                            : null;
                }
 
                /*
                 * Temporary migration support for a legacy
                 * plain-text password.
                 *
                 * A matching legacy password is immediately
                 * replaced with a secure PBKDF2 hash.
                 */
                if (legacyPasswordMatches(
                        password,
                        storedPassword
                )) {
                    upgradeLegacyPassword(
                            conn,
                            ownerUserId,
                            password
                    );
 
                    return ownerUserId;
                }
 
                return null;
            }
        }
    }
 
    /**
     * Retained for existing code that only needs a true or false
     * authentication result.
     */
    public boolean authenticate(
            String username,
            String password
    ) throws Exception {
 
        return authenticateAndGetOwnerUserId(
                username,
                password
        ) != null;
    }
 
    /**
     * Replaces an account password with a newly generated
     * secure PBKDF2 hash.
     */
    public void updatePassword(
            int ownerUserId,
            String newPassword
    ) throws Exception {
 
        String passwordHash =
                PasswordHasher.hashPassword(
                        newPassword
                );
 
        String sql = """
            UPDATE owner_user
            SET password_hash = ?
            WHERE owner_user_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    passwordHash
            );
 
            statement.setInt(
                    2,
                    ownerUserId
            );
 
            int rowsUpdated =
                    statement.executeUpdate();
 
            if (rowsUpdated != 1) {
                throw new IllegalStateException(
                        "The user password could not be updated."
                );
            }
        }
    }
 
    /**
     * Changes the password for an active user after confirming
     * that the current password is correct.
     */
    public boolean changePassword(
            String username,
            String currentPassword,
            String newPassword
    ) throws Exception {
 
        if (username == null
                || username.isBlank()
                || currentPassword == null
                || currentPassword.isBlank()
                || newPassword == null
                || newPassword.isBlank()) {
 
            return false;
        }
 
        Integer ownerUserId =
                authenticateAndGetOwnerUserId(
                        username.trim(),
                        currentPassword
                );
 
        if (ownerUserId == null) {
            return false;
        }
 
        String newPasswordHash =
                PasswordHasher.hashPassword(
                        newPassword
                );
 
        String sql = """
            UPDATE owner_user
            SET password_hash = ?
            WHERE owner_user_id = ?
              AND active = 1
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    newPasswordHash
            );
 
            statement.setInt(
                    2,
                    ownerUserId
            );
 
            return statement.executeUpdate() == 1;
        }
    }
 
    /**
     * Replaces a plain-text password with a proper hash.
     */
    private void upgradeLegacyPassword(
            Connection conn,
            int ownerUserId,
            String password
    ) throws Exception {
 
        String secureHash =
                PasswordHasher.hashPassword(
                        password
                );
 
        String sql = """
            UPDATE owner_user
            SET password_hash = ?
            WHERE owner_user_id = ?
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    secureHash
            );
 
            statement.setInt(
                    2,
                    ownerUserId
            );
 
            int rowsUpdated =
                    statement.executeUpdate();
 
            if (rowsUpdated != 1) {
                throw new IllegalStateException(
                        "The legacy password could not be upgraded."
                );
            }
        }
    }
 
    /**
     * Compares a typed password against one still stored in plain text.
     */
    private boolean legacyPasswordMatches(
            String enteredPassword,
            String storedPassword
    ) {
        byte[] enteredBytes =
                enteredPassword.getBytes(
                        StandardCharsets.UTF_8
                );
 
        byte[] storedBytes =
                storedPassword.getBytes(
                        StandardCharsets.UTF_8
                );
 
        return MessageDigest.isEqual(
                enteredBytes,
                storedBytes
        );
    }
}