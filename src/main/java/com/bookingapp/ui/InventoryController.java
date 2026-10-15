package com.bookingapp.ui;

import com.bookingapp.dao.InventoryDAO;
import com.bookingapp.model.InventoryItem;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the inventory screen.
 *
 * This class manages the user interface for viewing, adding, searching,
 * highlighting and deleting inventory items. It also supports unit conversion
 * fields, preferred supplier details, default unit costs and re-order
 * quantities.
 */
public class InventoryController {

    /**
     * Text field used to enter the inventory item name.
     */
    @FXML
    private TextField nameField;

    /**
     * Text field used to enter the base unit used for the inventory item.
     */
    @FXML
    private TextField unitField;

    /**
     * Text field used to enter the purchase unit, such as bag, box or bottle.
     */
    @FXML
    private TextField purchaseUnitField;

    /**
     * Text field used to enter the recipe unit, such as grams, millilitres or
     * individual pieces.
     */
    @FXML
    private TextField recipeUnitField;

    /**
     * Text field used to enter the conversion factor between purchase and recipe
     * units.
     */
    @FXML
    private TextField conversionFactorField;

    /**
     * Text field used to enter the current inventory quantity.
     */
    @FXML
    private TextField quantityField;

    /**
     * Text field used to enter the re-order level.
     */
    @FXML
    private TextField reorderLevelField;

    /**
     * Text field used to enter the preferred supplier ID, if one is known.
     */
    @FXML
    private TextField preferredSupplierIdField;

    /**
     * Text field used to enter the default unit cost.
     */
    @FXML
    private TextField defaultUnitCostField;

    /**
     * Text field used to enter the normal re-order quantity.
     */
    @FXML
    private TextField reorderQuantityField;

    /**
     * Text field used to search the inventory table.
     */
    @FXML
    private TextField searchField;

    /**
     * Label used to display low-stock warnings and inventory status messages.
     */
    @FXML
    private Label warningLabel;

    /**
     * Table used to display inventory item records.
     */
    @FXML
    private TableView<InventoryItem> inventoryTable;

    /**
     * Table column used to display the inventory item ID.
     */
    @FXML
    private TableColumn<InventoryItem, Number> idColumn;

    /**
     * Table column used to display the inventory item name.
     */
    @FXML
    private TableColumn<InventoryItem, String> nameColumn;

    /**
     * Table column used to display the base unit.
     */
    @FXML
    private TableColumn<InventoryItem, String> unitColumn;

    /**
     * Table column used to display the purchase unit.
     */
    @FXML
    private TableColumn<InventoryItem, String> purchaseUnitColumn;

    /**
     * Table column used to display the recipe unit.
     */
    @FXML
    private TableColumn<InventoryItem, String> recipeUnitColumn;

    /**
     * Table column used to display the unit conversion factor.
     */
    @FXML
    private TableColumn<InventoryItem, Number> conversionFactorColumn;

    /**
     * Table column used to display the current inventory quantity.
     */
    @FXML
    private TableColumn<InventoryItem, Number> quantityColumn;

    /**
     * Table column used to display the re-order level.
     */
    @FXML
    private TableColumn<InventoryItem, Number> reorderColumn;

    /**
     * Table column used to display the preferred supplier ID.
     */
    @FXML
    private TableColumn<InventoryItem, Number> supplierColumn;

    /**
     * Table column used to display the default unit cost.
     */
    @FXML
    private TableColumn<InventoryItem, Number> defaultCostColumn;

    /**
     * Table column used to display the usual re-order quantity.
     */
    @FXML
    private TableColumn<InventoryItem, Number> reorderQuantityColumn;

    /**
     * Master list of inventory items used by the table, search and sort
     * functions.
     */
    private final ObservableList<InventoryItem> masterInventoryItems =
            FXCollections.observableArrayList();

    /**
     * Filtered list used to search inventory items.
     */
    private FilteredList<InventoryItem> filteredInventoryItems;

    /**
     * Data access object used for inventory database operations.
     */
    private final InventoryDAO inventoryDAO = new InventoryDAO();

    /**
     * Initialises the inventory screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the table columns, search behaviour, low-stock row
     * highlighting and initial inventory data.
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        configureSearch();
        configureRowHighlighting();
        loadInventoryItems();
    }

    /**
     * Shows the keyboard shortcuts available on the inventory screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Inventory Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + A: Add inventory item\n"
                + "Alt + C: Clear form\n"
                + "Alt + L: Show low-stock items\n"
                + "Alt + R: Refresh inventory\n"
                + "Alt + D: Delete selected inventory item\n"
                + "Alt + B: Back to Dashboard"
        );

        alert.showAndWait();
    }

    /**
     * Configures the inventory table columns.
     *
     * Each table column is linked to the correct inventory item value. Optional
     * columns are checked for null so the controller can still work if some
     * columns are not present in the FXML file.
     */
    private void configureTableColumns() {
        idColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(
                        data.getValue().getInventoryItemId()
                )
        );

        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getName())
                )
        );

        unitColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getUnit())
                )
        );

        purchaseUnitColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getPurchaseUnit())
                )
        );

        recipeUnitColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getRecipeUnit())
                )
        );

        conversionFactorColumn.setCellValueFactory(data ->
                new SimpleDoubleProperty(
                        data.getValue().getConversionFactor()
                )
        );

        quantityColumn.setCellValueFactory(data ->
                new SimpleDoubleProperty(
                        data.getValue().getCurrentQuantity()
                )
        );

        reorderColumn.setCellValueFactory(data ->
                new SimpleDoubleProperty(
                        data.getValue().getReorderLevel()
                )
        );

        if (supplierColumn != null) {
            supplierColumn.setCellValueFactory(data ->
                    new SimpleObjectProperty<>(
                            data.getValue().getPreferredSupplierId()
                    )
            );
        }

        if (defaultCostColumn != null) {
            defaultCostColumn.setCellValueFactory(data ->
                    new SimpleObjectProperty<>(
                            data.getValue().getDefaultUnitCost()
                    )
            );
        }

        if (reorderQuantityColumn != null) {
            reorderQuantityColumn.setCellValueFactory(data ->
                    new SimpleObjectProperty<>(
                            data.getValue().getReorderQuantity()
                    )
            );
        }
    }

    /**
     * Configures searching and sorting for the inventory table.
     *
     * The search field filters the master inventory list and the sorted list
     * keeps table column sorting available.
     */
    private void configureSearch() {
        filteredInventoryItems =
                new FilteredList<>(masterInventoryItems, item -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        filteredInventoryItems.setPredicate(item ->
                                matchesSearch(item, newValue)
                        )
        );

        SortedList<InventoryItem> sortedInventoryItems =
                new SortedList<>(filteredInventoryItems);

        sortedInventoryItems.comparatorProperty()
                .bind(inventoryTable.comparatorProperty());

        inventoryTable.setItems(sortedInventoryItems);
    }

    /**
     * Checks whether an inventory item matches the search text.
     *
     * The search checks the inventory item ID, name, units, conversion factor,
     * stock quantity, re-order level, preferred supplier ID, cost, re-order
     * quantity and low-stock status.
     *
     * @param item       the inventory item to check
     * @param searchText the search text entered by the user
     * @return true if the item matches the search text, otherwise false
     */
    private boolean matchesSearch(
            InventoryItem item,
            String searchText
    ) {
        if (item == null) {
            return false;
        }

        String searchValue = safeText(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(item.getInventoryItemId()),
                searchValue
        )
                || containsSearchValue(
                        item.getName(),
                        searchValue
                )
                || containsSearchValue(
                        item.getUnit(),
                        searchValue
                )
                || containsSearchValue(
                        item.getPurchaseUnit(),
                        searchValue
                )
                || containsSearchValue(
                        item.getRecipeUnit(),
                        searchValue
                )
                || containsSearchValue(
                        String.valueOf(item.getConversionFactor()),
                        searchValue
                )
                || containsSearchValue(
                        String.valueOf(item.getCurrentQuantity()),
                        searchValue
                )
                || containsSearchValue(
                        String.valueOf(item.getReorderLevel()),
                        searchValue
                )
                || containsSearchValue(
                        item.getPreferredSupplierId() == null
                                ? ""
                                : String.valueOf(
                                        item.getPreferredSupplierId()
                                ),
                        searchValue
                )
                || containsSearchValue(
                        item.getDefaultUnitCost() == null
                                ? ""
                                : String.valueOf(
                                        item.getDefaultUnitCost()
                                ),
                        searchValue
                )
                || containsSearchValue(
                        item.getReorderQuantity() == null
                                ? ""
                                : String.valueOf(
                                        item.getReorderQuantity()
                                ),
                        searchValue
                )
                || containsSearchValue(
                        item.isLowStock() ? "low stock" : "in stock",
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
     * Configures row highlighting for low-stock inventory items.
     *
     * Low-stock rows are given a light warning background so the user can see
     * which items need attention.
     */
    private void configureRowHighlighting() {
        inventoryTable.setRowFactory(tableView -> new TableRow<>() {

            /**
             * Updates the style of a table row.
             *
             * Low-stock items are highlighted. Empty rows and normal stock rows
             * use the default table style.
             *
             * @param item  the inventory item displayed in the row
             * @param empty whether the row is empty
             */
            @Override
            protected void updateItem(
                    InventoryItem item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");

                } else if (item.isLowStock()) {
                    setStyle("-fx-background-color: #ffe5e5;");

                } else {
                    setStyle("");
                }
            }
        });
    }

    /**
     * Adds a new inventory item.
     *
     * The method validates the required fields, parses the numeric fields,
     * applies default units where optional unit fields are blank and saves the
     * new inventory item to the database.
     */
    @FXML
    private void addInventoryItem() {
        String name = nameField.getText();
        String unit = unitField.getText();
        String purchaseUnit = purchaseUnitField.getText();
        String recipeUnit = recipeUnitField.getText();
        String conversionFactorText = conversionFactorField.getText();
        String quantityText = quantityField.getText();
        String reorderText = reorderLevelField.getText();

        String preferredSupplierIdText =
                preferredSupplierIdField != null
                        ? preferredSupplierIdField.getText()
                        : null;

        String defaultUnitCostText =
                defaultUnitCostField != null
                        ? defaultUnitCostField.getText()
                        : null;

        String reorderQuantityText =
                reorderQuantityField != null
                        ? reorderQuantityField.getText()
                        : null;

        if (name == null || name.isBlank()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Item name is required."
            );

            return;
        }

        if (unit == null || unit.isBlank()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Unit is required."
            );

            return;
        }

        try {
            double quantity =
                    Double.parseDouble(quantityText);

            double reorderLevel =
                    Double.parseDouble(reorderText);

            double conversionFactor =
                    parseConversionFactor(conversionFactorText);

            Integer preferredSupplierId =
                    parseOptionalInteger(preferredSupplierIdText);

            Double defaultUnitCost =
                    parseOptionalDouble(defaultUnitCostText);

            Double reorderQuantity =
                    parseOptionalDouble(reorderQuantityText);

            InventoryItem item =
                    new InventoryItem(
                            0,
                            name,
                            unit,
                            quantity,
                            reorderLevel,
                            true
                    );

            item.setPurchaseUnit(
                    blankToDefault(
                            purchaseUnit,
                            unit
                    )
            );

            item.setRecipeUnit(
                    blankToDefault(
                            recipeUnit,
                            unit
                    )
            );

            item.setConversionFactor(conversionFactor);
            item.setPreferredSupplierId(preferredSupplierId);
            item.setDefaultUnitCost(defaultUnitCost);
            item.setReorderQuantity(reorderQuantity);

            inventoryDAO.addInventoryItem(item);

            clearFields();
            loadInventoryItems();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Inventory item added successfully."
            );

        } catch (NumberFormatException e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Quantity, re-order level, conversion factor, supplier ID, "
                            + "default unit cost and re-order quantity must be "
                            + "valid numbers."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error adding inventory item."
            );
        }
    }

    /**
     * Loads all inventory items from the database.
     *
     * The master inventory list is refreshed and the warning label is updated
     * to show whether any items are at or below their re-order level.
     */
    @FXML
    private void loadInventoryItems() {
        try {
            masterInventoryItems.setAll(
                    inventoryDAO.getAllInventoryItems()
            );

            updateWarningLabelForAllItems();

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading inventory items."
            );
        }
    }

    /**
     * Loads only low-stock inventory items.
     *
     * This filters the table to show items that need attention because their
     * current quantity is at or below the re-order level.
     */
    @FXML
    private void loadLowStockItems() {
        try {
            masterInventoryItems.setAll(
                    inventoryDAO.getLowStockItems()
            );

            if (masterInventoryItems.isEmpty()) {
                warningLabel.setText("No low-stock items.");

            } else {
                warningLabel.setText("Showing low-stock items only.");
            }

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading low-stock items."
            );
        }
    }

    /**
     * Deletes the selected inventory item.
     *
     * An inventory item must be selected before this action can be completed.
     * The table is refreshed after the item has been deleted.
     */
    @FXML
    private void deleteSelectedInventoryItem() {
        InventoryItem selectedItem =
                inventoryTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedItem == null) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Please select an inventory item to delete."
            );

            return;
        }

        try {
            inventoryDAO.deleteInventoryItem(
                    selectedItem.getInventoryItemId()
            );

            loadInventoryItems();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Inventory item deleted successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error deleting inventory item."
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
     * Updates the warning label after all inventory items have been loaded.
     *
     * The label shows the number of items that are at or below their re-order
     * level. If there are no low-stock items, a clear message is shown.
     */
    private void updateWarningLabelForAllItems() {
        long lowStockCount =
                masterInventoryItems
                        .stream()
                        .filter(InventoryItem::isLowStock)
                        .count();

        if (lowStockCount > 0) {
            warningLabel.setText(
                    "Warning: "
                            + lowStockCount
                            + " item(s) are at or below re-order level."
            );

        } else {
            warningLabel.setText("No low-stock items.");
        }
    }

    /**
     * Parses an optional integer value.
     *
     * Blank values are treated as null because the field is optional.
     *
     * @param value the text value to parse
     * @return the parsed integer, or null if the value is blank
     */
    private Integer parseOptionalInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Integer.parseInt(value.trim());
    }

    /**
     * Parses an optional decimal value.
     *
     * Blank values are treated as null because the field is optional.
     *
     * @param value the text value to parse
     * @return the parsed decimal value, or null if the value is blank
     */
    private Double parseOptionalDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value.trim());
    }

    /**
     * Parses the unit conversion factor.
     *
     * If no conversion factor is entered, the method defaults to 1.0. If a
     * value is entered, it must be greater than zero.
     *
     * @param value the conversion factor text entered by the user
     * @return the parsed conversion factor
     */
    private double parseConversionFactor(String value) {
        if (value == null || value.isBlank()) {
            return 1.0;
        }

        double parsedValue =
                Double.parseDouble(value.trim());

        if (parsedValue <= 0) {
            throw new NumberFormatException(
                    "Conversion factor must be greater than zero."
            );
        }

        return parsedValue;
    }

    /**
     * Returns a default value when a text field is blank.
     *
     * This is used so purchase and recipe units can default to the base unit
     * when the user leaves the optional fields blank.
     *
     * @param value        the entered value
     * @param defaultValue the value to use when the entered value is blank
     * @return the entered value, or the default value if the entered value is blank
     */
    private String blankToDefault(
            String value,
            String defaultValue
    ) {
        if (value == null || value.isBlank()) {
            return safeText(defaultValue);
        }

        return value.trim();
    }

    /**
     * Clears the inventory form fields.
     *
     * Optional fields are checked for null so the method can safely run even if
     * some optional controls are not present in the FXML file.
     */
    @FXML
    private void clearFields() {
        nameField.clear();
        unitField.clear();
        purchaseUnitField.clear();
        recipeUnitField.clear();
        conversionFactorField.clear();
        quantityField.clear();
        reorderLevelField.clear();

        if (preferredSupplierIdField != null) {
            preferredSupplierIdField.clear();
        }

        if (defaultUnitCostField != null) {
            defaultUnitCostField.clear();
        }

        if (reorderQuantityField != null) {
            reorderQuantityField.clear();
        }

        nameField.requestFocus();
    }

    /**
     * Safely trims text.
     *
     * Null values are converted into an empty string so that search, validation
     * and display logic can handle text safely.
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
     * general information messages on the inventory screen.
     *
     * @param type    the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle("Inventory");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}