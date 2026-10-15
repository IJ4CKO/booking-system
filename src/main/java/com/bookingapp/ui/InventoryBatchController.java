package com.bookingapp.ui;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bookingapp.dao.DBConnection;
import com.bookingapp.dao.InventoryBatchDAO;
import com.bookingapp.dao.InventoryDAO;
import com.bookingapp.dao.StockMovementDAO;
import com.bookingapp.dao.SupplierDAO;
import com.bookingapp.model.InventoryBatch;
import com.bookingapp.model.InventoryItem;
import com.bookingapp.model.Supplier;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Controller for the inventory batches screen.
 *
 * This class manages the user interface for adding, updating, deleting,
 * searching and viewing inventory batches. It also updates overall inventory
 * quantities and stock movement records when batch quantities change.
 */
public class InventoryBatchController {

    /**
     * Small tolerance value used when comparing decimal stock quantities.
     */
    private static final double QUANTITY_TOLERANCE = 0.000001;

    /**
     * Data access object used for inventory batch database operations.
     */
    private final InventoryBatchDAO inventoryBatchDAO = new InventoryBatchDAO();

    /**
     * Data access object used for inventory item database operations.
     */
    private final InventoryDAO inventoryDAO = new InventoryDAO();

    /**
     * Data access object used for supplier database operations.
     */
    private final SupplierDAO supplierDAO = new SupplierDAO();

    /**
     * Data access object used for stock movement database operations.
     */
    private final StockMovementDAO stockMovementDAO = new StockMovementDAO();

    /**
     * Stores inventory items by inventory item ID for quick lookup.
     */
    private final Map<Integer, InventoryItem> inventoryItemsById = new HashMap<>();

    /**
     * Stores suppliers by supplier ID for quick lookup.
     */
    private final Map<Integer, Supplier> suppliersById = new HashMap<>();

    /**
     * Master list of inventory batches used by the table, search and sort
     * functions.
     */
    private final ObservableList<InventoryBatch> masterBatches =
            FXCollections.observableArrayList();

    /**
     * Filtered list used to search inventory batches.
     */
    private FilteredList<InventoryBatch> filteredBatches;

    /**
     * Combo box used to select the inventory item linked to the batch.
     */
    @FXML
    private ComboBox<InventoryItem> inventoryItemComboBox;

    /**
     * Combo box used to select the supplier linked to the batch.
     */
    @FXML
    private ComboBox<Supplier> supplierComboBox;

    /**
     * Text field used to enter the batch code.
     */
    @FXML
    private TextField batchCodeField;

    /**
     * Date picker used to select the purchase date.
     */
    @FXML
    private DatePicker purchaseDatePicker;

    /**
     * Date picker used to select the expiry date, if one applies.
     */
    @FXML
    private DatePicker expiryDatePicker;

    /**
     * Text field used to enter the quantity received.
     */
    @FXML
    private TextField quantityReceivedField;

    /**
     * Text field used to enter the quantity remaining.
     */
    @FXML
    private TextField quantityRemainingField;

    /**
     * Text field used to enter the unit cost.
     */
    @FXML
    private TextField unitCostField;

    /**
     * Text field used to search the inventory batch table.
     */
    @FXML
    private TextField searchField;

    /**
     * Table used to display inventory batches.
     */
    @FXML
    private TableView<InventoryBatch> batchTable;

    /**
     * Table column used to display the batch ID.
     */
    @FXML
    private TableColumn<InventoryBatch, Integer> idColumn;

    /**
     * Table column used to display the inventory item name.
     */
    @FXML
    private TableColumn<InventoryBatch, String> inventoryItemColumn;

    /**
     * Table column used to display the supplier name.
     */
    @FXML
    private TableColumn<InventoryBatch, String> supplierColumn;

    /**
     * Table column used to display the batch code.
     */
    @FXML
    private TableColumn<InventoryBatch, String> batchCodeColumn;

    /**
     * Table column used to display the purchase date.
     */
    @FXML
    private TableColumn<InventoryBatch, String> purchaseDateColumn;

    /**
     * Table column used to display the expiry date.
     */
    @FXML
    private TableColumn<InventoryBatch, String> expiryDateColumn;

    /**
     * Table column used to display the quantity received.
     */
    @FXML
    private TableColumn<InventoryBatch, Double> quantityReceivedColumn;

    /**
     * Table column used to display the quantity remaining.
     */
    @FXML
    private TableColumn<InventoryBatch, Double> quantityRemainingColumn;

    /**
     * Table column used to display the unit cost.
     */
    @FXML
    private TableColumn<InventoryBatch, Double> unitCostColumn;

    /**
     * Table column used to display the remaining value of the batch.
     */
    @FXML
    private TableColumn<InventoryBatch, Double> batchValueColumn;

    /**
     * Label used to display the total remaining stock value.
     */
    @FXML
    private Label totalStockValueLabel;

    /**
     * Initialises the inventory batches screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the table, combo boxes, search behaviour, table
     * selection handling and initial inventory batch data.
     */
    @FXML
    public void initialize() {
        batchTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        configureComboBoxes();
        configureTableColumns();
        configureSearch();
        configureTableSelection();

        purchaseDatePicker.setValue(LocalDate.now());

        loadReferenceData();
        loadBatches();
    }

    /**
 * Shows the keyboard shortcuts available on the inventory batches screen.
 *
 * This keeps the page less cluttered while still making keyboard support
 * available to users who cannot easily use a mouse.
 */
@FXML
private void showKeyboardShortcuts() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);

    alert.setTitle("Keyboard Shortcuts");
    alert.setHeaderText("Inventory Batches Keyboard Shortcuts");

    alert.setContentText(
            "Tab / Shift + Tab: Move between controls\n"
            + "Enter or Space: Select the focused button\n\n"
            + "Alt + K: Show keyboard shortcuts\n"
            + "Alt + A: Add batch\n"
            + "Alt + U: Update selected batch\n"
            + "Alt + C: Clear form\n"
            + "Alt + D: Delete selected batch\n"
            + "Alt + R: Refresh batches\n"
            + "Alt + B: Back to Dashboard"
    );

    alert.showAndWait();
}

    /**
     * Configures the inventory item and supplier combo boxes.
     */
    private void configureComboBoxes() {
        configureInventoryItemComboBox();
        configureSupplierComboBox();
    }

    /**
     * Configures how inventory items are displayed in the inventory item combo
     * box.
     *
     * Each inventory item is shown using its name and unit so the user can
     * clearly identify the item being selected.
     */
    private void configureInventoryItemComboBox() {
        inventoryItemComboBox.setCellFactory(listView -> new ListCell<>() {

            /**
             * Updates how inventory items are displayed in the drop-down list.
             *
             * @param item  the inventory item to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getUnit() + ")");
                }
            }
        });

        inventoryItemComboBox.setButtonCell(new ListCell<>() {

            /**
             * Updates how the selected inventory item is displayed.
             *
             * @param item  the selected inventory item
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(inventoryItemComboBox.getPromptText());
                } else {
                    setText(item.getName() + " (" + item.getUnit() + ")");
                }
            }
        });

        inventoryItemComboBox.setConverter(new StringConverter<>() {

            /**
             * Converts an inventory item into display text.
             *
             * @param item the inventory item to display
             * @return the readable inventory item name and unit
             */
            @Override
            public String toString(InventoryItem item) {
                if (item == null) {
                    return "";
                }

                return item.getName() + " (" + item.getUnit() + ")";
            }

            /**
             * Converts text back into an inventory item.
             *
             * Direct text conversion is not used because inventory items are
             * selected from existing records.
             *
             * @param text the entered text
             * @return null because typed conversion is not used
             */
            @Override
            public InventoryItem fromString(String text) {
                return null;
            }
        });
    }

    /**
     * Configures how suppliers are displayed in the supplier combo box.
     *
     * Suppliers are shown using their supplier name.
     */
    private void configureSupplierComboBox() {
        supplierComboBox.setCellFactory(listView -> new ListCell<>() {

            /**
             * Updates how suppliers are displayed in the drop-down list.
             *
             * @param supplier the supplier to display
             * @param empty    whether the cell is empty
             */
            @Override
            protected void updateItem(Supplier supplier, boolean empty) {
                super.updateItem(supplier, empty);

                if (empty || supplier == null) {
                    setText(null);
                } else {
                    setText(supplier.getSupplierName());
                }
            }
        });

        supplierComboBox.setButtonCell(new ListCell<>() {

            /**
             * Updates how the selected supplier is displayed.
             *
             * @param supplier the selected supplier
             * @param empty    whether the cell is empty
             */
            @Override
            protected void updateItem(Supplier supplier, boolean empty) {
                super.updateItem(supplier, empty);

                if (empty || supplier == null) {
                    setText(supplierComboBox.getPromptText());
                } else {
                    setText(supplier.getSupplierName());
                }
            }
        });

        supplierComboBox.setConverter(new StringConverter<>() {

            /**
             * Converts a supplier into display text.
             *
             * @param supplier the supplier to display
             * @return the supplier name
             */
            @Override
            public String toString(Supplier supplier) {
                if (supplier == null) {
                    return "";
                }

                return supplier.getSupplierName();
            }

            /**
             * Converts text back into a supplier.
             *
             * Direct text conversion is not used because suppliers are selected
             * from existing records.
             *
             * @param text the entered text
             * @return null because typed conversion is not used
             */
            @Override
            public Supplier fromString(String text) {
                return null;
            }
        });
    }

    /**
     * Configures the inventory batch table columns.
     *
     * Each table column is linked to the correct inventory batch value. Numeric
     * values are formatted as either quantities or money values.
     */
    private void configureTableColumns() {
        idColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getBatchId()
                )
        );

        inventoryItemColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(
                        getInventoryItemDisplayName(cellData.getValue())
                )
        );

        supplierColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(
                        getSupplierDisplayName(cellData.getValue())
                )
        );

        batchCodeColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(
                        safeText(cellData.getValue().getBatchCode())
                )
        );

        purchaseDateColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(
                        safeText(cellData.getValue().getPurchaseDate())
                )
        );

        expiryDateColumn.setCellValueFactory(cellData -> {
            String expiryDate = cellData.getValue().getExpiryDate();

            if (expiryDate == null || expiryDate.isBlank()) {
                return new ReadOnlyStringWrapper("No expiry date");
            }

            return new ReadOnlyStringWrapper(expiryDate);
        });

        quantityReceivedColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getQuantityReceived()
                )
        );

        quantityRemainingColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getQuantityRemaining()
                )
        );

        unitCostColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getUnitCost()
                )
        );

        batchValueColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getBatchValue()
                )
        );

        configureNumberColumn(quantityReceivedColumn, false);
        configureNumberColumn(quantityRemainingColumn, false);
        configureNumberColumn(unitCostColumn, true);
        configureNumberColumn(batchValueColumn, true);
    }

    /**
     * Configures how numeric table columns are displayed.
     *
     * The same method is used for quantity columns and currency columns.
     *
     * @param column   the table column to configure
     * @param currency true if the value should be formatted as money
     */
    private void configureNumberColumn(
            TableColumn<InventoryBatch, Double> column,
            boolean currency
    ) {
        column.setCellFactory(tableColumn -> new TableCell<>() {

            /**
             * Updates the displayed value for a numeric table cell.
             *
             * @param value the value to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                } else if (currency) {
                    setText(String.format("£%.2f", value));
                } else {
                    setText(formatQuantity(value));
                }
            }
        });
    }

    /**
     * Configures searching and sorting for the inventory batch table.
     *
     * The search field filters the master batch list and the sorted list keeps
     * table column sorting available.
     */
    private void configureSearch() {
        filteredBatches = new FilteredList<>(masterBatches, batch -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        filteredBatches.setPredicate(batch ->
                                matchesSearch(batch, newValue)
                        )
        );

        SortedList<InventoryBatch> sortedBatches =
                new SortedList<>(filteredBatches);

        sortedBatches.comparatorProperty()
                .bind(batchTable.comparatorProperty());

        batchTable.setItems(sortedBatches);
    }

    /**
     * Checks whether an inventory batch matches the search text.
     *
     * The search checks batch ID, inventory item, supplier, batch code,
     * purchase date, expiry date, quantities, costs and batch value.
     *
     * @param batch      the inventory batch to check
     * @param searchText the search text entered by the user
     * @return true if the batch matches the search text, otherwise false
     */
    private boolean matchesSearch(
            InventoryBatch batch,
            String searchText
    ) {
        if (batch == null) {
            return false;
        }

        String searchValue = safeText(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(batch.getBatchId()),
                searchValue
        )
                || containsSearchValue(
                        getInventoryItemDisplayName(batch),
                        searchValue
                )
                || containsSearchValue(
                        getSupplierDisplayName(batch),
                        searchValue
                )
                || containsSearchValue(
                        batch.getBatchCode(),
                        searchValue
                )
                || containsSearchValue(
                        batch.getPurchaseDate(),
                        searchValue
                )
                || containsSearchValue(
                        batch.getExpiryDate(),
                        searchValue
                )
                || containsSearchValue(
                        formatQuantity(batch.getQuantityReceived()),
                        searchValue
                )
                || containsSearchValue(
                        formatQuantity(batch.getQuantityRemaining()),
                        searchValue
                )
                || containsSearchValue(
                        String.format("£%.2f", batch.getUnitCost()),
                        searchValue
                )
                || containsSearchValue(
                        String.format("%.2f", batch.getUnitCost()),
                        searchValue
                )
                || containsSearchValue(
                        String.format("£%.2f", batch.getBatchValue()),
                        searchValue
                )
                || containsSearchValue(
                        String.format("%.2f", batch.getBatchValue()),
                        searchValue
                )
                || containsSearchValue(
                        batch.getExpiryDate() == null
                                || batch.getExpiryDate().isBlank()
                                ? "no expiry date"
                                : "",
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
     * Configures the table selection listener.
     *
     * When the user selects a batch in the table, the batch details are loaded
     * into the form so they can be viewed or updated.
     */
    private void configureTableSelection() {
        batchTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldBatch, newBatch) -> {
                    if (newBatch != null) {
                        populateForm(newBatch);
                    }
                });
    }

    /**
     * Loads inventory items and suppliers used by this screen.
     *
     * Inventory items and suppliers are stored in lookup maps so the table can
     * display names instead of only database IDs. The combo boxes are also
     * populated from this data.
     */
    private void loadReferenceData() {
        try {
            List<InventoryItem> inventoryItems =
                    inventoryDAO.getActiveInventoryItems();

            List<Supplier> suppliers =
                    supplierDAO.getAllSuppliers();

            inventoryItemsById.clear();

            for (InventoryItem inventoryItem : inventoryItems) {
                inventoryItemsById.put(
                        inventoryItem.getInventoryItemId(),
                        inventoryItem
                );
            }

            suppliersById.clear();

            for (Supplier supplier : suppliers) {
                suppliersById.put(
                        supplier.getSupplierId(),
                        supplier
                );
            }

            inventoryItemComboBox.setItems(
                    FXCollections.observableArrayList(inventoryItems)
            );

            supplierComboBox.setItems(
                    FXCollections.observableArrayList(suppliers)
            );

        } catch (Exception e) {
            showError(
                    "Unable to load inventory items and suppliers.",
                    e
            );
        }
    }

    /**
     * Loads all inventory batches from the database.
     *
     * The master batch list is refreshed and the total remaining stock value is
     * updated.
     */
    @FXML
    private void loadBatches() {
        try {
            List<InventoryBatch> batches =
                    inventoryBatchDAO.getAllBatches();

            masterBatches.setAll(batches);

            updateTotalStockValue();

        } catch (Exception e) {
            showError(
                    "Unable to load inventory batches.",
                    e
            );
        }
    }

    /**
     * Adds a new inventory batch.
     *
     * The batch is created from the form values. The batch record, overall
     * inventory quantity and stock movement record are saved in one database
     * transaction.
     */
    @FXML
    private void addBatch() {
        try {
            InventoryBatch newBatch = createBatchFromForm(0);
            double quantityToAdd = newBatch.getQuantityRemaining();

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    inventoryBatchDAO.addBatch(conn, newBatch);

                    inventoryDAO.updateInventoryQuantity(
                            conn,
                            newBatch.getInventoryItemId(),
                            quantityToAdd
                    );

                    stockMovementDAO.addStockMovement(
                            conn,
                            newBatch.getInventoryItemId(),
                            quantityToAdd,
                            null,
                            "PURCHASE",
                            "Inventory batch added: "
                                    + newBatch.getBatchCode()
                    );

                    conn.commit();

                } catch (Exception e) {
                    rollbackTransaction(conn, e);
                    throw e;

                } finally {
                    conn.setAutoCommit(true);
                }
            }

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Inventory batch added successfully."
            );

            clearForm();
            loadBatches();

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.WARNING,
                    e.getMessage()
            );

        } catch (Exception e) {
            showError(
                    "Unable to add the inventory batch.",
                    e
            );
        }
    }

    /**
     * Updates the selected inventory batch.
     *
     * The inventory item cannot be changed once a batch is selected. If the
     * remaining quantity changes, the overall inventory quantity and stock
     * movement records are adjusted in the same database transaction.
     */
    @FXML
    private void updateBatch() {
        InventoryBatch selectedBatch =
                batchTable.getSelectionModel().getSelectedItem();

        if (selectedBatch == null) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select an inventory batch to update."
            );

            return;
        }

        try {
            InventoryBatch updatedBatch =
                    createBatchFromForm(selectedBatch.getBatchId());

            updatedBatch.setInventoryItemId(
                    selectedBatch.getInventoryItemId()
            );

            double quantityDifference =
                    updatedBatch.getQuantityRemaining()
                            - selectedBatch.getQuantityRemaining();

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    inventoryBatchDAO.updateBatch(conn, updatedBatch);

                    if (Math.abs(quantityDifference) > QUANTITY_TOLERANCE) {
                        inventoryDAO.updateInventoryQuantity(
                                conn,
                                updatedBatch.getInventoryItemId(),
                                quantityDifference
                        );

                        stockMovementDAO.addStockMovement(
                                conn,
                                updatedBatch.getInventoryItemId(),
                                quantityDifference,
                                null,
                                "ADJUSTMENT",
                                "Batch quantity adjusted: "
                                        + updatedBatch.getBatchCode()
                        );
                    }

                    conn.commit();

                } catch (Exception e) {
                    rollbackTransaction(conn, e);
                    throw e;

                } finally {
                    conn.setAutoCommit(true);
                }
            }

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Inventory batch updated successfully."
            );

            clearForm();
            loadBatches();

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.WARNING,
                    e.getMessage()
            );

        } catch (Exception e) {
            showError(
                    "Unable to update the inventory batch.",
                    e
            );
        }
    }

    /**
     * Deletes the selected inventory batch.
     *
     * A batch can only be deleted if none of its received stock has already been
     * used. Any remaining stock is removed from the overall inventory quantity
     * and a stock movement record is added.
     */
    @FXML
    private void deleteSelectedBatch() {
        InventoryBatch selectedBatch =
                batchTable.getSelectionModel().getSelectedItem();

        if (selectedBatch == null) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select an inventory batch to delete."
            );

            return;
        }

        double quantityAlreadyUsed =
                selectedBatch.getQuantityReceived()
                        - selectedBatch.getQuantityRemaining();

        if (Math.abs(quantityAlreadyUsed) > QUANTITY_TOLERANCE) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "This batch cannot be deleted because some of its "
                            + "stock has already been used."
            );

            return;
        }

        Alert confirmation =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Delete Inventory Batch");
        confirmation.setHeaderText(
                "Delete batch "
                        + selectedBatch.getBatchCode()
                        + "?"
        );

        confirmation.setContentText(
                "The remaining stock in this batch will also be "
                        + "removed from the overall inventory quantity."
        );

        Optional<ButtonType> result =
                confirmation.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                double quantityToRemove =
                        selectedBatch.getQuantityRemaining();

                inventoryBatchDAO.deleteBatch(
                        conn,
                        selectedBatch.getBatchId()
                );

                if (quantityToRemove > QUANTITY_TOLERANCE) {
                    inventoryDAO.updateInventoryQuantity(
                            conn,
                            selectedBatch.getInventoryItemId(),
                            -quantityToRemove
                    );

                    stockMovementDAO.addStockMovement(
                            conn,
                            selectedBatch.getInventoryItemId(),
                            -quantityToRemove,
                            null,
                            "ADJUSTMENT",
                            "Unused inventory batch deleted: "
                                    + selectedBatch.getBatchCode()
                    );
                }

                conn.commit();

            } catch (Exception e) {
                rollbackTransaction(conn, e);
                throw e;

            } finally {
                conn.setAutoCommit(true);
            }

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Inventory batch deleted successfully."
            );

            clearForm();
            loadBatches();

        } catch (Exception e) {
            showError(
                    "Unable to delete the inventory batch. "
                            + "It may be linked to existing records.",
                    e
            );
        }
    }

    /**
     * Creates an inventory batch object from the form values.
     *
     * The method validates the selected inventory item, batch code, purchase
     * date, optional expiry date, quantities and unit cost before creating the
     * batch object.
     *
     * @param batchId the batch ID to use, or 0 for a new batch
     * @return the inventory batch created from the form
     */
    private InventoryBatch createBatchFromForm(int batchId) {
        InventoryItem selectedInventoryItem =
                inventoryItemComboBox.getValue();

        Supplier selectedSupplier =
                supplierComboBox.getValue();

        String batchCode =
                batchCodeField.getText() == null
                        ? ""
                        : batchCodeField.getText().trim();

        LocalDate purchaseDate =
                purchaseDatePicker.getValue();

        LocalDate expiryDate =
                expiryDatePicker.getValue();

        if (selectedInventoryItem == null) {
            throw new IllegalArgumentException(
                    "Select an inventory item."
            );
        }

        if (batchCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter a batch code."
            );
        }

        if (purchaseDate == null) {
            throw new IllegalArgumentException(
                    "Select a purchase date."
            );
        }

        if (expiryDate != null && expiryDate.isBefore(purchaseDate)) {
            throw new IllegalArgumentException(
                    "The expiry date cannot be before the purchase date."
            );
        }

        double quantityReceived =
                parsePositiveDouble(
                        quantityReceivedField.getText(),
                        "Quantity received"
                );

        double quantityRemaining =
                parseNonNegativeDouble(
                        quantityRemainingField.getText(),
                        "Quantity remaining"
                );

        double unitCost =
                parseNonNegativeDouble(
                        unitCostField.getText(),
                        "Unit cost"
                );

        if (quantityRemaining > quantityReceived + QUANTITY_TOLERANCE) {
            throw new IllegalArgumentException(
                    "Quantity remaining cannot be greater than "
                            + "quantity received."
            );
        }

        Integer supplierId =
                selectedSupplier == null
                        ? null
                        : selectedSupplier.getSupplierId();

        return new InventoryBatch(
                batchId,
                selectedInventoryItem.getInventoryItemId(),
                supplierId,
                batchCode,
                purchaseDate.toString(),
                expiryDate == null ? null : expiryDate.toString(),
                quantityReceived,
                quantityRemaining,
                unitCost
        );
    }

    /**
     * Populates the form with the selected batch details.
     *
     * This allows the selected batch to be edited. The inventory item combo box
     * is disabled so that the batch remains linked to its original inventory
     * item.
     *
     * @param batch the selected inventory batch
     */
    private void populateForm(InventoryBatch batch) {
        inventoryItemComboBox.setValue(
                inventoryItemsById.get(batch.getInventoryItemId())
        );

        Integer supplierId = batch.getSupplierId();

        supplierComboBox.setValue(
                supplierId == null
                        ? null
                        : suppliersById.get(supplierId)
        );

        batchCodeField.setText(batch.getBatchCode());
        purchaseDatePicker.setValue(parseDate(batch.getPurchaseDate()));
        expiryDatePicker.setValue(parseDate(batch.getExpiryDate()));
        quantityReceivedField.setText(formatQuantity(batch.getQuantityReceived()));
        quantityRemainingField.setText(formatQuantity(batch.getQuantityRemaining()));
        unitCostField.setText(String.format("%.2f", batch.getUnitCost()));

        inventoryItemComboBox.setDisable(true);
    }

    /**
     * Clears the batch form.
     *
     * The current table selection is cleared, the inventory item combo box is
     * re-enabled and all form fields are reset ready for a new batch.
     */
    @FXML
    private void clearForm() {
        batchTable.getSelectionModel().clearSelection();

        inventoryItemComboBox.setDisable(false);
        inventoryItemComboBox.setValue(null);
        supplierComboBox.setValue(null);

        batchCodeField.clear();
        purchaseDatePicker.setValue(LocalDate.now());
        expiryDatePicker.setValue(null);
        quantityReceivedField.clear();
        quantityRemainingField.clear();
        unitCostField.clear();

        inventoryItemComboBox.requestFocus();
    }

    /**
     * Updates the total remaining stock value label.
     *
     * If the value cannot be loaded, an unavailable message is shown instead.
     */
    private void updateTotalStockValue() {
        try {
            double totalStockValue =
                    inventoryBatchDAO.getTotalStockValue();

            totalStockValueLabel.setText(
                    String.format(
                            "Total remaining stock value: £%.2f",
                            totalStockValue
                    )
            );

        } catch (Exception e) {
            totalStockValueLabel.setText(
                    "Total remaining stock value: unavailable"
            );
        }
    }

    /**
     * Gets the display name for the inventory item linked to a batch.
     *
     * If the inventory item cannot be found, the inventory item ID is shown as
     * fallback text.
     *
     * @param batch the inventory batch to read
     * @return the inventory item name or fallback item ID text
     */
    private String getInventoryItemDisplayName(InventoryBatch batch) {
        int inventoryItemId = batch.getInventoryItemId();

        InventoryItem inventoryItem =
                inventoryItemsById.get(inventoryItemId);

        if (inventoryItem == null) {
            return "Item ID " + inventoryItemId;
        }

        return inventoryItem.getName();
    }

    /**
     * Gets the display name for the supplier linked to a batch.
     *
     * If no supplier is linked, "No supplier" is returned. If the supplier
     * cannot be found, the supplier ID is shown as fallback text.
     *
     * @param batch the inventory batch to read
     * @return the supplier name or fallback supplier text
     */
    private String getSupplierDisplayName(InventoryBatch batch) {
        Integer supplierId = batch.getSupplierId();

        if (supplierId == null) {
            return "No supplier";
        }

        Supplier supplier = suppliersById.get(supplierId);

        if (supplier == null) {
            return "Supplier ID " + supplierId;
        }

        return supplier.getSupplierName();
    }

    /**
     * Parses and validates a positive decimal number.
     *
     * @param text      the text entered by the user
     * @param fieldName the field name used in validation messages
     * @return the parsed number
     */
    private double parsePositiveDouble(
            String text,
            String fieldName
    ) {
        double value =
                parseDouble(text, fieldName);

        if (value <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero."
            );
        }

        return value;
    }

    /**
     * Parses and validates a non-negative decimal number.
     *
     * @param text      the text entered by the user
     * @param fieldName the field name used in validation messages
     * @return the parsed number
     */
    private double parseNonNegativeDouble(
            String text,
            String fieldName
    ) {
        double value =
                parseDouble(text, fieldName);

        if (value < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative."
            );
        }

        return value;
    }

    /**
     * Parses a decimal number entered by the user.
     *
     * @param text      the text entered by the user
     * @param fieldName the field name used in validation messages
     * @return the parsed number
     */
    private double parseDouble(
            String text,
            String fieldName
    ) {
        if (text == null || text.trim().isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        try {
            return Double.parseDouble(text.trim());

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + " must be a valid number."
            );
        }
    }

    /**
     * Parses a stored date value.
     *
     * Stored dates are expected to use the default ISO database format. If the
     * value is blank, null is returned.
     *
     * @param dateText the stored date text
     * @return the parsed date, or null if the value is blank
     */
    private LocalDate parseDate(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return null;
        }

        return LocalDate.parse(dateText);
    }

    /**
     * Formats a quantity for display.
     *
     * Whole numbers are shown without decimal places. Other values are shown to
     * two decimal places.
     *
     * @param quantity the quantity to format
     * @return the formatted quantity
     */
    private String formatQuantity(double quantity) {
        if (Math.abs(quantity - Math.rint(quantity)) < QUANTITY_TOLERANCE) {
            return String.format("%.0f", quantity);
        }

        return String.format("%.2f", quantity);
    }

    /**
     * Rolls a database transaction back without hiding the original exception.
     *
     * If rollback fails, the rollback exception is added to the original
     * exception as a suppressed exception.
     *
     * @param conn              the active database connection
     * @param originalException the exception that caused the rollback
     */
    private void rollbackTransaction(
            Connection conn,
            Exception originalException
    ) {
        try {
            conn.rollback();

        } catch (Exception rollbackException) {
            originalException.addSuppressed(rollbackException);
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
     * This is used for validation errors, warnings, successful actions and
     * general messages on the inventory batches screen.
     *
     * @param alertType the type of alert to show
     * @param message   the message to display
     */
    private void showMessage(
            Alert.AlertType alertType,
            String message
    ) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Inventory Batches");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error message with exception details.
     *
     * The exception is printed for debugging and a readable error message is
     * displayed to the user.
     *
     * @param message   the main error message to show
     * @param exception the exception that caused the error
     */
    private void showError(
            String message,
            Exception exception
    ) {
        exception.printStackTrace();

        String details =
                exception.getMessage() == null
                        ? ""
                        : "\n\nDetails: " + exception.getMessage();

        showMessage(
                Alert.AlertType.ERROR,
                message + details
        );
    }
}