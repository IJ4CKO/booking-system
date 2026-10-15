package com.bookingapp.ui;

import com.bookingapp.dao.UserDAO;
import com.bookingapp.security.UserSession;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

/**
 * Controller for the change password screen.
 *
 * This class allows a signed-in owner user to change their password. It checks
 * the current session, validates the new password and uses the user data access
 * object to update the stored password securely.
 */
public class ChangePasswordController {

    /**
     * Minimum number of characters required for a new password.
     */
    private static final int MINIMUM_PASSWORD_LENGTH = 8;

    /**
     * Data access object used for user account database operations.
     */
    private final UserDAO userDAO = new UserDAO();

    /**
     * Label used to display the signed-in username.
     */
    @FXML
    private Label usernameLabel;

    /**
     * Password field used to enter the current password.
     */
    @FXML
    private PasswordField currentPasswordField;

    /**
     * Password field used to enter the new password.
     */
    @FXML
    private PasswordField newPasswordField;

    /**
     * Password field used to confirm the new password.
     */
    @FXML
    private PasswordField confirmPasswordField;

    /**
     * Button used to submit the password change.
     */
    @FXML
    private Button changePasswordButton;

    /**
     * Initialises the change password screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It checks whether a user is signed in and displays the username.
     * If no signed-in user is found, the password change button is disabled.
     */
    @FXML
    private void initialize() {
        String username = UserSession.getUsername();

        if (username == null || username.isBlank()) {
            usernameLabel.setText("No signed-in user");
            changePasswordButton.setDisable(true);

            showMessage(
                    Alert.AlertType.ERROR,
                    "No signed-in user could be found."
            );

            return;
        }

        usernameLabel.setText(username);
    }

    /**
     * Shows the keyboard shortcuts available on the change password screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Change Password Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + S: Save new password\n"
                + "Alt + C: Clear password fields\n"
                + "Alt + B: Back to Dashboard"
        );

        alert.showAndWait();
    }

    /**
     * Changes the signed-in user's password.
     *
     * The method validates that all fields have been completed, checks the new
     * password strength rules, checks that both new password fields match and
     * prevents the user from reusing their current password. If validation
     * passes, the password is changed through the user DAO.
     */
    @FXML
    private void changePassword() {
        String username = UserSession.getUsername();
        String currentPassword =
                currentPasswordField.getText();
        String newPassword =
                newPasswordField.getText();
        String confirmedPassword =
                confirmPasswordField.getText();

        if (username == null || username.isBlank()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "No signed-in user could be found."
            );

            return;
        }

        if (currentPassword == null
                || currentPassword.isBlank()
                || newPassword == null
                || newPassword.isBlank()
                || confirmedPassword == null
                || confirmedPassword.isBlank()) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "Complete all password fields."
            );

            return;
        }

        if (newPassword.length()
                < MINIMUM_PASSWORD_LENGTH) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "The new password must contain at least "
                            + MINIMUM_PASSWORD_LENGTH
                            + " characters."
            );

            newPasswordField.requestFocus();
            return;
        }

        if (!containsUppercaseLetter(newPassword)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "The new password must contain at least "
                            + "one uppercase letter."
            );

            newPasswordField.requestFocus();
            return;
        }

        if (!containsLowercaseLetter(newPassword)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "The new password must contain at least "
                            + "one lowercase letter."
            );

            newPasswordField.requestFocus();
            return;
        }

        if (!containsNumber(newPassword)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "The new password must contain at least "
                            + "one number."
            );

            newPasswordField.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmedPassword)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "The new passwords do not match."
            );

            confirmPasswordField.clear();
            confirmPasswordField.requestFocus();
            return;
        }

        if (newPassword.equals(currentPassword)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "The new password must be different from "
                            + "the current password."
            );

            newPasswordField.clear();
            confirmPasswordField.clear();
            newPasswordField.requestFocus();
            return;
        }

        try {
            boolean changed = userDAO.changePassword(
                    username,
                    currentPassword,
                    newPassword
            );

            if (!changed) {
                currentPasswordField.clear();

                showMessage(
                        Alert.AlertType.ERROR,
                        "The current password is incorrect."
                );

                currentPasswordField.requestFocus();
                return;
            }

            clearFields();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Your password has been changed successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to change the password.\n\n"
                            + "No changes have been saved."
            );
        }
    }

    /**
     * Clears all password fields.
     *
     * This resets the current password, new password and confirm password
     * fields, then moves the cursor back to the current password field.
     */
    @FXML
    private void clearFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        currentPasswordField.requestFocus();
    }

    /**
     * Returns the user to the dashboard screen.
     *
     * The existing scene is reused so that the current window size and position
     * are preserved.
     */
    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/bookingapp/ui/dashboard.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = (Stage) currentPasswordField
                    .getScene()
                    .getWindow();

            /*
             * Keep the existing Scene so the current window size and position
             * are preserved.
             */
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to return to the dashboard."
            );
        }
    }

    /**
     * Checks whether a password contains an uppercase letter.
     *
     * @param password the password to check
     * @return true if the password contains an uppercase letter, otherwise false
     */
    private boolean containsUppercaseLetter(
            String password
    ) {
        return password.chars()
                .anyMatch(Character::isUpperCase);
    }

    /**
     * Checks whether a password contains a lowercase letter.
     *
     * @param password the password to check
     * @return true if the password contains a lowercase letter, otherwise false
     */
    private boolean containsLowercaseLetter(
            String password
    ) {
        return password.chars()
                .anyMatch(Character::isLowerCase);
    }

    /**
     * Checks whether a password contains a number.
     *
     * @param password the password to check
     * @return true if the password contains a number, otherwise false
     */
    private boolean containsNumber(String password) {
        return password.chars()
                .anyMatch(Character::isDigit);
    }

    /**
     * Shows a message dialog to the user.
     *
     * This is used for validation errors, password change results and navigation
     * errors on the change password screen.
     *
     * @param type    the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert = new Alert(type);

        alert.setTitle("Change Password");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}