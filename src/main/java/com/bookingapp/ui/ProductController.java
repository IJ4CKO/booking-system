package com.bookingapp.ui;

import com.bookingapp.dao.ProductDAO;
import com.bookingapp.model.Product;

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
 * Controller for the products screen.
 *
 * This class manages the product user interface. It allows the user to add
 * products, search existing products, view product records in a table, delete
 * selected products and return to the dashboard.
 */
public class ProductController {

    /**
     * Text field used to enter the product name.
     */
    @FXML
    private TextField nameField;

    /**
     * Text area used to enter the product description.
     */
    @FXML
    private TextArea descriptionField;

    /**
     * Text field used to search the product table.
     */
    @FXML
    private TextField searchField;

    /**
     * Table used to display product records.
     */
    @FXML
    private TableView<Product> productTable;

    /**
     * Table column used to display the product ID.
     */
    @FXML
    private TableColumn<Product, Number> idColumn;

    /**
     * Table column used to display the product name.
     */
    @FXML
    private TableColumn<Product, String> nameColumn;

    /**
     * Table column used to display the product description.
     */
    @FXML
    private TableColumn<Product, String> descriptionColumn;

    /**
     * Master list of products used by the table, search and sort functions.
     */
    private final ObservableList<Product> masterProducts
            = FXCollections.observableArrayList();

    /**
     * Data access object used for product database operations.
     */
    private final ProductDAO productDAO = new ProductDAO();

    /**
     * Initialises the products screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the table columns, configures searching and loads the
     * product records from the database.
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        configureSearch();
        loadProducts();
    }

    /**
     * Configures the product table columns.
     *
     * Each table column is linked to the correct product value so that the
     * product ID, product name and product description are displayed in the
     * table.
     */
    private void configureTableColumns() {
        idColumn.setCellValueFactory(data
                -> new SimpleIntegerProperty(data.getValue().getProductId())
        );

        nameColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getName())
                )
        );

        descriptionColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getDescription())
                )
        );
    }

    /**
     * Configures searching and sorting for the product table.
     *
     * The search field filters the master product list and the sorted list
     * keeps table column sorting available.
     */
    private void configureSearch() {
        FilteredList<Product> filteredProducts
                = new FilteredList<>(masterProducts, product -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue)
                -> filteredProducts.setPredicate(product
                        -> matchesSearch(product, newValue)
                )
        );

        SortedList<Product> sortedProducts
                = new SortedList<>(filteredProducts);

        sortedProducts.comparatorProperty()
                .bind(productTable.comparatorProperty());

        productTable.setItems(sortedProducts);
    }

    /**
     * Checks whether a product matches the search text.
     *
     * The search checks the product ID, product name and product description.
     *
     * @param product the product to check
     * @param searchText the search text entered by the user
     * @return true if the product matches the search text, otherwise false
     */
    private boolean matchesSearch(
            Product product,
            String searchText
    ) {
        if (product == null) {
            return false;
        }

        String searchValue = safeText(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(product.getProductId()),
                searchValue
        )
                || containsSearchValue(
                        product.getName(),
                        searchValue
                )
                || containsSearchValue(
                        product.getDescription(),
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
     * Adds a new product using the values entered in the form.
     *
     * The product name is required. If validation passes, the product is saved
     * to the database, the form fields are cleared and the product table is
     * refreshed.
     */
    @FXML
    private void addProduct() {
        String name = nameField.getText();
        String description = descriptionField.getText();

        if (name == null || name.isBlank()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Product name is required."
            );

            return;
        }

        try {
            Product product
                    = new Product(
                            0,
                            name,
                            description,
                            true
                    );

            productDAO.addProduct(product);

            nameField.clear();
            descriptionField.clear();

            loadProducts();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Product added successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error adding product."
            );
        }
    }

    /**
     * Shows the keyboard shortcuts available on the products screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Products Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + A: Add product\n"
                + "Alt + D: Delete selected product\n"
                + "Alt + R: Refresh products\n"
                + "Alt + B: Back to Dashboard"
        );

        alert.showAndWait();
    }

    /**
     * Loads all products from the database into the table.
     *
     * The master product list is refreshed and the current table selection is
     * cleared. If loading fails, an error message is shown to the user.
     */
    @FXML
    private void loadProducts() {
        try {
            masterProducts.setAll(
                    productDAO.getAllProducts()
            );

            productTable.getSelectionModel().clearSelection();

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading products."
            );
        }
    }

    /**
     * Deletes the selected product.
     *
     * A product must be selected before this action can be completed. If a
     * product is selected, it is deleted from the database and the table is
     * refreshed.
     */
    @FXML
    private void deleteSelectedProduct() {
        Product selectedProduct
                = productTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedProduct == null) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Please select a product to delete."
            );

            return;
        }

        try {
            productDAO.deleteProduct(
                    selectedProduct.getProductId()
            );

            loadProducts();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Product deleted successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error deleting product."
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
     * general information messages on the products screen.
     *
     * @param type the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle("Products");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
