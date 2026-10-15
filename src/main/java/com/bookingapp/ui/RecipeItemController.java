package com.bookingapp.ui;

import com.bookingapp.dao.InventoryDAO;
import com.bookingapp.dao.ProductDAO;
import com.bookingapp.dao.RecipeItemDAO;
import com.bookingapp.model.InventoryItem;
import com.bookingapp.model.Product;
import com.bookingapp.model.RecipeItem;

import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the recipe items screen.
 *
 * This class manages the user interface for linking products to the inventory
 * items required to make them. It allows the user to add recipe items, search
 * existing recipe items, view recipe records in a table, delete selected recipe
 * items and return to the dashboard.
 */
public class RecipeItemController {

    /**
     * Combo box used to select the product linked to the recipe item.
     */
    @FXML
    private ComboBox<Product> productComboBox;

    /**
     * Combo box used to select the inventory item required for the product.
     */
    @FXML
    private ComboBox<InventoryItem> inventoryItemComboBox;

    /**
     * Text field used to enter the quantity of the inventory item required per
     * product unit.
     */
    @FXML
    private TextField quantityRequiredField;

    /**
     * Text field used to search the recipe item table.
     */
    @FXML
    private TextField searchField;

    /**
     * Table used to display recipe item records.
     */
    @FXML
    private TableView<RecipeItem> recipeTable;

    /**
     * Table column used to display the recipe item ID.
     */
    @FXML
    private TableColumn<RecipeItem, Number> idColumn;

    /**
     * Table column used to display the product name.
     */
    @FXML
    private TableColumn<RecipeItem, String> productColumn;

    /**
     * Table column used to display the inventory item name.
     */
    @FXML
    private TableColumn<RecipeItem, String> inventoryColumn;

    /**
     * Table column used to display the quantity required per unit.
     */
    @FXML
    private TableColumn<RecipeItem, Number> quantityColumn;

    /**
     * Master list of recipe items used by the table, search and sort functions.
     */
    private final ObservableList<RecipeItem> masterRecipeItems =
            FXCollections.observableArrayList();

    /**
     * Data access object used for recipe item database operations.
     */
    private final RecipeItemDAO recipeItemDAO = new RecipeItemDAO();

    /**
     * Data access object used for product database operations.
     */
    private final ProductDAO productDAO = new ProductDAO();

    /**
     * Data access object used for inventory item database operations.
     */
    private final InventoryDAO inventoryDAO = new InventoryDAO();

    /**
     * Initialises the recipe items screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the table columns, configures searching and loads the
     * product, inventory item and recipe item records from the database.
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        configureSearch();

        loadProducts();
        loadInventoryItems();
        loadRecipeItems();
    }

    /**
     * Shows the keyboard shortcuts available on the recipe items screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Recipe Items Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + A: Add recipe item\n"
                + "Alt + D: Delete selected recipe item\n"
                + "Alt + R: Refresh recipe items\n"
                + "Alt + B: Back to Dashboard"
        );

        alert.showAndWait();
    }

    /**
     * Configures the recipe item table columns.
     *
     * Each table column is linked to the correct recipe item value so that the
     * recipe item ID, product name, inventory item name and required quantity
     * are displayed in the table.
     */
    private void configureTableColumns() {
        idColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(
                        data.getValue().getRecipeItemId()
                )
        );

        productColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getProductName())
                )
        );

        inventoryColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getInventoryItemName())
                )
        );

        quantityColumn.setCellValueFactory(data ->
                new SimpleDoubleProperty(
                        data.getValue().getQuantityRequiredPerUnit()
                )
        );
    }

    /**
     * Configures searching and sorting for the recipe item table.
     *
     * The search field filters the master recipe item list and the sorted list
     * keeps table column sorting available.
     */
    private void configureSearch() {
        FilteredList<RecipeItem> filteredRecipeItems =
                new FilteredList<>(masterRecipeItems, recipeItem -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        filteredRecipeItems.setPredicate(recipeItem ->
                                matchesSearch(recipeItem, newValue)
                        )
        );

        SortedList<RecipeItem> sortedRecipeItems =
                new SortedList<>(filteredRecipeItems);

        sortedRecipeItems.comparatorProperty()
                .bind(recipeTable.comparatorProperty());

        recipeTable.setItems(sortedRecipeItems);
    }

    /**
     * Checks whether a recipe item matches the search text.
     *
     * The search checks the recipe item ID, product name, inventory item name
     * and quantity required per unit.
     *
     * @param recipeItem the recipe item to check
     * @param searchText the search text entered by the user
     * @return true if the recipe item matches the search text, otherwise false
     */
    private boolean matchesSearch(
            RecipeItem recipeItem,
            String searchText
    ) {
        if (recipeItem == null) {
            return false;
        }

        String searchValue = safeText(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(recipeItem.getRecipeItemId()),
                searchValue
        )
                || containsSearchValue(
                        recipeItem.getProductName(),
                        searchValue
                )
                || containsSearchValue(
                        recipeItem.getInventoryItemName(),
                        searchValue
                )
                || containsSearchValue(
                        String.valueOf(
                                recipeItem.getQuantityRequiredPerUnit()
                        ),
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
     * Loads all products into the product combo box.
     *
     * These products are used when creating recipe items.
     */
    private void loadProducts() {
        try {
            productComboBox.setItems(
                    FXCollections.observableArrayList(
                            productDAO.getAllProducts()
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading products."
            );
        }
    }

    /**
     * Loads all inventory items into the inventory item combo box.
     *
     * These inventory items are used when defining what stock is required for a
     * product recipe.
     */
    private void loadInventoryItems() {
        try {
            inventoryItemComboBox.setItems(
                    FXCollections.observableArrayList(
                            inventoryDAO.getAllInventoryItems()
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading inventory items."
            );
        }
    }

    /**
     * Adds a new recipe item using the values selected in the form.
     *
     * The method validates that a product and inventory item have been selected
     * and that the quantity required is a number. If validation passes, the
     * recipe item is saved to the database and the table is refreshed.
     */
    @FXML
    private void addRecipeItem() {
        try {
            Product selectedProduct =
                    productComboBox.getValue();

            InventoryItem selectedInventoryItem =
                    inventoryItemComboBox.getValue();

            if (selectedProduct == null) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "Please select a product."
                );

                return;
            }

            if (selectedInventoryItem == null) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "Please select an inventory item."
                );

                return;
            }

            double quantityRequired =
                    Double.parseDouble(
                            quantityRequiredField.getText()
                    );

            RecipeItem recipeItem =
                    new RecipeItem(
                            0,
                            selectedProduct.getProductId(),
                            selectedInventoryItem.getInventoryItemId(),
                            quantityRequired
                    );

            recipeItemDAO.addRecipeItem(recipeItem);

            productComboBox.setValue(null);
            inventoryItemComboBox.setValue(null);
            quantityRequiredField.clear();

            loadRecipeItems();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Recipe item added successfully."
            );

        } catch (NumberFormatException e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Quantity must be a number."
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
                    "Error adding recipe item."
            );
        }
    }

    /**
     * Loads all recipe items from the database into the table.
     *
     * The master recipe item list is refreshed and the current table selection
     * is cleared. If loading fails, an error message is shown to the user.
     */
    @FXML
    private void loadRecipeItems() {
        try {
            masterRecipeItems.setAll(
                    recipeItemDAO.getAllRecipeItems()
            );

            recipeTable.getSelectionModel().clearSelection();

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading recipe items."
            );
        }
    }

    /**
     * Deletes the selected recipe item.
     *
     * A recipe item must be selected before this action can be completed. If a
     * recipe item is selected, it is deleted from the database and the table is
     * refreshed.
     */
    @FXML
    private void deleteSelectedRecipeItem() {
        RecipeItem selectedItem =
                recipeTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedItem == null) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Please select a recipe item to delete."
            );

            return;
        }

        try {
            recipeItemDAO.deleteRecipeItem(
                    selectedItem.getRecipeItemId()
            );

            loadRecipeItems();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Recipe item deleted successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error deleting recipe item."
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
            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/com/bookingapp/ui/dashboard.fxml"
                            )
                    );

            Parent root = loader.load();

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert =
                    new Alert(Alert.AlertType.ERROR);

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
     * Null values are converted into an empty string so that search, table
     * display and validation logic can handle text safely.
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
     * general information messages on the recipe items screen.
     *
     * @param type    the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle("Recipe Items");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}