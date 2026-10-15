package com.bookingapp.ui;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.bookingapp.dao.DataRetentionDAO;
import com.bookingapp.model.DataRetentionCustomer;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controls the Data Retention screen.
 *
 * This screen helps the business owner review customers whose latest confirmed
 * or completed order was more than seven years ago. The owner can then choose
 * whether to anonymise the selected customer's personal data.
 */
public class DataRetentionController {

    @FXML
    private TableView<DataRetentionCustomer> dataRetentionTable;

    @FXML
    private TableColumn<DataRetentionCustomer, Integer> customerIdColumn;

    @FXML
    private TableColumn<DataRetentionCustomer, String> fullNameColumn;

    @FXML
    private TableColumn<DataRetentionCustomer, String> emailColumn;

    @FXML
    private TableColumn<DataRetentionCustomer, String> phoneColumn;

    @FXML
    private TableColumn<DataRetentionCustomer, String> lastOrderDateColumn;

    @FXML
    private TableColumn<DataRetentionCustomer, Integer>
            yearsSinceLastOrderColumn;

    @FXML
    private TableColumn<DataRetentionCustomer, String>
            recommendedActionColumn;

    private final DataRetentionDAO dataRetentionDAO =
            new DataRetentionDAO();

    /**
     * Sets up the table columns and loads customers who are due for data
     * retention review.
     */
    @FXML
    private void initialize() {
        customerIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("customerId")
        );

        fullNameColumn.setCellValueFactory(
                new PropertyValueFactory<>("fullName")
        );

        emailColumn.setCellValueFactory(
                new PropertyValueFactory<>("email")
        );

        phoneColumn.setCellValueFactory(
                new PropertyValueFactory<>("phone")
        );

        lastOrderDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("formattedLastOrderDate")
        );

        yearsSinceLastOrderColumn.setCellValueFactory(
                new PropertyValueFactory<>("yearsSinceLastOrder")
        );

        recommendedActionColumn.setCellValueFactory(
                new PropertyValueFactory<>("recommendedAction")
        );

        loadCustomersDueForReview();
    }

    /**
     * Loads customers whose latest confirmed or completed order is outside the
     * seven-year retention period.
     */
    @FXML
    private void loadCustomersDueForReview() {
        try {
            List<DataRetentionCustomer> customersDueForReview =
                    dataRetentionDAO.getCustomersDueForRetentionReview();

            dataRetentionTable.setItems(
                    FXCollections.observableArrayList(customersDueForReview)
            );

        } catch (RuntimeException exception) {
            showError(
                    "Unable to Load Data Retention Records",
                    exception.getMessage()
            );
        }
    }

    /**
     * Anonymises the selected customer after confirmation from the business
     * owner.
     *
     * The customer is not deleted. Their personal details are removed from the
     * database while the customer ID and booking history remain available for
     * non-identifying business records.
     */
    @FXML
    private void anonymiseSelectedCustomer() {
        DataRetentionCustomer selectedCustomer =
                dataRetentionTable.getSelectionModel().getSelectedItem();

        if (selectedCustomer == null) {
            showInformation(
                    "No Customer Selected",
                    "Please select a customer to anonymise."
            );
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Customer Anonymisation");
        confirmationAlert.setHeaderText("Anonymise selected customer?");
        confirmationAlert.setContentText(
                "This will remove the selected customer's personal details "
                        + "because their latest confirmed or completed order "
                        + "is outside the seven-year retention period.\n\n"
                        + "The customer row will not be deleted. Booking "
                        + "history will remain for non-identifying business "
                        + "records.\n\n"
                        + "Customer ID: "
                        + selectedCustomer.getCustomerId()
                        + "\nCustomer: "
                        + selectedCustomer.getFullName()
                        + "\nLast order date: "
                        + selectedCustomer.getFormattedLastOrderDate()
                        + "\nYears since last order: "
                        + selectedCustomer.getYearsSinceLastOrder()
        );

        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            dataRetentionDAO.anonymiseCustomer(
                    selectedCustomer.getCustomerId()
            );

            showInformation(
                    "Customer Anonymised",
                    "The selected customer's personal data has been "
                            + "anonymised and the action has been logged."
            );

            loadCustomersDueForReview();

        } catch (RuntimeException exception) {
            showError(
                    "Unable to Anonymise Customer",
                    exception.getMessage()
            );
        }
    }

    /**
     * Shows keyboard shortcut guidance for this screen.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Data Retention Keyboard Shortcuts");
        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                        + "Enter or Space: Select the focused button\n\n"
                        + "Alt + K: Show keyboard shortcuts\n"
                        + "Alt + R: Refresh retention list\n"
                        + "Alt + A: Anonymise selected customer\n"
                        + "Alt + B: Back to Dashboard"
        );

        alert.showAndWait();
    }

    /**
     * Returns to the dashboard screen.
     */
    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/com/bookingapp/ui/dashboard.fxml"
                    )
            );

            Stage stage = (Stage) dataRetentionTable
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.setMaximized(true);

        } catch (IOException exception) {
            showError(
                    "Navigation Error",
                    "Unable to return to the dashboard."
            );
        }
    }

    /**
     * Shows an information message to the user.
     *
     * @param title   the alert title and heading
     * @param message the message to display
     */
    private void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error message to the user.
     *
     * @param title   the alert title and heading
     * @param message the error message to display
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}