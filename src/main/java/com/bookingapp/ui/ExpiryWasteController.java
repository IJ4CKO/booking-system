package com.bookingapp.ui;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.bookingapp.dao.DBConnection;
import com.bookingapp.dao.InventoryBatchDAO;
import com.bookingapp.dao.InventoryDAO;
import com.bookingapp.dao.InventoryWasteDAO;
import com.bookingapp.dao.StockMovementDAO;
import com.bookingapp.dao.SupplierDAO;
import com.bookingapp.model.InventoryBatch;
import com.bookingapp.model.InventoryItem;
import com.bookingapp.model.InventoryWaste;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Controller for the expiry and waste management screen.
 *
 * This class manages the user interface for viewing expired or soon-to-expire
 * inventory batches and recording stock waste. It also supports searching,
 * displaying stock value at risk, recording waste transactions and updating
 * inventory, batch quantities and stock movement records.
 */
public class ExpiryWasteController {

    /**
     * Small tolerance value used when comparing decimal stock quantities.
     */
    private static final double QUANTITY_TOLERANCE = 0.000001;

    /**
     * Waste reason used when stock has expired.
     */
    private static final String REASON_EXPIRED = "Expired";

    /**
     * Waste reason used when stock has been damaged.
     */
    private static final String REASON_DAMAGED = "Damaged";

    /**
     * Waste reason used when stock has been lost through spillage.
     */
    private static final String REASON_SPILLAGE = "Spillage";

    /**
     * Waste reason used when stock has been lost due to a failed bake.
     */
    private static final String REASON_FAILED_BAKE = "Failed bake";

    /**
     * Waste reason used when a stocktake has found a discrepancy.
     */
    private static final String REASON_STOCKTAKE = "Stocktake discrepancy";

    /**
     * Waste reason used when none of the standard reasons apply.
     */
    private static final String REASON_OTHER = "Other";

    /**
     * Date format used when displaying dates in British format.
     */
    private static final DateTimeFormatter BRITISH_DATE_FORMAT
            = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Data access object used for inventory batch database operations.
     */
    private final InventoryBatchDAO inventoryBatchDAO
            = new InventoryBatchDAO();

    /**
     * Data access object used for inventory item database operations.
     */
    private final InventoryDAO inventoryDAO
            = new InventoryDAO();

    /**
     * Data access object used for inventory waste database operations.
     */
    private final InventoryWasteDAO inventoryWasteDAO
            = new InventoryWasteDAO();

    /**
     * Data access object used for stock movement database operations.
     */
    private final StockMovementDAO stockMovementDAO
            = new StockMovementDAO();

    /**
     * Data access object used for supplier database operations.
     */
    private final SupplierDAO supplierDAO
            = new SupplierDAO();

    /**
     * Stores inventory items by inventory item ID for quick lookup.
     */
    private final Map<Integer, InventoryItem> inventoryItemsById
            = new HashMap<>();

    /**
     * Stores suppliers by supplier ID for quick lookup.
     */
    private final Map<Integer, Supplier> suppliersById
            = new HashMap<>();

    /**
     * Stores inventory batches by batch ID for quick lookup.
     */
    private final Map<Integer, InventoryBatch> batchesById
            = new HashMap<>();

    /**
     * Master list of expired and soon-to-expire batches used by the expiry
     * table, search and sort functions.
     */
    private final ObservableList<InventoryBatch> masterExpiryBatches
            = FXCollections.observableArrayList();

    /**
     * Master list of waste records used by the waste history table, search and
     * sort functions.
     */
    private final ObservableList<InventoryWaste> masterWasteRecords
            = FXCollections.observableArrayList();

    /**
     * Filtered list used to search expiry batch records.
     */
    private FilteredList<InventoryBatch> filteredExpiryBatches;

    /**
     * Filtered list used to search waste history records.
     */
    private FilteredList<InventoryWaste> filteredWasteRecords;

    /**
     * Combo box used to select how far ahead expiry warnings should be shown.
     */
    @FXML
    private ComboBox<Integer> expiryPeriodComboBox;

    /**
     * Text field used to search the expiry batch table.
     */
    @FXML
    private TextField expirySearchField;

    /**
     * Table used to display expired and soon-to-expire inventory batches.
     */
    @FXML
    private TableView<InventoryBatch> expiryBatchTable;

    /**
     * Table column used to display the inventory batch ID.
     */
    @FXML
    private TableColumn<InventoryBatch, Integer> expiryBatchIdColumn;

    /**
     * Table column used to display the inventory item name.
     */
    @FXML
    private TableColumn<InventoryBatch, String> expiryItemColumn;

    /**
     * Table column used to display the supplier name.
     */
    @FXML
    private TableColumn<InventoryBatch, String> expirySupplierColumn;

    /**
     * Table column used to display the batch code.
     */
    @FXML
    private TableColumn<InventoryBatch, String> expiryBatchCodeColumn;

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
     * Table column used to display the quantity remaining in the batch.
     */
    @FXML
    private TableColumn<InventoryBatch, Double> expiryRemainingColumn;

    /**
     * Table column used to display the batch unit cost.
     */
    @FXML
    private TableColumn<InventoryBatch, Double> expiryUnitCostColumn;

    /**
     * Table column used to display the remaining value of the batch.
     */
    @FXML
    private TableColumn<InventoryBatch, Double> expiryValueColumn;

    /**
     * Table column used to display the expiry status of the batch.
     */
    @FXML
    private TableColumn<InventoryBatch, String> expiryStatusColumn;

    /**
     * Label used to display the number of expired batches.
     */
    @FXML
    private Label expiredCountLabel;

    /**
     * Label used to display the number of batches expiring soon.
     */
    @FXML
    private Label expiringSoonCountLabel;

    /**
     * Label used to display the total stock value at risk.
     */
    @FXML
    private Label stockValueAtRiskLabel;

    /**
     * Combo box used to select the batch that will have waste recorded.
     */
    @FXML
    private ComboBox<InventoryBatch> wasteBatchComboBox;

    /**
     * Date picker used to select the waste date.
     */
    @FXML
    private DatePicker wasteDatePicker;

    /**
     * Text field used to enter the quantity wasted.
     */
    @FXML
    private TextField wasteQuantityField;

    /**
     * Combo box used to select the reason for the waste.
     */
    @FXML
    private ComboBox<String> wasteReasonComboBox;

    /**
     * Text area used to enter optional waste notes.
     */
    @FXML
    private TextArea wasteNotesArea;

    /**
     * Label used to display the selected batch.
     */
    @FXML
    private Label selectedBatchLabel;

    /**
     * Label used to display the inventory item linked to the selected batch.
     */
    @FXML
    private Label selectedItemLabel;

    /**
     * Label used to display the expiry date of the selected batch.
     */
    @FXML
    private Label selectedExpiryLabel;

    /**
     * Label used to display the quantity remaining in the selected batch.
     */
    @FXML
    private Label selectedRemainingLabel;

    /**
     * Label used to display the unit cost of the selected batch.
     */
    @FXML
    private Label selectedUnitCostLabel;

    /**
     * Label used to display the estimated value of the waste being entered.
     */
    @FXML
    private Label wasteValuePreviewLabel;

    /**
     * Button used to record the waste transaction.
     */
    @FXML
    private Button recordWasteButton;

    /**
     * Text field used to search the waste history table.
     */
    @FXML
    private TextField wasteHistorySearchField;

    /**
     * Table used to display recorded waste history.
     */
    @FXML
    private TableView<InventoryWaste> wasteHistoryTable;

    /**
     * Table column used to display the waste record ID.
     */
    @FXML
    private TableColumn<InventoryWaste, Integer> wasteIdColumn;

    /**
     * Table column used to display the inventory item linked to the waste.
     */
    @FXML
    private TableColumn<InventoryWaste, String> wasteItemColumn;

    /**
     * Table column used to display the batch linked to the waste.
     */
    @FXML
    private TableColumn<InventoryWaste, String> wasteBatchColumn;

    /**
     * Table column used to display the waste date.
     */
    @FXML
    private TableColumn<InventoryWaste, String> wasteDateColumn;

    /**
     * Table column used to display the quantity wasted.
     */
    @FXML
    private TableColumn<InventoryWaste, Double> wasteQuantityColumn;

    /**
     * Table column used to display the reason for the waste.
     */
    @FXML
    private TableColumn<InventoryWaste, String> wasteReasonColumn;

    /**
     * Table column used to display the unit cost of the wasted stock.
     */
    @FXML
    private TableColumn<InventoryWaste, Double> wasteUnitCostColumn;

    /**
     * Table column used to display the total value of the waste.
     */
    @FXML
    private TableColumn<InventoryWaste, Double> wasteValueColumn;

    /**
     * Table column used to display any waste notes.
     */
    @FXML
    private TableColumn<InventoryWaste, String> wasteNotesColumn;

    /**
     * Label used to display the total recorded waste value.
     */
    @FXML
    private Label totalWasteValueLabel;

    /**
     * Initialises the expiry and waste management screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the date picker, combo boxes, tables, search
     * features, reference data, expiry information and waste history.
     */
    @FXML
    public void initialize() {
        configureDatePicker();
        configureExpiryPeriodComboBox();
        configureWasteReasonComboBox();
        configureWasteBatchComboBox();
        configureExpiryTable();
        configureWasteHistoryTable();
        configureSearch();

        wasteDatePicker.setValue(LocalDate.now());

        wasteQuantityField.textProperty().addListener(
                (observable, oldValue, newValue) -> updateWasteValuePreview()
        );

        loadReferenceData();
        loadWasteBatchChoices();
        loadExpiryBatches();
        loadWasteHistory();
        clearWasteForm();
    }

    /**
     * Shows the keyboard shortcuts available on the expiry and waste screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Expiry and Waste Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + A: Refresh all data\n"
                + "Alt + E: Refresh expiry information\n"
                + "Alt + W: Record waste\n"
                + "Alt + C: Clear waste form\n"
                + "Alt + H: Refresh waste history\n"
                + "Alt + B: Back to Dashboard"
        );

        alert.showAndWait();
    }

    /**
     * Configures the waste date picker to use British date formatting.
     *
     * Dates are displayed and entered using dd/MM/yyyy.
     */
    private void configureDatePicker() {
        wasteDatePicker.setConverter(new StringConverter<>() {

            /**
             * Converts a LocalDate into display text.
             *
             * @param date the date to display
             * @return the formatted date, or an empty string if no date is set
             */
            @Override
            public String toString(LocalDate date) {
                if (date == null) {
                    return "";
                }

                return date.format(BRITISH_DATE_FORMAT);
            }

            /**
             * Converts entered text into a LocalDate.
             *
             * @param text the date text entered by the user
             * @return the parsed date, or null if the text is blank
             */
            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }

                return LocalDate.parse(
                        text.trim(),
                        BRITISH_DATE_FORMAT
                );
            }
        });
    }

    /**
     * Configures the expiry period combo box.
     *
     * The user can choose how many days ahead should be checked for expiring
     * stock. Changing the value reloads the expiry table.
     */
    private void configureExpiryPeriodComboBox() {
        expiryPeriodComboBox.setItems(
                FXCollections.observableArrayList(7, 14, 30, 60, 90)
        );

        expiryPeriodComboBox.setConverter(new StringConverter<>() {

            /**
             * Converts the number of days into readable display text.
             *
             * @param days the selected number of days
             * @return the display text for the number of days
             */
            @Override
            public String toString(Integer days) {
                if (days == null) {
                    return "";
                }

                return days + " days";
            }

            /**
             * Converts text back into a value.
             *
             * The combo box values are selected from a fixed list, so direct
             * text conversion is not needed.
             *
             * @param text the text entered by the user
             * @return null because typed conversion is not used
             */
            @Override
            public Integer fromString(String text) {
                return null;
            }
        });

        expiryPeriodComboBox.setValue(30);

        expiryPeriodComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadExpiryBatches();
                    }
                }
        );
    }

    /**
     * Configures the waste reason combo box.
     *
     * The list contains the standard reasons used when recording wasted stock.
     */
    private void configureWasteReasonComboBox() {
        wasteReasonComboBox.setItems(
                FXCollections.observableArrayList(
                        REASON_EXPIRED,
                        REASON_DAMAGED,
                        REASON_SPILLAGE,
                        REASON_FAILED_BAKE,
                        REASON_STOCKTAKE,
                        REASON_OTHER
                )
        );
    }

    /**
     * Configures the waste batch combo box.
     *
     * The combo box displays readable batch choices and updates the selected
     * batch details and estimated waste value when the selected batch changes.
     */
    private void configureWasteBatchComboBox() {
        wasteBatchComboBox.setCellFactory(listView -> new ListCell<>() {

            /**
             * Updates how inventory batches are displayed in the drop-down
             * list.
             *
             * @param batch the inventory batch to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    InventoryBatch batch,
                    boolean empty
            ) {
                super.updateItem(batch, empty);

                if (empty || batch == null) {
                    setText(null);
                } else {
                    setText(formatBatchChoice(batch));
                }
            }
        });

        wasteBatchComboBox.setButtonCell(new ListCell<>() {

            /**
             * Updates how the selected inventory batch is displayed.
             *
             * @param batch the selected inventory batch
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    InventoryBatch batch,
                    boolean empty
            ) {
                super.updateItem(batch, empty);

                if (empty || batch == null) {
                    setText(wasteBatchComboBox.getPromptText());
                } else {
                    setText(formatBatchChoice(batch));
                }
            }
        });

        wasteBatchComboBox.setConverter(new StringConverter<>() {

            /**
             * Converts an inventory batch into display text.
             *
             * @param batch the inventory batch to display
             * @return a readable batch description
             */
            @Override
            public String toString(InventoryBatch batch) {
                if (batch == null) {
                    return "";
                }

                return formatBatchChoice(batch);
            }

            /**
             * Converts text into an inventory batch.
             *
             * Direct text conversion is not used because batches are selected
             * from existing records.
             *
             * @param text the entered text
             * @return null because typed conversion is not used
             */
            @Override
            public InventoryBatch fromString(String text) {
                return null;
            }
        });

        wasteBatchComboBox.valueProperty().addListener(
                (observable, oldBatch, newBatch) -> {
                    updateSelectedBatchDetails(newBatch);
                    updateWasteValuePreview();
                }
        );
    }

    /**
     * Formats an inventory batch for display in the waste batch combo box.
     *
     * The display text includes the inventory item name, batch code and
     * quantity remaining.
     *
     * @param batch the inventory batch to format
     * @return a readable batch choice
     */
    private String formatBatchChoice(InventoryBatch batch) {
        InventoryItem inventoryItem
                = inventoryItemsById.get(batch.getInventoryItemId());

        String itemName
                = inventoryItem == null
                        ? "Item ID " + batch.getInventoryItemId()
                        : inventoryItem.getName();

        String unit
                = inventoryItem == null
                        ? ""
                        : " " + inventoryItem.getUnit();

        return itemName
                + " — "
                + safeBatchCode(batch)
                + " — "
                + formatQuantity(batch.getQuantityRemaining())
                + unit
                + " remaining";
    }

    /**
     * Configures the expiry batch table.
     *
     * Each table column is linked to the correct inventory batch value. Numeric
     * columns are formatted for quantities and money values, and selecting a
     * batch also selects it in the waste form.
     */
    private void configureExpiryTable() {
        expiryBatchTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        expiryBatchIdColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getBatchId()
                )
        );

        expiryItemColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        getInventoryItemName(
                                cellData.getValue().getInventoryItemId()
                        )
                )
        );

        expirySupplierColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        getSupplierName(
                                cellData.getValue().getSupplierId()
                        )
                )
        );

        expiryBatchCodeColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        safeBatchCode(cellData.getValue())
                )
        );

        purchaseDateColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        formatStoredDate(
                                cellData.getValue().getPurchaseDate()
                        )
                )
        );

        expiryDateColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        formatStoredDate(
                                cellData.getValue().getExpiryDate()
                        )
                )
        );

        expiryRemainingColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getQuantityRemaining()
                )
        );

        expiryUnitCostColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getUnitCost()
                )
        );

        expiryValueColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getBatchValue()
                )
        );

        expiryStatusColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        getExpiryStatus(cellData.getValue())
                )
        );

        configureBatchNumberColumn(expiryRemainingColumn, false);
        configureBatchNumberColumn(expiryUnitCostColumn, true);
        configureBatchNumberColumn(expiryValueColumn, true);

        expiryBatchTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldBatch, newBatch) -> {
                    if (newBatch != null) {
                        selectWasteBatch(newBatch.getBatchId());
                    }
                });
    }

    /**
     * Configures how numeric inventory batch columns are displayed.
     *
     * The same method is used for quantity and currency columns.
     *
     * @param column the table column to configure
     * @param currency true if the value should be formatted as money
     */
    private void configureBatchNumberColumn(
            TableColumn<InventoryBatch, Double> column,
            boolean currency
    ) {
        column.setCellFactory(tableColumn -> new TableCell<>() {

            /**
             * Updates the displayed number for a batch table cell.
             *
             * @param value the number value to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                } else if (currency) {
                    setText(formatMoney(value));
                } else {
                    setText(formatQuantity(value));
                }
            }
        });
    }

    /**
     * Configures the waste history table.
     *
     * Each table column is linked to the correct waste record value. Numeric
     * columns are formatted for quantities and money values.
     */
    private void configureWasteHistoryTable() {
        wasteHistoryTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        wasteIdColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getWasteId()
                )
        );

        wasteItemColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        getInventoryItemName(
                                cellData.getValue().getInventoryItemId()
                        )
                )
        );

        wasteBatchColumn.setCellValueFactory(cellData -> {
            int batchId = cellData.getValue().getBatchId();
            InventoryBatch batch = batchesById.get(batchId);

            String batchCode
                    = batch == null
                            ? "Batch ID " + batchId
                            : safeBatchCode(batch);

            return new ReadOnlyStringWrapper(batchCode);
        });

        wasteDateColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        formatStoredDate(
                                cellData.getValue().getWasteDate()
                        )
                )
        );

        wasteQuantityColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getQuantityWasted()
                )
        );

        wasteReasonColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        safeText(cellData.getValue().getReason())
                )
        );

        wasteUnitCostColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getUnitCost()
                )
        );

        wasteValueColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getWasteValue()
                )
        );

        wasteNotesColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        safeText(cellData.getValue().getNotes())
                )
        );

        configureWasteNumberColumn(wasteQuantityColumn, false);
        configureWasteNumberColumn(wasteUnitCostColumn, true);
        configureWasteNumberColumn(wasteValueColumn, true);
    }

    /**
     * Configures how numeric waste history columns are displayed.
     *
     * The same method is used for quantity and currency columns.
     *
     * @param column the table column to configure
     * @param currency true if the value should be formatted as money
     */
    private void configureWasteNumberColumn(
            TableColumn<InventoryWaste, Double> column,
            boolean currency
    ) {
        column.setCellFactory(tableColumn -> new TableCell<>() {

            /**
             * Updates the displayed number for a waste history table cell.
             *
             * @param value the number value to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                } else if (currency) {
                    setText(formatMoney(value));
                } else {
                    setText(formatQuantity(value));
                }
            }
        });
    }

    /**
     * Configures searching and sorting for the expiry and waste history tables.
     *
     * The expiry search field filters expiry batches, while the waste history
     * search field filters recorded waste records.
     */
    private void configureSearch() {
        filteredExpiryBatches
                = new FilteredList<>(masterExpiryBatches, batch -> true);

        expirySearchField.textProperty().addListener(
                (observable, oldValue, newValue)
                -> applyExpirySearch(newValue)
        );

        SortedList<InventoryBatch> sortedExpiryBatches
                = new SortedList<>(filteredExpiryBatches);

        sortedExpiryBatches.comparatorProperty()
                .bind(expiryBatchTable.comparatorProperty());

        expiryBatchTable.setItems(sortedExpiryBatches);

        filteredWasteRecords
                = new FilteredList<>(masterWasteRecords, waste -> true);

        wasteHistorySearchField.textProperty().addListener(
                (observable, oldValue, newValue)
                -> applyWasteHistorySearch(newValue)
        );

        SortedList<InventoryWaste> sortedWasteRecords
                = new SortedList<>(filteredWasteRecords);

        sortedWasteRecords.comparatorProperty()
                .bind(wasteHistoryTable.comparatorProperty());

        wasteHistoryTable.setItems(sortedWasteRecords);
    }

    /**
     * Applies the expiry search text to the expiry batch table.
     *
     * @param searchText the search text entered by the user
     */
    private void applyExpirySearch(String searchText) {
        filteredExpiryBatches.setPredicate(batch
                -> matchesExpirySearch(batch, searchText)
        );
    }

    /**
     * Checks whether an inventory batch matches the expiry search text.
     *
     * The search checks batch ID, inventory item, supplier, batch code,
     * purchase date, expiry date, quantity, value and expiry status.
     *
     * @param batch the inventory batch to check
     * @param searchText the search text entered by the user
     * @return true if the batch matches the search text, otherwise false
     */
    private boolean matchesExpirySearch(
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
                        getInventoryItemName(batch.getInventoryItemId()),
                        searchValue
                )
                || containsSearchValue(
                        getSupplierName(batch.getSupplierId()),
                        searchValue
                )
                || containsSearchValue(
                        safeBatchCode(batch),
                        searchValue
                )
                || containsSearchValue(
                        batch.getPurchaseDate(),
                        searchValue
                )
                || containsSearchValue(
                        formatStoredDate(batch.getPurchaseDate()),
                        searchValue
                )
                || containsSearchValue(
                        batch.getExpiryDate(),
                        searchValue
                )
                || containsSearchValue(
                        formatStoredDate(batch.getExpiryDate()),
                        searchValue
                )
                || containsSearchValue(
                        formatQuantity(batch.getQuantityRemaining()),
                        searchValue
                )
                || containsSearchValue(
                        formatMoney(batch.getUnitCost()),
                        searchValue
                )
                || containsSearchValue(
                        String.format(Locale.UK, "%.2f", batch.getUnitCost()),
                        searchValue
                )
                || containsSearchValue(
                        formatMoney(batch.getBatchValue()),
                        searchValue
                )
                || containsSearchValue(
                        String.format(Locale.UK, "%.2f", batch.getBatchValue()),
                        searchValue
                )
                || containsSearchValue(
                        getExpiryStatus(batch),
                        searchValue
                );
    }

    /**
     * Applies the waste history search text to the waste history table.
     *
     * @param searchText the search text entered by the user
     */
    private void applyWasteHistorySearch(String searchText) {
        filteredWasteRecords.setPredicate(waste
                -> matchesWasteHistorySearch(waste, searchText)
        );
    }

    /**
     * Checks whether a waste record matches the waste history search text.
     *
     * The search checks waste ID, inventory item, batch, waste date, quantity,
     * reason, unit cost, waste value and notes.
     *
     * @param waste the waste record to check
     * @param searchText the search text entered by the user
     * @return true if the waste record matches the search text, otherwise false
     */
    private boolean matchesWasteHistorySearch(
            InventoryWaste waste,
            String searchText
    ) {
        if (waste == null) {
            return false;
        }

        String searchValue = safeText(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(waste.getWasteId()),
                searchValue
        )
                || containsSearchValue(
                        getInventoryItemName(waste.getInventoryItemId()),
                        searchValue
                )
                || containsSearchValue(
                        getBatchDisplayName(waste.getBatchId()),
                        searchValue
                )
                || containsSearchValue(
                        waste.getWasteDate(),
                        searchValue
                )
                || containsSearchValue(
                        formatStoredDate(waste.getWasteDate()),
                        searchValue
                )
                || containsSearchValue(
                        formatQuantity(waste.getQuantityWasted()),
                        searchValue
                )
                || containsSearchValue(
                        waste.getReason(),
                        searchValue
                )
                || containsSearchValue(
                        formatMoney(waste.getUnitCost()),
                        searchValue
                )
                || containsSearchValue(
                        String.format(Locale.UK, "%.2f", waste.getUnitCost()),
                        searchValue
                )
                || containsSearchValue(
                        formatMoney(waste.getWasteValue()),
                        searchValue
                )
                || containsSearchValue(
                        String.format(Locale.UK, "%.2f", waste.getWasteValue()),
                        searchValue
                )
                || containsSearchValue(
                        waste.getNotes(),
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
     * Loads inventory items, suppliers and inventory batches for lookup use.
     *
     * The data is stored in maps so that table columns and form labels can
     * display names instead of only database IDs.
     */
    private void loadReferenceData() {
        try {
            List<InventoryItem> inventoryItems
                    = inventoryDAO.getAllInventoryItems();

            List<Supplier> suppliers
                    = supplierDAO.getAllSuppliers();

            List<InventoryBatch> batches
                    = inventoryBatchDAO.getAllBatches();

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

            batchesById.clear();

            for (InventoryBatch batch : batches) {
                batchesById.put(
                        batch.getBatchId(),
                        batch
                );
            }

        } catch (Exception e) {
            showError(
                    "Unable to load the inventory reference data.",
                    e
            );
        }
    }

    /**
     * Loads available inventory batches into the waste batch combo box.
     *
     * Only batches with remaining stock are shown as selectable waste batches.
     * The batch lookup map is also refreshed.
     */
    private void loadWasteBatchChoices() {
        try {
            List<InventoryBatch> allBatches
                    = inventoryBatchDAO.getAllBatches();

            List<InventoryBatch> availableBatches
                    = allBatches.stream()
                            .filter(batch
                                    -> batch.getQuantityRemaining()
                            > QUANTITY_TOLERANCE
                            )
                            .toList();

            wasteBatchComboBox.setItems(
                    FXCollections.observableArrayList(availableBatches)
            );

            batchesById.clear();

            for (InventoryBatch batch : allBatches) {
                batchesById.put(
                        batch.getBatchId(),
                        batch
                );
            }

        } catch (Exception e) {
            showError(
                    "Unable to load the available inventory batches.",
                    e
            );
        }
    }

    /**
     * Loads expired and soon-to-expire inventory batches.
     *
     * The selected expiry period controls how far ahead the system checks for
     * expiring batches. The table and summary labels are refreshed after the
     * data has loaded.
     */
    @FXML
    private void loadExpiryBatches() {
        try {
            Integer selectedDays
                    = expiryPeriodComboBox.getValue();

            int daysAhead
                    = selectedDays == null
                            ? 30
                            : selectedDays;

            List<InventoryBatch> expiredBatches
                    = inventoryBatchDAO.getExpiredBatches();

            List<InventoryBatch> expiringSoonBatches
                    = inventoryBatchDAO.getBatchesExpiringSoon(daysAhead);

            Map<Integer, InventoryBatch> combinedBatches
                    = new LinkedHashMap<>();

            for (InventoryBatch batch : expiredBatches) {
                combinedBatches.put(
                        batch.getBatchId(),
                        batch
                );
            }

            for (InventoryBatch batch : expiringSoonBatches) {
                combinedBatches.put(
                        batch.getBatchId(),
                        batch
                );
            }

            List<InventoryBatch> displayBatches
                    = new ArrayList<>(combinedBatches.values());

            masterExpiryBatches.setAll(displayBatches);

            applyExpirySearch(
                    expirySearchField == null
                            ? ""
                            : expirySearchField.getText()
            );

            double valueAtRisk
                    = displayBatches.stream()
                            .mapToDouble(InventoryBatch::getBatchValue)
                            .sum();

            expiredCountLabel.setText(
                    "Expired batches: " + expiredBatches.size()
            );

            expiringSoonCountLabel.setText(
                    "Expiring within "
                    + daysAhead
                    + " days: "
                    + expiringSoonBatches.size()
            );

            stockValueAtRiskLabel.setText(
                    "Stock value at risk: "
                    + formatMoney(valueAtRisk)
            );

        } catch (Exception e) {
            showError(
                    "Unable to load the expiry information.",
                    e
            );
        }
    }

    /**
     * Gets the expiry status for an inventory batch.
     *
     * The status shows whether the batch is expired, expires today, expires
     * tomorrow, expires in a number of days or has no expiry date.
     *
     * @param batch the inventory batch to check
     * @return a readable expiry status
     */
    private String getExpiryStatus(InventoryBatch batch) {
        LocalDate expiryDate
                = parseStoredDate(batch.getExpiryDate());

        if (expiryDate == null) {
            return "NO EXPIRY DATE";
        }

        long daysUntilExpiry
                = ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        expiryDate
                );

        if (daysUntilExpiry < 0) {
            return "EXPIRED";
        }

        if (daysUntilExpiry == 0) {
            return "EXPIRES TODAY";
        }

        if (daysUntilExpiry == 1) {
            return "EXPIRES TOMORROW";
        }

        return "EXPIRES IN " + daysUntilExpiry + " DAYS";
    }

    /**
     * Selects a batch in the waste batch combo box using its batch ID.
     *
     * This is used when the user selects a row in the expiry batch table.
     *
     * @param batchId the batch ID to select
     */
    private void selectWasteBatch(int batchId) {
        for (InventoryBatch batch : wasteBatchComboBox.getItems()) {
            if (batch.getBatchId() == batchId) {
                wasteBatchComboBox.setValue(batch);
                return;
            }
        }
    }

    /**
     * Updates the selected batch labels in the waste form.
     *
     * If no batch is selected, the form labels are reset and the record waste
     * button is disabled. If a batch is selected, the batch, item, expiry,
     * remaining quantity and unit cost details are displayed.
     *
     * @param batch the selected inventory batch, or null if none is selected
     */
    private void updateSelectedBatchDetails(InventoryBatch batch) {
        if (batch == null) {
            selectedBatchLabel.setText("Selected batch: None");
            selectedItemLabel.setText("Inventory item: None");
            selectedExpiryLabel.setText("Expiry date: Not selected");
            selectedRemainingLabel.setText("Quantity remaining: 0");
            selectedUnitCostLabel.setText("Unit cost: £0.00");
            recordWasteButton.setDisable(true);
            return;
        }

        InventoryItem inventoryItem
                = inventoryItemsById.get(batch.getInventoryItemId());

        String itemName
                = inventoryItem == null
                        ? "Item ID " + batch.getInventoryItemId()
                        : inventoryItem.getName();

        String unit
                = inventoryItem == null
                        ? ""
                        : " " + inventoryItem.getUnit();

        selectedBatchLabel.setText(
                "Selected batch: " + safeBatchCode(batch)
        );

        selectedItemLabel.setText(
                "Inventory item: " + itemName
        );

        selectedExpiryLabel.setText(
                "Expiry date: " + formatStoredDate(batch.getExpiryDate())
        );

        selectedRemainingLabel.setText(
                "Quantity remaining: "
                + formatQuantity(batch.getQuantityRemaining())
                + unit
        );

        selectedUnitCostLabel.setText(
                "Unit cost: " + formatMoney(batch.getUnitCost())
        );

        recordWasteButton.setDisable(false);
    }

    /**
     * Updates the estimated waste value preview.
     *
     * The preview is calculated by multiplying the entered waste quantity by
     * the selected batch unit cost.
     */
    private void updateWasteValuePreview() {
        InventoryBatch selectedBatch
                = wasteBatchComboBox.getValue();

        if (selectedBatch == null) {
            wasteValuePreviewLabel.setText(
                    "Estimated waste value: £0.00"
            );
            return;
        }

        String quantityText
                = wasteQuantityField.getText();

        if (quantityText == null || quantityText.isBlank()) {
            wasteValuePreviewLabel.setText(
                    "Estimated waste value: £0.00"
            );
            return;
        }

        try {
            double quantity
                    = Double.parseDouble(quantityText.trim());

            double wasteValue
                    = quantity > 0
                            ? quantity * selectedBatch.getUnitCost()
                            : 0.0;

            wasteValuePreviewLabel.setText(
                    "Estimated waste value: "
                    + formatMoney(wasteValue)
            );

        } catch (NumberFormatException e) {
            wasteValuePreviewLabel.setText(
                    "Estimated waste value: £0.00"
            );
        }
    }

    /**
     * Records waste for the selected inventory batch.
     *
     * The method validates the selected batch, waste date, reason and wasted
     * quantity. If validation passes, it records the waste, reduces the batch
     * quantity, reduces the inventory total and adds a stock movement record in
     * one database transaction.
     */
    @FXML
    private void recordWaste() {
        InventoryBatch selectedBatch
                = wasteBatchComboBox.getValue();

        if (selectedBatch == null) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select an inventory batch."
            );
            return;
        }

        LocalDate wasteDate
                = wasteDatePicker.getValue();

        if (wasteDate == null) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select a waste date."
            );
            return;
        }

        if (wasteDate.isAfter(LocalDate.now())) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "The waste date cannot be in the future."
            );
            return;
        }

        String reason
                = wasteReasonComboBox.getValue();

        if (reason == null || reason.isBlank()) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select a reason for the waste."
            );
            return;
        }

        double quantityWasted;

        try {
            quantityWasted
                    = parsePositiveDouble(
                            wasteQuantityField.getText(),
                            "Quantity wasted"
                    );

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.WARNING,
                    e.getMessage()
            );
            return;
        }

        int batchId
                = selectedBatch.getBatchId();

        try (Connection conn = DBConnection.getConnection()) {
            boolean originalAutoCommit
                    = conn.getAutoCommit();

            conn.setAutoCommit(false);

            try {
                InventoryBatch currentBatch
                        = inventoryBatchDAO.getBatchById(
                                conn,
                                batchId
                        );

                if (currentBatch == null) {
                    throw new IllegalStateException(
                            "The selected inventory batch could not be found."
                    );
                }

                if (currentBatch.getQuantityRemaining()
                        <= QUANTITY_TOLERANCE) {

                    throw new IllegalStateException(
                            "The selected inventory batch has no stock remaining."
                    );
                }

                if (quantityWasted
                        > currentBatch.getQuantityRemaining()
                        + QUANTITY_TOLERANCE) {

                    throw new IllegalArgumentException(
                            "The quantity wasted cannot be greater than "
                            + "the quantity remaining in the batch."
                    );
                }

                validateExpiredReason(
                        currentBatch,
                        wasteDate,
                        reason
                );

                double newQuantityRemaining
                        = currentBatch.getQuantityRemaining()
                        - quantityWasted;

                if (Math.abs(newQuantityRemaining)
                        < QUANTITY_TOLERANCE) {

                    newQuantityRemaining = 0.0;
                }

                double wasteValue
                        = quantityWasted * currentBatch.getUnitCost();

                InventoryWaste wasteRecord
                        = new InventoryWaste(
                                0,
                                currentBatch.getBatchId(),
                                currentBatch.getInventoryItemId(),
                                wasteDate.toString(),
                                quantityWasted,
                                reason,
                                currentBatch.getUnitCost(),
                                wasteValue,
                                blankToNull(wasteNotesArea.getText())
                        );

                inventoryWasteDAO.addWasteRecord(
                        conn,
                        wasteRecord
                );

                inventoryBatchDAO.updateBatchQuantityRemaining(
                        conn,
                        currentBatch.getBatchId(),
                        newQuantityRemaining
                );

                inventoryDAO.updateInventoryQuantity(
                        conn,
                        currentBatch.getInventoryItemId(),
                        -quantityWasted
                );

                String movementType
                        = REASON_EXPIRED.equals(reason)
                        ? "EXPIRED"
                        : "WASTE";

                stockMovementDAO.addStockMovement(
                        conn,
                        currentBatch.getInventoryItemId(),
                        -quantityWasted,
                        null,
                        movementType,
                        buildStockMovementNotes(
                                currentBatch,
                                wasteDate,
                                reason,
                                wasteRecord.getNotes()
                        )
                );

                conn.commit();

            } catch (Exception e) {
                rollbackTransaction(conn, e);
                throw e;

            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Waste recorded successfully."
            );

            refreshAllData();
            clearWasteForm();

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.WARNING,
                    e.getMessage()
            );

        } catch (Exception e) {
            showError(
                    "Unable to record the waste.",
                    e
            );
        }
    }

    /**
     * Validates whether the expired reason can be used for the selected batch.
     *
     * If the reason is Expired, the batch must have an expiry date and that
     * expiry date must not be after the selected waste date.
     *
     * @param batch the selected inventory batch
     * @param wasteDate the selected waste date
     * @param reason the selected waste reason
     */
    private void validateExpiredReason(
            InventoryBatch batch,
            LocalDate wasteDate,
            String reason
    ) {
        if (!REASON_EXPIRED.equals(reason)) {
            return;
        }

        LocalDate expiryDate
                = parseStoredDate(batch.getExpiryDate());

        if (expiryDate == null) {
            throw new IllegalArgumentException(
                    "This batch does not have an expiry date. "
                    + "Select another waste reason."
            );
        }

        if (expiryDate.isAfter(wasteDate)) {
            throw new IllegalArgumentException(
                    "This batch had not expired on the selected waste date. "
                    + "Select another reason or correct the waste date."
            );
        }
    }

    /**
     * Builds notes for the stock movement created when waste is recorded.
     *
     * The notes include the batch code, waste reason, waste date and any
     * optional notes entered by the user.
     *
     * @param batch the batch linked to the waste
     * @param wasteDate the date the waste was recorded
     * @param reason the reason for the waste
     * @param notes optional notes entered by the user
     * @return readable stock movement notes
     */
    private String buildStockMovementNotes(
            InventoryBatch batch,
            LocalDate wasteDate,
            String reason,
            String notes
    ) {
        StringBuilder message
                = new StringBuilder();

        message.append("Waste recorded. Batch: ")
                .append(safeBatchCode(batch))
                .append(". Reason: ")
                .append(reason)
                .append(". Waste date: ")
                .append(wasteDate.format(BRITISH_DATE_FORMAT));

        if (notes != null && !notes.isBlank()) {
            message.append(". Notes: ")
                    .append(notes);
        }

        return message.toString();
    }

    /**
     * Loads all recorded waste history.
     *
     * The waste history table is refreshed, the current search value is
     * reapplied and the total recorded waste value label is updated.
     */
    @FXML
    private void loadWasteHistory() {
        try {
            List<InventoryWaste> wasteRecords
                    = inventoryWasteDAO.getAllWasteRecords();

            masterWasteRecords.setAll(wasteRecords);

            applyWasteHistorySearch(
                    wasteHistorySearchField == null
                            ? ""
                            : wasteHistorySearchField.getText()
            );

            double totalWasteValue
                    = inventoryWasteDAO.getTotalWasteValue();

            totalWasteValueLabel.setText(
                    "Total recorded waste value: "
                    + formatMoney(totalWasteValue)
            );

        } catch (Exception e) {
            showError(
                    "Unable to load the waste history.",
                    e
            );
        }
    }

    /**
     * Refreshes all data shown on the expiry and waste screen.
     *
     * This reloads reference data, available batch choices, expiry information
     * and waste history.
     */
    @FXML
    private void refreshAllData() {
        loadReferenceData();
        loadWasteBatchChoices();
        loadExpiryBatches();
        loadWasteHistory();
    }

    /**
     * Clears the waste form.
     *
     * This clears the selected batch, resets the waste date to today's date,
     * clears the quantity, reason and notes fields, and resets the selected
     * batch details.
     */
    @FXML
    private void clearWasteForm() {
        expiryBatchTable.getSelectionModel().clearSelection();
        wasteBatchComboBox.setValue(null);
        wasteDatePicker.setValue(LocalDate.now());
        wasteQuantityField.clear();
        wasteReasonComboBox.setValue(null);
        wasteNotesArea.clear();
        updateSelectedBatchDetails(null);
        updateWasteValuePreview();
    }

    /**
     * Parses and validates a positive decimal number.
     *
     * This is used for numeric form fields such as quantity wasted.
     *
     * @param text the text entered by the user
     * @param fieldName the field name used in validation messages
     * @return the parsed positive number
     * @throws IllegalArgumentException if the value is blank, invalid or not
     * greater than zero
     */
    private double parsePositiveDouble(
            String text,
            String fieldName
    ) {
        if (text == null || text.trim().isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        try {
            double value
                    = Double.parseDouble(text.trim());

            if (value <= QUANTITY_TOLERANCE) {
                throw new IllegalArgumentException(
                        fieldName + " must be greater than zero."
                );
            }

            return value;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + " must be a valid number."
            );
        }
    }

    /**
     * Parses a stored database date.
     *
     * Stored dates are expected to use the default ISO format yyyy-MM-dd. If
     * the value is blank or invalid, null is returned.
     *
     * @param dateText the stored date text
     * @return the parsed date, or null if the date is missing or invalid
     */
    private LocalDate parseStoredDate(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(dateText);

        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Formats a stored database date for display.
     *
     * Valid dates are displayed in British format. If the value is blank, a
     * message is returned. If the value cannot be parsed, the original value is
     * returned.
     *
     * @param dateText the stored date text
     * @return the formatted date text
     */
    private String formatStoredDate(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return "No expiry date";
        }

        LocalDate date
                = parseStoredDate(dateText);

        if (date == null) {
            return dateText;
        }

        return date.format(BRITISH_DATE_FORMAT);
    }

    /**
     * Gets an inventory item name from its ID.
     *
     * If the item cannot be found in the lookup map, the item ID is returned
     * instead.
     *
     * @param inventoryItemId the inventory item ID
     * @return the inventory item name or fallback item ID text
     */
    private String getInventoryItemName(int inventoryItemId) {
        InventoryItem inventoryItem
                = inventoryItemsById.get(inventoryItemId);

        if (inventoryItem == null) {
            return "Item ID " + inventoryItemId;
        }

        return inventoryItem.getName();
    }

    /**
     * Gets a supplier name from its ID.
     *
     * If no supplier is linked, "No supplier" is returned. If the supplier ID
     * cannot be found in the lookup map, the supplier ID is returned instead.
     *
     * @param supplierId the supplier ID, or null if no supplier is linked
     * @return the supplier name or fallback supplier text
     */
    private String getSupplierName(Integer supplierId) {
        if (supplierId == null) {
            return "No supplier";
        }

        Supplier supplier
                = suppliersById.get(supplierId);

        if (supplier == null) {
            return "Supplier ID " + supplierId;
        }

        return supplier.getSupplierName();
    }

    /**
     * Gets a readable batch display name from its ID.
     *
     * If the batch cannot be found in the lookup map, the batch ID is returned
     * instead.
     *
     * @param batchId the batch ID
     * @return the batch code or fallback batch ID text
     */
    private String getBatchDisplayName(int batchId) {
        InventoryBatch batch
                = batchesById.get(batchId);

        if (batch == null) {
            return "Batch ID " + batchId;
        }

        return safeBatchCode(batch);
    }

    /**
     * Safely gets a readable batch code.
     *
     * If the batch code is missing, the batch ID is used instead.
     *
     * @param batch the inventory batch to read
     * @return the batch code or fallback batch ID text
     */
    private String safeBatchCode(InventoryBatch batch) {
        if (batch == null) {
            return "";
        }

        if (batch.getBatchCode() == null
                || batch.getBatchCode().isBlank()) {

            return "Batch " + batch.getBatchId();
        }

        return batch.getBatchCode();
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
        if (Math.abs(quantity - Math.rint(quantity))
                < QUANTITY_TOLERANCE) {

            return String.format(Locale.UK, "%.0f", quantity);
        }

        return String.format(Locale.UK, "%.2f", quantity);
    }

    /**
     * Formats a money value for display.
     *
     * @param amount the money amount to format
     * @return the formatted money value
     */
    private String formatMoney(double amount) {
        return String.format(Locale.UK, "£%.2f", amount);
    }

    /**
     * Safely trims text.
     *
     * Null values are converted into an empty string so that search, formatting
     * and display logic can handle text safely.
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
     * This is used so optional notes are stored as null when the user leaves
     * the field empty.
     *
     * @param value the text value to check
     * @return null if the value is blank, otherwise the trimmed value
     */
    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Rolls a database transaction back without hiding the original exception.
     *
     * If the rollback fails, the rollback exception is added to the original
     * exception as a suppressed exception.
     *
     * @param conn the active database connection
     * @param originalException the original exception that caused the rollback
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
            FXMLLoader loader
                    = new FXMLLoader(
                            getClass().getResource(
                                    "/com/bookingapp/ui/dashboard.fxml"
                            )
                    );

            Parent root
                    = loader.load();

            Stage stage
                    = (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert
                    = new Alert(Alert.AlertType.ERROR);

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
     * Shows a message dialog to the user.
     *
     * This is used for validation errors, warnings, successful actions and
     * general messages on the expiry and waste management screen.
     *
     * @param alertType the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType alertType,
            String message
    ) {
        Alert alert
                = new Alert(alertType);

        alert.setTitle("Expiry and Waste Management");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error message with exception details.
     *
     * This method prints the exception for debugging and then shows a readable
     * message to the user.
     *
     * @param message the main error message to show
     * @param exception the exception that caused the error
     */
    private void showError(
            String message,
            Exception exception
    ) {
        exception.printStackTrace();

        String details
                = exception.getMessage() == null
                ? ""
                : "\n\nDetails: " + exception.getMessage();

        showMessage(
                Alert.AlertType.ERROR,
                message + details
        );
    }
}
