package com.bookingapp.ui;

import com.bookingapp.dao.SupplierDAO;
import com.bookingapp.model.Supplier;

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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the suppliers screen.
 *
 * This class manages the supplier user interface. It allows the user to add
 * suppliers, search existing suppliers, view supplier records in a table,
 * delete selected suppliers and return to the dashboard.
 */
public class SupplierController {

    /**
     * Text field used to enter the supplier name.
     */
    @FXML
    private TextField supplierNameField;

    /**
     * Text field used to enter the supplier contact name.
     */
    @FXML
    private TextField contactNameField;

    /**
     * Text field used to enter the supplier telephone number.
     */
    @FXML
    private TextField phoneField;

    /**
     * Text field used to enter the supplier email address.
     */
    @FXML
    private TextField emailField;

    /**
     * Text area used to enter optional supplier notes.
     */
    @FXML
    private TextArea notesField;

    /**
     * Text field used to search the supplier table.
     */
    @FXML
    private TextField searchField;

    /**
     * Table used to display supplier records.
     */
    @FXML
    private TableView<Supplier> supplierTable;

    /**
     * Table column used to display the supplier ID.
     */
    @FXML
    private TableColumn<Supplier, Number> supplierIdColumn;

    /**
     * Table column used to display the supplier name.
     */
    @FXML
    private TableColumn<Supplier, String> supplierNameColumn;

    /**
     * Table column used to display the supplier contact name.
     */
    @FXML
    private TableColumn<Supplier, String> contactNameColumn;

    /**
     * Table column used to display the supplier telephone number.
     */
    @FXML
    private TableColumn<Supplier, String> phoneColumn;

    /**
     * Table column used to display the supplier email address.
     */
    @FXML
    private TableColumn<Supplier, String> emailColumn;

    /**
     * Table column used to display any supplier notes.
     */
    @FXML
    private TableColumn<Supplier, String> notesColumn;

    /**
     * Master list of suppliers used by the table, search and sort functions.
     */
    private final ObservableList<Supplier> masterSuppliers
            = FXCollections.observableArrayList();

    /**
     * Data access object used for supplier database operations.
     */
    private final SupplierDAO supplierDAO = new SupplierDAO();

    /**
     * Initialises the suppliers screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the table columns, configures searching and loads the
     * supplier records from the database.
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        configureSearch();
        loadSuppliers();
    }

    /**
     * Configures the supplier table columns.
     *
     * Each table column is linked to the correct supplier value so that the
     * supplier ID, supplier name, contact name, telephone number, email address
     * and notes are displayed in the table.
     */
    private void configureTableColumns() {
        supplierIdColumn.setCellValueFactory(data
                -> new SimpleIntegerProperty(
                        data.getValue().getSupplierId()
                )
        );

        supplierNameColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getSupplierName())
                )
        );

        contactNameColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getContactName())
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

        notesColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getNotes())
                )
        );
    }

    /**
     * Shows the keyboard shortcuts available on the suppliers screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Suppliers Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + A: Add supplier\n"
                + "Alt + R: Refresh suppliers\n"
                + "Alt + D: Delete selected supplier\n"
                + "Alt + B: Back to Dashboard"
        );

        alert.showAndWait();
    }

    /**
     * Configures searching and sorting for the supplier table.
     *
     * The search field filters the master supplier list and the sorted list
     * keeps table column sorting available.
     */
    private void configureSearch() {
        FilteredList<Supplier> filteredSuppliers
                = new FilteredList<>(masterSuppliers, supplier -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue)
                -> filteredSuppliers.setPredicate(supplier
                        -> matchesSearch(supplier, newValue)
                )
        );

        SortedList<Supplier> sortedSuppliers
                = new SortedList<>(filteredSuppliers);

        sortedSuppliers.comparatorProperty()
                .bind(supplierTable.comparatorProperty());

        supplierTable.setItems(sortedSuppliers);
    }

    /**
     * Checks whether a supplier matches the search text.
     *
     * The search checks the supplier ID, supplier name, contact name, telephone
     * number, email address and notes.
     *
     * @param supplier the supplier to check
     * @param searchText the search text entered by the user
     * @return true if the supplier matches the search text, otherwise false
     */
    private boolean matchesSearch(
            Supplier supplier,
            String searchText
    ) {
        if (supplier == null) {
            return false;
        }

        String searchValue = safeText(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(supplier.getSupplierId()),
                searchValue
        )
                || containsSearchValue(
                        supplier.getSupplierName(),
                        searchValue
                )
                || containsSearchValue(
                        supplier.getContactName(),
                        searchValue
                )
                || containsSearchValue(
                        supplier.getPhone(),
                        searchValue
                )
                || containsSearchValue(
                        supplier.getEmail(),
                        searchValue
                )
                || containsSearchValue(
                        supplier.getNotes(),
                        searchValue
                );
    }

    /**
     * Checks whether a text value contains the search value.
     *
     * Null values are converted into an empty string before checking.
     *
     * @param value the text value to check
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
     * Adds a new supplier using the values entered in the form.
     *
     * The supplier name is required. Optional contact name, telephone number,
     * email address and notes fields are converted to null when left blank.
     */
    @FXML
    private void addSupplier() {
        String supplierName = supplierNameField.getText();
        String contactName = contactNameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String notes = notesField.getText();

        if (supplierName == null || supplierName.isBlank()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Supplier name is required."
            );

            return;
        }

        try {
            Supplier supplier
                    = new Supplier(
                            0,
                            supplierName.trim(),
                            blankToNull(contactName),
                            blankToNull(phone),
                            blankToNull(email),
                            blankToNull(notes)
                    );

            supplierDAO.addSupplier(supplier);

            clearFields();
            loadSuppliers();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Supplier added successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to add supplier."
            );
        }
    }

    /**
     * Loads all suppliers from the database into the supplier table.
     *
     * The master supplier list is refreshed and the current table selection is
     * cleared. If loading fails, an error message is shown to the user.
     */
    @FXML
    private void loadSuppliers() {
        try {
            masterSuppliers.setAll(
                    supplierDAO.getAllSuppliers()
            );

            supplierTable.getSelectionModel().clearSelection();

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to load suppliers."
            );
        }
    }

    /**
     * Deletes the selected supplier.
     *
     * A supplier must be selected before this action can be completed. If a
     * supplier is selected, it is deleted from the database and the table is
     * refreshed.
     */
    @FXML
    private void deleteSelectedSupplier() {
        Supplier selectedSupplier
                = supplierTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedSupplier == null) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Please select a supplier to delete."
            );

            return;
        }

        try {
            supplierDAO.deleteSupplier(
                    selectedSupplier.getSupplierId()
            );

            loadSuppliers();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Supplier deleted successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to delete supplier."
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
     * Clears all supplier form fields.
     *
     * This resets the supplier name, contact name, telephone number, email
     * address and notes fields ready for a new supplier to be entered.
     */
    private void clearFields() {
        supplierNameField.clear();
        contactNameField.clear();
        phoneField.clear();
        emailField.clear();
        notesField.clear();
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
     * Converts blank text into null.
     *
     * This is used so optional supplier fields are stored as null when the user
     * leaves them empty.
     *
     * @param value the text value to check
     * @return null if the value is blank, otherwise the trimmed value
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    /**
     * Shows a message dialog to the user.
     *
     * This is used for validation errors, loading errors, deletion results and
     * general information messages on the suppliers screen.
     *
     * @param type the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle("Suppliers");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
