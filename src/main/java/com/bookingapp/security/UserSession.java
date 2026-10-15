package com.bookingapp.security;

/**
 * Stores details about the currently signed-in owner user.
 *
 * This class is used to keep track of the authenticated user during the
 * current application session. It stores the owner user ID and username, but
 * deliberately does not store the user's password.
 */
public final class UserSession {

    private static Integer ownerUserId;
    private static String username;

    /**
     * Prevents objects of this utility class from being created.
     *
     * All session details are stored using static fields and accessed through
     * static methods, so this class does not need to be instantiated.
     */
    private UserSession() {
        // Utility class.
    }

    /**
     * Starts a session for a successfully authenticated owner.
     *
     * The password is deliberately not stored. Only the owner user ID and
     * username are kept so the system can identify the current user.
     *
     * @param signedInOwnerUserId the database identifier for the signed-in owner
     * @param signedInUsername    the username of the signed-in owner
     * @throws IllegalArgumentException if the owner user ID or username is invalid
     */
    public static void startSession(
            int signedInOwnerUserId,
            String signedInUsername
    ) {
        if (signedInOwnerUserId <= 0) {
            throw new IllegalArgumentException(
                    "A valid owner user ID is required."
            );
        }

        if (signedInUsername == null
                || signedInUsername.isBlank()) {

            throw new IllegalArgumentException(
                    "A valid username is required."
            );
        }

        ownerUserId = signedInOwnerUserId;
        username = signedInUsername.trim();
    }

    /**
     * Gets the owner user ID for the current session.
     *
     * If no user is currently logged in, an exception is thrown because there
     * is no valid owner user ID to return.
     *
     * @return the signed-in owner user ID
     * @throws IllegalStateException if there is no authenticated owner session
     */
    public static int getOwnerUserId() {
        if (!isLoggedIn()) {
            throw new IllegalStateException(
                    "There is no authenticated owner session."
            );
        }

        return ownerUserId;
    }

    /**
     * Gets the username for the current session.
     *
     * @return the signed-in username, or null if no user is logged in
     */
    public static String getUsername() {
        return username;
    }

    /**
     * Checks whether an owner user is currently logged in.
     *
     * A user is treated as logged in when a valid owner user ID and username
     * have both been stored.
     *
     * @return true if an owner user is logged in, otherwise false
     */
    public static boolean isLoggedIn() {
        return ownerUserId != null
                && ownerUserId > 0
                && username != null
                && !username.isBlank();
    }

    /**
     * Clears the current user session.
     *
     * This is used when the user logs out or when the session needs to be reset.
     * It removes the stored owner user ID and username.
     */
    public static void clear() {
        ownerUserId = null;
        username = null;
    }
}