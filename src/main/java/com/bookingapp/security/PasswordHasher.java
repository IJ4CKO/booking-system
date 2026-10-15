package com.bookingapp.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Provides password hashing and password verification for the system.
 *
 * This class uses PBKDF2 with HMAC SHA-256 so that plain text passwords are
 * not stored in the database. A random salt is generated for each password,
 * and the stored value includes the algorithm name, iteration count, salt and
 * hashed password.
 */
public final class PasswordHasher {

    private static final String ALGORITHM =
            "PBKDF2WithHmacSHA256";

    private static final String HASH_PREFIX =
            "PBKDF2_SHA256";

    private static final int ITERATIONS = 210_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    /**
     * Prevents objects of this utility class from being created.
     *
     * All methods in this class are static, so the class does not need to be
     * instantiated.
     */
    private PasswordHasher() {
        /*
         * Utility class. Prevents objects being created.
         */
    }

    /**
     * Creates a secure hash for a password.
     *
     * The method creates a random salt, hashes the password using PBKDF2 and
     * returns a single stored string containing the algorithm, iteration count,
     * salt and password hash.
     *
     * @param password the plain text password to hash
     * @return the stored password hash value
     * @throws IllegalArgumentException if the password is empty or null
     */
    public static String hashPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty."
            );
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = createHash(
                password.toCharArray(),
                salt,
                ITERATIONS
        );

        return HASH_PREFIX
                + "$"
                + ITERATIONS
                + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Checks whether a supplied password matches a stored password hash.
     *
     * The stored hash is split into its stored parts, including the algorithm
     * prefix, iteration count, salt and expected hash. The supplied password is
     * then hashed using the same salt and iteration count before being compared
     * securely.
     *
     * @param password           the plain text password supplied by the user
     * @param storedPasswordHash the password hash stored in the database
     * @return true if the password matches the stored hash, otherwise false
     */
    public static boolean verifyPassword(
            String password,
            String storedPasswordHash
    ) {
        if (password == null
                || storedPasswordHash == null
                || storedPasswordHash.isBlank()) {

            return false;
        }

        try {
            String[] hashParts =
                    storedPasswordHash.split("\\$");

            if (hashParts.length != 4) {
                return false;
            }

            if (!HASH_PREFIX.equals(hashParts[0])) {
                return false;
            }

            int iterations = Integer.parseInt(hashParts[1]);

            byte[] salt = Base64.getDecoder()
                    .decode(hashParts[2]);

            byte[] expectedHash = Base64.getDecoder()
                    .decode(hashParts[3]);

            byte[] suppliedPasswordHash = createHash(
                    password.toCharArray(),
                    salt,
                    iterations
            );

            return MessageDigest.isEqual(
                    expectedHash,
                    suppliedPasswordHash
            );

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks whether a stored value appears to be a secure password hash.
     *
     * This is useful for identifying whether a password value has already been
     * hashed using the expected format.
     *
     * @param value the stored password value to check
     * @return true if the value starts with the expected hash prefix, otherwise false
     */
    public static boolean isSecureHash(String value) {
        return value != null
                && value.startsWith(HASH_PREFIX + "$");
    }

    /**
     * Creates the password hash using PBKDF2.
     *
     * The password, salt, iteration count and key length are passed into a
     * key specification. The password array is cleared afterwards to reduce
     * how long the password remains in memory.
     *
     * @param password   the password characters to hash
     * @param salt       the salt used to make the hash unique
     * @param iterations the number of hashing iterations to apply
     * @return the generated password hash as a byte array
     */
    private static byte[] createHash(
            char[] password,
            byte[] salt,
            int iterations
    ) {
        PBEKeySpec keySpecification = new PBEKeySpec(
                password,
                salt,
                iterations,
                KEY_LENGTH_BITS
        );

        try {
            SecretKeyFactory secretKeyFactory =
                    SecretKeyFactory.getInstance(ALGORITHM);

            return secretKeyFactory
                    .generateSecret(keySpecification)
                    .getEncoded();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to securely hash the password.",
                    e
            );

        } finally {
            keySpecification.clearPassword();

            for (int index = 0;
                    index < password.length;
                    index++) {

                password[index] = '\0';
            }
        }
    }
}