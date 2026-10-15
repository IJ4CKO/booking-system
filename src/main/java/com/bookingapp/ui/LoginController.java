package com.bookingapp.ui;

import com.bookingapp.dao.UserDAO;
import com.bookingapp.security.UserSession;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * Controller for the login screen.
 *
 * This class manages the user login process for the staff-facing desktop
 * application. It checks the entered username and password, starts a user
 * session after a successful login and opens the dashboard screen.
 */
public class LoginController {

    /**
     * Data access object used for user authentication database operations.
     */
    private final UserDAO userDAO
            = new UserDAO();

    /**
     * Text field used to enter the username.
     */
    @FXML
    private TextField usernameField;

    /**
     * Password field used to enter the password.
     */
    @FXML
    private PasswordField passwordField;

    /**
     * Initialises the login screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It allows the user to press Enter from either the username field
     * or password field to submit the login form.
     */
    @FXML
    private void initialize() {
        /*
         * Allows the user to press Enter from either field to submit the login
         * form.
         */
        usernameField.setOnAction(
                event -> handleLogin()
        );

        passwordField.setOnAction(
                event -> handleLogin()
        );
    }

    /**
     * Handles keyboard shortcuts on the login screen.
     *
     * Alt + S signs the user in. This is added as a direct key handler because
     */
    @FXML
    private void handleKeyboardShortcuts(KeyEvent event) {
        if (event.isAltDown() && event.getCode() == KeyCode.S) {
            handleLogin();
            event.consume();
        }
    }

    /**
     * Shows the keyboard shortcuts available on the login screen.
     *
     * This keeps the login screen less cluttered while still making keyboard
     * support available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Login Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + S: Sign in\n"
                + "Enter in username or password field: Sign in"
        );

        alert.showAndWait();
    }

    /**
     * Handles the login process.
     *
     * The method validates that both fields have been completed, clears any
     * previous session, checks the entered login details and starts a new user
     * session if the login is successful. If the login fails, a clear message
     * is shown to the user.
     */
    @FXML
    private void handleLogin() {
        String username
                = usernameField.getText();

        String password
                = passwordField.getText();

        if (username == null
                || username.isBlank()
                || password == null
                || password.isBlank()) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "Enter both your username and password."
            );

            return;
        }

        String normalisedUsername
                = username.trim();

        try {
            /*
             * Remove any previous session before attempting a new login.
             */
            UserSession.clear();

            Integer ownerUserId
                    = userDAO
                            .authenticateAndGetOwnerUserId(
                                    normalisedUsername,
                                    password
                            );

            if (ownerUserId != null) {
                /*
                 * Store only the authenticated owner's ID and username. The
                 * password is never stored in the user session.
                 */
                UserSession.startSession(
                        ownerUserId,
                        normalisedUsername
                );

                passwordField.clear();

                openDashboard();
                return;
            }

            passwordField.clear();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Invalid username or password."
            );

            passwordField.requestFocus();

        } catch (Exception e) {
            e.printStackTrace();

            UserSession.clear();
            passwordField.clear();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to check the login details.\n\n"
                    + "Please check the database connection."
            );
        }
    }

    /**
     * Opens the dashboard screen after a successful login.
     *
     * The existing scene is reused so that the current window size, position
     * and maximised state are preserved.
     */
    private void openDashboard() {
        try {
            FXMLLoader loader
                    = new FXMLLoader(
                            getClass().getResource(
                                    "/com/bookingapp/ui/"
                                    + "dashboard.fxml"
                            )
                    );

            Parent root
                    = loader.load();

            Stage stage
                    = (Stage) usernameField
                            .getScene()
                            .getWindow();

            /*
             * Reuse the existing Scene so the current window size, position and
             * maximised state are preserved.
             */
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();

            UserSession.clear();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to open the dashboard."
            );
        }
    }

    /**
     * Shows a message dialog to the user.
     *
     * This is used for validation warnings, failed login attempts and
     * navigation errors on the login screen.
     *
     * @param type the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert
                = new Alert(type);

        alert.setTitle("Login");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
