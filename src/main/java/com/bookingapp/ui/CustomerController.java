package com.bookingapp.ui;

import com.bookingapp.dao.CustomerDAO;
import com.bookingapp.model.Customer;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the customers screen.
 *
 * This class manages the customer user interface. It allows the user to add
 * customers, search existing customers, view customer records in a table,
 * delete selected customers and return to the dashboard.
 */
public class CustomerController {

    /**
     * Text field used to enter the customer's full name.
     */
    @FXML
    private TextField fullNameField;

    /**
     * Text field used to enter the customer's telephone number.
     */
    @FXML
    private TextField phoneField;

    /**
     * Text field used to enter the customer's email address.
     */
    @FXML
    private TextField emailField;

    /**
     * Text field used to search the customer table.
     */
    @FXML
    private TextField searchField;

    /**
     * Table used to display customer records.
     */
    @FXML
    private TableView<Customer> customerTable;

    /**
     * Table column used to display the customer ID.
     */
    @FXML
    private TableColumn<Customer, Number> idColumn;

    /**
     * Table column used to display the customer's full name.
     */
    @FXML
    private TableColumn<Customer, String> nameColumn;

    /**
     * Table column used to display the customer's telephone number.
     */
    @FXML
    private TableColumn<Customer, String> phoneColumn;

    /**
     * Table column used to display the customer's email address.
     */
    @FXML
    private TableColumn<Customer, String> emailColumn;

    /**
     * Master list of customers used by the table, search and sort functions.
     */
    private final ObservableList<Customer> masterCustomers
            = FXCollections.observableArrayList();

    /**
     * Data access object used for customer database operations.
     */
    private final CustomerDAO customerDAO = new CustomerDAO();

    /**
     * Initialises the customers screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the table columns, configures searching and loads the
     * customer records from the database.
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        configureSearch();
        loadCustomers();
    }

    /**
     * Shows the keyboard shortcuts available on the customers screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Customers Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + A: Add customer\n"
                + "Alt + D: Delete selected customer\n"
                + "Alt + R: Refresh customers\n"
                + "Alt + B: Back to Dashboard"
        );

        alert.showAndWait();
    }

    /**
     * Configures the customer table columns.
     *
     * Each table column is linked to the correct customer value so that the
     * customer ID, full name, telephone number and email address are displayed
     * in the table.
     */
    private void configureTableColumns() {
        idColumn.setCellValueFactory(data
                -> new SimpleIntegerProperty(
                        data.getValue().getCustomerId()
                )
        );

        nameColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getFullName())
                )
        );

        phoneColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getPhone())
                )
        );

        emailColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getEmail())
                )
        );
    }

    /**
     * Configures the search box for the customer table.
     *
     * The search box filters the master customer list and allows the user to
     * search by customer ID, full name, telephone number or email address.
     */
    private void configureSearch() {
        FilteredList<Customer> filteredCustomers
                = new FilteredList<>(masterCustomers, customer -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue)
                -> filteredCustomers.setPredicate(customer
                        -> matchesSearch(customer, newValue)
                )
        );

        SortedList<Customer> sortedCustomers
                = new SortedList<>(filteredCustomers);

        sortedCustomers.comparatorProperty()
                .bind(customerTable.comparatorProperty());

        customerTable.setItems(sortedCustomers);
    }

    /**
     * Checks whether a customer matches the search text.
     *
     * @param customer   the customer to check
     * @param searchText the text entered by the user
     * @return true if the customer matches the search text, otherwise false
     */
    private boolean matchesSearch(
            Customer customer,
            String searchText
    ) {
        if (customer == null) {
            return false;
        }

        String searchValue = safeText(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(customer.getCustomerId()),
                searchValue
        )
                || containsSearchValue(
                        customer.getFullName(),
                        searchValue
                )
                || containsSearchValue(
                        customer.getPhone(),
                        searchValue
                )
                || containsSearchValue(
                        customer.getEmail(),
                        searchValue
                );
    }

    /**
     * Checks whether a text value contains the search value.
     *
     * Null values are converted into an empty string before checking.
     *
     * @param value       the text value to check
     * @param searchValue the search value to find
     * @return true if the value contains the search value, otherwise false
     */
    private boolean containsSearchValue(
            String value,
            String searchValue
    ) {
        return safeText(value)
                .toLowerCase()
                .contains(searchValue);
    }

    /**
     * Adds a new customer using the values entered in the form.
     *
     * The customer's full name is required. If validation passes, the customer
     * is saved to the database and the table is refreshed.
     */
    @FXML
    private void addCustomer() {
        String fullName = fullNameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();

        if (fullName == null || fullName.isBlank()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Full name is required"
            );

            return;
        }

        try {
            Customer customer
                    = new Customer(
                            0,
                            fullName,
                            phone,
                            email
                    );

            customerDAO.addCustomer(customer);

            fullNameField.clear();
            phoneField.clear();
            emailField.clear();

            loadCustomers();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Customer added successfully"
            );

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    e.getMessage()
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error adding customer"
            );
        }
    }

    /**
     * Loads all customers from the database into the table.
     *
     * The master customer list is refreshed and the current table selection is
     * cleared. If loading fails, an error message is shown to the user.
     */
    @FXML
    private void loadCustomers() {
        try {
            masterCustomers.setAll(
                    customerDAO.getAllCustomers()
            );

            customerTable.getSelectionModel().clearSelection();

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading customers"
            );
        }
    }

    /**
     * Deletes the selected customer.
     *
     * A customer must be selected before this action can be completed. If a
     * customer is selected, the customer is deleted from the database and the
     * table is refreshed.
     */
    @FXML
    private void deleteSelectedCustomer() {
        Customer selectedCustomer
                = customerTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedCustomer == null) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Please select a customer to delete"
            );

            return;
        }

        try {
            customerDAO.deleteCustomer(
                    selectedCustomer.getCustomerId()
            );

            loadCustomers();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Customer deleted successfully"
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error deleting customer"
            );
        }
    }

    /**
     * Returns the user to the dashboard screen.
     *
     * @param event the JavaFX action event from the Back button
     */
    @FXML
    private void goBack(ActionEvent event) {
        try {
            FXMLLoader loader
                    = new FXMLLoader(
                            getClass().getResource(
                                    "/com/bookingapp/ui/dashboard.fxml"
                            )
                    );

            Parent root = loader.load();

            Stage stage
                    = (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Unable to return to the dashboard.\n\n"
                    + e.getMessage()
            );
            alert.showAndWait();
        }
    }

    /**
     * Safely trims text.
     *
     * Null values are converted into an empty string so that search and display
     * logic can handle text safely.
     *
     * @param value the text value to clean
     * @return the trimmed text, or an empty string if the value is null
     */
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Shows a message dialog to the user.
     *
     * This is used for validation errors, loading errors, deletion results and
     * general information messages on the customers screen.
     *
     * @param type    the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle("Customers");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}