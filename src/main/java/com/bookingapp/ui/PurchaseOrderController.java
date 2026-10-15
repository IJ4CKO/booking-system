package com.bookingapp.ui;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.bookingapp.dao.DBConnection;
import com.bookingapp.dao.InventoryBatchDAO;
import com.bookingapp.dao.InventoryDAO;
import com.bookingapp.dao.PurchaseOrderDAO;
import com.bookingapp.dao.StockMovementDAO;
import com.bookingapp.dao.SupplierDAO;
import com.bookingapp.model.InventoryBatch;
import com.bookingapp.model.InventoryItem;
import com.bookingapp.model.PurchaseOrder;
import com.bookingapp.model.PurchaseOrderLine;
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
import javafx.scene.control.ButtonType;
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
 * Controller for the purchase orders screen.
 *
 * This class manages the staff-facing purchase order interface. It allows the
 * user to create draft purchase orders, add inventory items to an order, mark
 * an order as ordered, receive an order into stock, cancel an order, delete
 * draft or cancelled orders and return to the dashboard.
 */
public class PurchaseOrderController {

    /**
     * Status used when a purchase order is still editable.
     */
    private static final String STATUS_DRAFT = "DRAFT";

    /**
     * Status used when a purchase order has been sent to the supplier.
     */
    private static final String STATUS_ORDERED = "ORDERED";

    /**
     * Status used when a purchase order has been received into stock.
     */
    private static final String STATUS_RECEIVED = "RECEIVED";

    /**
     * Status used when a purchase order has been cancelled.
     */
    private static final String STATUS_CANCELLED = "CANCELLED";

    /**
     * Small tolerance value used when comparing decimal stock quantities.
     */
    private static final double QUANTITY_TOLERANCE = 0.000001;

    /**
     * Data access object used for purchase order database operations.
     */
    private final PurchaseOrderDAO purchaseOrderDAO
            = new PurchaseOrderDAO();

    /**
     * Data access object used for supplier database operations.
     */
    private final SupplierDAO supplierDAO
            = new SupplierDAO();

    /**
     * Data access object used for inventory item database operations.
     */
    private final InventoryDAO inventoryDAO
            = new InventoryDAO();

    /**
     * Data access object used for inventory batch database operations.
     */
    private final InventoryBatchDAO inventoryBatchDAO
            = new InventoryBatchDAO();

    /**
     * Data access object used for stock movement database operations.
     */
    private final StockMovementDAO stockMovementDAO
            = new StockMovementDAO();

    /**
     * Stores suppliers by supplier ID for quick lookup.
     */
    private final Map<Integer, Supplier> suppliersById
            = new HashMap<>();

    /**
     * Stores inventory items by inventory item ID for quick lookup.
     */
    private final Map<Integer, InventoryItem> inventoryItemsById
            = new HashMap<>();

    /**
     * Master list of purchase orders used by the purchase order table, search
     * and sort functions.
     */
    private final ObservableList<PurchaseOrder> masterPurchaseOrders
            = FXCollections.observableArrayList();

    /**
     * List of purchase order lines currently being viewed or edited.
     */
    private final ObservableList<PurchaseOrderLine> currentOrderLines
            = FXCollections.observableArrayList();

    /**
     * Currently selected or active purchase order.
     */
    private PurchaseOrder currentPurchaseOrder;

    /**
     * Combo box used to select the supplier for the purchase order.
     */
    @FXML
    private ComboBox<Supplier> supplierComboBox;

    /**
     * Date picker used to select the purchase order date.
     */
    @FXML
    private DatePicker orderDatePicker;

    /**
     * Date picker used to select the expected delivery date, if known.
     */
    @FXML
    private DatePicker expectedDeliveryDatePicker;

    /**
     * Text area used to enter optional purchase order notes.
     */
    @FXML
    private TextArea notesArea;

    /**
     * Label used to display the current purchase order reference.
     */
    @FXML
    private Label currentOrderLabel;

    /**
     * Label used to display the current purchase order status.
     */
    @FXML
    private Label orderStatusLabel;

    /**
     * Label used to display the current purchase order total.
     */
    @FXML
    private Label orderTotalLabel;

    /**
     * Combo box used to select an inventory item for a purchase order line.
     */
    @FXML
    private ComboBox<InventoryItem> inventoryItemComboBox;

    /**
     * Text field used to enter the quantity ordered for a line item.
     */
    @FXML
    private TextField quantityOrderedField;

    /**
     * Text field used to enter the unit cost for a line item.
     */
    @FXML
    private TextField unitCostField;

    /**
     * Text field used to search the purchase order table.
     */
    @FXML
    private TextField searchField;

    /**
     * Table used to display purchase orders.
     */
    @FXML
    private TableView<PurchaseOrder> purchaseOrderTable;

    /**
     * Table column used to display the purchase order ID.
     */
    @FXML
    private TableColumn<PurchaseOrder, Integer> orderIdColumn;

    /**
     * Table column used to display the supplier linked to the purchase order.
     */
    @FXML
    private TableColumn<PurchaseOrder, String> orderSupplierColumn;

    /**
     * Table column used to display the order date.
     */
    @FXML
    private TableColumn<PurchaseOrder, String> orderDateColumn;

    /**
     * Table column used to display the expected delivery date.
     */
    @FXML
    private TableColumn<PurchaseOrder, String> expectedDeliveryColumn;

    /**
     * Table column used to display the purchase order status.
     */
    @FXML
    private TableColumn<PurchaseOrder, String> statusColumn;

    /**
     * Table column used to display the purchase order total cost.
     */
    @FXML
    private TableColumn<PurchaseOrder, Double> totalCostColumn;

    /**
     * Table used to display the line items for the current purchase order.
     */
    @FXML
    private TableView<PurchaseOrderLine> orderLineTable;

    /**
     * Table column used to display the inventory item linked to a line.
     */
    @FXML
    private TableColumn<PurchaseOrderLine, String> lineInventoryItemColumn;

    /**
     * Table column used to display the quantity ordered for a line.
     */
    @FXML
    private TableColumn<PurchaseOrderLine, Double> lineQuantityColumn;

    /**
     * Table column used to display the unit cost for a line.
     */
    @FXML
    private TableColumn<PurchaseOrderLine, Double> lineUnitCostColumn;

    /**
     * Table column used to display the total cost for a line.
     */
    @FXML
    private TableColumn<PurchaseOrderLine, Double> lineTotalColumn;

    /**
     * Button used to add a line item to the current purchase order.
     */
    @FXML
    private Button addLineButton;

    /**
     * Button used to update the selected purchase order line.
     */
    @FXML
    private Button updateLineButton;

    /**
     * Button used to remove the selected purchase order line.
     */
    @FXML
    private Button removeLineButton;

    /**
     * Button used to save the current purchase order.
     */
    @FXML
    private Button saveOrderButton;

    /**
     * Button used to mark a draft purchase order as ordered.
     */
    @FXML
    private Button markOrderedButton;

    /**
     * Button used to receive an ordered purchase order into stock.
     */
    @FXML
    private Button receiveOrderButton;

    /**
     * Button used to cancel the current purchase order.
     */
    @FXML
    private Button cancelOrderButton;

    /**
     * Button used to delete a draft or cancelled purchase order.
     */
    @FXML
    private Button deleteOrderButton;

    /**
     * Initialises the purchase orders screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the combo boxes, purchase order table, search box,
     * order line table, default dates, reference data and initial form state.
     */
    @FXML
    public void initialize() {
        configureSupplierComboBox();
        configureInventoryItemComboBox();
        configurePurchaseOrderTable();
        configurePurchaseOrderSearch();
        configureOrderLineTable();

        orderLineTable.setItems(currentOrderLines);
        orderDatePicker.setValue(LocalDate.now());

        loadReferenceData();
        loadPurchaseOrders();
        clearOrderForm();
    }

    /**
     * Configures how suppliers are displayed in the supplier combo box.
     *
     * Suppliers are shown using their supplier name. Direct text conversion is
     * not used because suppliers are selected from existing database records.
     */
    private void configureSupplierComboBox() {
        supplierComboBox.setCellFactory(listView -> new ListCell<>() {

            /**
             * Updates how suppliers are displayed in the drop-down list.
             *
             * @param supplier the supplier to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    Supplier supplier,
                    boolean empty
            ) {
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
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    Supplier supplier,
                    boolean empty
            ) {
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
             * @return the supplier name, or an empty string if no supplier is
             * set
             */
            @Override
            public String toString(Supplier supplier) {
                if (supplier == null) {
                    return "";
                }

                return supplier.getSupplierName();
            }

            /**
             * Converts text into a supplier.
             *
             * This is not used because suppliers are selected from existing
             * records.
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
     * Configures how inventory items are displayed in the inventory item combo
     * box.
     *
     * Inventory items are shown with their purchase and recipe units so the
     * user can understand how ordered stock will be converted when received.
     */
    private void configureInventoryItemComboBox() {
        inventoryItemComboBox.setCellFactory(listView -> new ListCell<>() {

            /**
             * Updates how inventory items are displayed in the drop-down list.
             *
             * @param item the inventory item to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    InventoryItem item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatInventoryItem(item));
                }
            }
        });

        inventoryItemComboBox.setButtonCell(new ListCell<>() {

            /**
             * Updates how the selected inventory item is displayed.
             *
             * @param item the selected inventory item
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    InventoryItem item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(inventoryItemComboBox.getPromptText());
                } else {
                    setText(formatInventoryItem(item));
                }
            }
        });

        inventoryItemComboBox.setConverter(new StringConverter<>() {

            /**
             * Converts an inventory item into display text.
             *
             * @param item the inventory item to display
             * @return a readable inventory item description
             */
            @Override
            public String toString(InventoryItem item) {
                if (item == null) {
                    return "";
                }

                return formatInventoryItem(item);
            }

            /**
             * Converts text into an inventory item.
             *
             * This is not used because inventory items are selected from
             * existing database records.
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
     * Formats an inventory item for display in the user interface.
     *
     * The display text includes the purchase unit and recipe stock unit so that
     * the user can understand how stock is bought and stored.
     *
     * @param item the inventory item to format
     * @return a readable inventory item description
     */
    private String formatInventoryItem(InventoryItem item) {
        return item.getName()
                + " (purchase: "
                + item.getPurchaseUnit()
                + ", recipe stock: "
                + item.getRecipeUnit()
                + ")";
    }

    /**
     * Configures the purchase order table.
     *
     * Each column is linked to the correct purchase order value. Money values
     * are formatted as pounds, and selecting a row loads the full purchase
     * order and its line items into the form.
     */
    private void configurePurchaseOrderTable() {
        purchaseOrderTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        orderIdColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getPurchaseOrderId()
                )
        );

        orderSupplierColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        getSupplierDisplayName(cellData.getValue())
                )
        );

        orderDateColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        safeText(cellData.getValue().getOrderDate())
                )
        );

        expectedDeliveryColumn.setCellValueFactory(cellData -> {
            String expectedDate
                    = cellData.getValue().getExpectedDeliveryDate();

            if (expectedDate == null || expectedDate.isBlank()) {
                expectedDate = "Not specified";
            }

            return new ReadOnlyStringWrapper(expectedDate);
        });

        statusColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(
                        safeText(cellData.getValue().getStatus())
                )
        );

        totalCostColumn.setCellValueFactory(cellData -> {
            Double totalCost
                    = cellData.getValue().getTotalCost();

            return new ReadOnlyObjectWrapper<>(
                    totalCost == null ? 0.0 : totalCost
            );
        });

        totalCostColumn.setCellFactory(column -> new TableCell<>() {

            /**
             * Updates how the total cost is displayed in the purchase order
             * table.
             *
             * @param value the money value to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    Double value,
                    boolean empty
            ) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(formatMoney(value));
                }
            }
        });

        purchaseOrderTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldOrder, newOrder) -> {
                    if (newOrder != null) {
                        loadSelectedPurchaseOrder(newOrder);
                    }
                });
    }

    /**
     * Configures searching and sorting for the purchase order table.
     *
     * The search field filters the master purchase order list and the sorted
     * list keeps table column sorting available.
     */
    private void configurePurchaseOrderSearch() {
        FilteredList<PurchaseOrder> filteredPurchaseOrders
                = new FilteredList<>(
                        masterPurchaseOrders,
                        purchaseOrder -> true
                );

        searchField.textProperty().addListener(
                (observable, oldValue, newValue)
                -> filteredPurchaseOrders.setPredicate(purchaseOrder
                        -> matchesPurchaseOrderSearch(
                        purchaseOrder,
                        newValue
                )
                )
        );

        SortedList<PurchaseOrder> sortedPurchaseOrders
                = new SortedList<>(filteredPurchaseOrders);

        sortedPurchaseOrders.comparatorProperty()
                .bind(purchaseOrderTable.comparatorProperty());

        purchaseOrderTable.setItems(sortedPurchaseOrders);
    }

    /**
     * Checks whether a purchase order matches the search text.
     *
     * The search checks the purchase order ID, supplier, dates, status, total
     * cost and notes.
     *
     * @param purchaseOrder the purchase order to check
     * @param searchText the search text entered by the user
     * @return true if the purchase order matches the search text, otherwise
     * false
     */
    private boolean matchesPurchaseOrderSearch(
            PurchaseOrder purchaseOrder,
            String searchText
    ) {
        if (purchaseOrder == null) {
            return false;
        }

        String searchValue
                = safeText(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(purchaseOrder.getPurchaseOrderId()),
                searchValue
        )
                || containsSearchValue(
                        getSupplierDisplayName(purchaseOrder),
                        searchValue
                )
                || containsSearchValue(
                        purchaseOrder.getOrderDate(),
                        searchValue
                )
                || containsSearchValue(
                        purchaseOrder.getExpectedDeliveryDate(),
                        searchValue
                )
                || containsSearchValue(
                        purchaseOrder.getExpectedDeliveryDate() == null
                        || purchaseOrder.getExpectedDeliveryDate().isBlank()
                        ? "not specified"
                        : "",
                        searchValue
                )
                || containsSearchValue(
                        purchaseOrder.getStatus(),
                        searchValue
                )
                || containsSearchValue(
                        formatMoney(
                                purchaseOrder.getTotalCost() == null
                                ? 0.0
                                : purchaseOrder.getTotalCost()
                        ),
                        searchValue
                )
                || containsSearchValue(
                        String.format(
                                Locale.UK,
                                "%.2f",
                                purchaseOrder.getTotalCost() == null
                                ? 0.0
                                : purchaseOrder.getTotalCost()
                        ),
                        searchValue
                )
                || containsSearchValue(
                        purchaseOrder.getNotes(),
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
     * Configures the purchase order line table.
     *
     * Each column is linked to the correct purchase order line value. Quantity
     * and money columns are formatted, and selecting a line loads it into the
     * line form for editing.
     */
    private void configureOrderLineTable() {
        orderLineTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        lineInventoryItemColumn.setCellValueFactory(cellData -> {
            int inventoryItemId
                    = cellData.getValue().getInventoryItemId();

            InventoryItem item
                    = inventoryItemsById.get(inventoryItemId);

            String itemName;

            if (item == null) {
                itemName = "Inventory item ID " + inventoryItemId;
            } else {
                itemName = formatInventoryItem(item);
            }

            return new ReadOnlyStringWrapper(itemName);
        });

        lineQuantityColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getQuantityOrdered()
                )
        );

        lineUnitCostColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getUnitCost()
                )
        );

        lineTotalColumn.setCellValueFactory(cellData
                -> new ReadOnlyObjectWrapper<>(
                        cellData.getValue().getLineTotal()
                )
        );

        configureNumberColumn(lineQuantityColumn, false);
        configureNumberColumn(lineUnitCostColumn, true);
        configureNumberColumn(lineTotalColumn, true);

        orderLineTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldLine, newLine) -> {
                    if (newLine != null) {
                        populateLineForm(newLine);
                    }
                });
    }

    /**
     * Configures how numeric purchase order line columns are displayed.
     *
     * The same method is used for quantity and currency columns.
     *
     * @param column the table column to configure
     * @param currency true if the value should be formatted as money
     */
    private void configureNumberColumn(
            TableColumn<PurchaseOrderLine, Double> column,
            boolean currency
    ) {
        column.setCellFactory(tableColumn -> new TableCell<>() {

            /**
             * Updates the displayed value for a numeric purchase order line
             * cell.
             *
             * @param value the number value to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    Double value,
                    boolean empty
            ) {
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
     * Loads suppliers and inventory items used by the screen.
     *
     * The results are stored in lookup maps so the table can display names
     * instead of only database IDs. The supplier and inventory item combo boxes
     * are also populated from this data.
     */
    private void loadReferenceData() {
        try {
            List<Supplier> suppliers
                    = supplierDAO.getAllSuppliers();

            List<InventoryItem> inventoryItems
                    = inventoryDAO.getActiveInventoryItems();

            suppliersById.clear();

            for (Supplier supplier : suppliers) {
                suppliersById.put(
                        supplier.getSupplierId(),
                        supplier
                );
            }

            inventoryItemsById.clear();

            for (InventoryItem item : inventoryItems) {
                inventoryItemsById.put(
                        item.getInventoryItemId(),
                        item
                );
            }

            supplierComboBox.setItems(
                    FXCollections.observableArrayList(suppliers)
            );

            inventoryItemComboBox.setItems(
                    FXCollections.observableArrayList(inventoryItems)
            );

        } catch (Exception e) {
            showError(
                    "Unable to load suppliers and inventory items.",
                    e
            );
        }
    }

    /**
     * Loads all purchase orders from the database into the purchase order
     * table.
     */
    @FXML
    private void loadPurchaseOrders() {
        try {
            masterPurchaseOrders.setAll(
                    purchaseOrderDAO.getAllPurchaseOrders()
            );

        } catch (Exception e) {
            showError(
                    "Unable to load purchase orders.",
                    e
            );
        }
    }

    /**
     * Loads the selected purchase order into the form.
     *
     * The method fills the supplier, dates, notes, status and line item table
     * using the selected purchase order. The available controls are then
     * enabled or disabled based on the order status.
     *
     * @param purchaseOrder the selected purchase order
     */
    private void loadSelectedPurchaseOrder(PurchaseOrder purchaseOrder) {
        currentPurchaseOrder = purchaseOrder;

        Supplier supplier
                = suppliersById.get(purchaseOrder.getSupplierId());

        supplierComboBox.setValue(supplier);
        orderDatePicker.setValue(parseDate(purchaseOrder.getOrderDate()));
        expectedDeliveryDatePicker.setValue(
                parseDate(purchaseOrder.getExpectedDeliveryDate())
        );

        notesArea.setText(safeText(purchaseOrder.getNotes()));
        currentOrderLabel.setText(
                "Purchase Order #"
                + purchaseOrder.getPurchaseOrderId()
        );
        orderStatusLabel.setText(safeText(purchaseOrder.getStatus()));

        try {
            List<PurchaseOrderLine> lines
                    = purchaseOrderDAO.getPurchaseOrderLines(
                            purchaseOrder.getPurchaseOrderId()
                    );

            currentOrderLines.setAll(lines);

        } catch (Exception e) {
            currentOrderLines.clear();

            showError(
                    "Unable to load the purchase order items.",
                    e
            );
        }

        clearLineForm();
        updateOrderTotal();
        updateControlStates();
    }

    /**
     * Adds a line item to the current purchase order.
     *
     * Only draft purchase orders can be edited. The method validates the
     * selected inventory item, prevents duplicate inventory items within the
     * same order and checks the quantity and unit cost before adding the line.
     */
    @FXML
    private void addOrderLine() {
        if (!isCurrentOrderEditable()) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only draft purchase orders can be edited."
            );

            return;
        }

        try {
            InventoryItem selectedItem
                    = inventoryItemComboBox.getValue();

            if (selectedItem == null) {
                throw new IllegalArgumentException(
                        "Select an inventory item."
                );
            }

            if (containsInventoryItem(
                    selectedItem.getInventoryItemId(),
                    null
            )) {
                throw new IllegalArgumentException(
                        "This inventory item is already included "
                        + "in the purchase order."
                );
            }

            double quantityOrdered
                    = parsePositiveDouble(
                            quantityOrderedField.getText(),
                            "Quantity ordered"
                    );

            double unitCost
                    = parsePositiveDouble(
                            unitCostField.getText(),
                            "Unit cost"
                    );

            PurchaseOrderLine newLine
                    = new PurchaseOrderLine(
                            0,
                            getCurrentPurchaseOrderId(),
                            selectedItem.getInventoryItemId(),
                            quantityOrdered,
                            unitCost
                    );

            currentOrderLines.add(newLine);
            clearLineForm();
            updateOrderTotal();

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.WARNING,
                    e.getMessage()
            );
        }
    }

    /**
     * Updates the selected purchase order line.
     *
     * Only draft purchase orders can be edited. The method validates the
     * selected line, selected inventory item, quantity and unit cost before
     * updating the line in the table.
     */
    @FXML
    private void updateSelectedOrderLine() {
        if (!isCurrentOrderEditable()) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only draft purchase orders can be edited."
            );

            return;
        }

        PurchaseOrderLine selectedLine
                = orderLineTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedLine == null) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select a purchase order item to update."
            );

            return;
        }

        try {
            InventoryItem selectedItem
                    = inventoryItemComboBox.getValue();

            if (selectedItem == null) {
                throw new IllegalArgumentException(
                        "Select an inventory item."
                );
            }

            if (containsInventoryItem(
                    selectedItem.getInventoryItemId(),
                    selectedLine
            )) {
                throw new IllegalArgumentException(
                        "This inventory item is already included "
                        + "in the purchase order."
                );
            }

            double quantityOrdered
                    = parsePositiveDouble(
                            quantityOrderedField.getText(),
                            "Quantity ordered"
                    );

            double unitCost
                    = parsePositiveDouble(
                            unitCostField.getText(),
                            "Unit cost"
                    );

            selectedLine.setInventoryItemId(
                    selectedItem.getInventoryItemId()
            );
            selectedLine.setQuantityOrdered(quantityOrdered);
            selectedLine.setUnitCost(unitCost);

            orderLineTable.refresh();
            clearLineForm();
            updateOrderTotal();

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.WARNING,
                    e.getMessage()
            );
        }
    }

    /**
     * Removes the selected line from the current purchase order.
     *
     * Only draft purchase orders can be edited. Removing the line also updates
     * the displayed order total.
     */
    @FXML
    private void removeSelectedOrderLine() {
        if (!isCurrentOrderEditable()) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only draft purchase orders can be edited."
            );

            return;
        }

        PurchaseOrderLine selectedLine
                = orderLineTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedLine == null) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select a purchase order item to remove."
            );

            return;
        }

        currentOrderLines.remove(selectedLine);
        clearLineForm();
        updateOrderTotal();
    }

    /**
     * Checks whether an inventory item is already present in the current order.
     *
     * The ignored line parameter is used when updating an existing line, so the
     * line being edited does not count as a duplicate of itself.
     *
     * @param inventoryItemId the inventory item ID to check
     * @param ignoredLine the line to ignore during the check, or null
     * @return true if the inventory item is already included, otherwise false
     */
    private boolean containsInventoryItem(
            int inventoryItemId,
            PurchaseOrderLine ignoredLine
    ) {
        for (PurchaseOrderLine line : currentOrderLines) {
            if (line != ignoredLine
                    && line.getInventoryItemId() == inventoryItemId) {

                return true;
            }
        }

        return false;
    }

    /**
     * Populates the purchase order line form with the selected line details.
     *
     * @param line the selected purchase order line
     */
    private void populateLineForm(PurchaseOrderLine line) {
        inventoryItemComboBox.setValue(
                inventoryItemsById.get(line.getInventoryItemId())
        );

        quantityOrderedField.setText(
                formatQuantity(line.getQuantityOrdered())
        );

        unitCostField.setText(
                String.format(Locale.UK, "%.2f", line.getUnitCost())
        );
    }

    /**
     * Clears the purchase order line form.
     *
     * This clears the selected line, selected inventory item, quantity and unit
     * cost fields.
     */
    @FXML
    private void clearLineForm() {
        orderLineTable.getSelectionModel().clearSelection();
        inventoryItemComboBox.setValue(null);
        quantityOrderedField.clear();
        unitCostField.clear();
        inventoryItemComboBox.requestFocus();
    }

    /**
     * Saves the current purchase order and its line items.
     *
     * Only draft purchase orders can be saved. The method validates the
     * purchase order form, copies the current lines and saves the order and
     * lines through the purchase order DAO.
     */
    @FXML
    private void savePurchaseOrder() {
        if (!isCurrentOrderEditable()) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only draft purchase orders can be saved."
            );

            return;
        }

        try {
            PurchaseOrder purchaseOrder
                    = createPurchaseOrderFromForm();

            List<PurchaseOrderLine> linesToSave
                    = createCopiesOfCurrentLines();

            int purchaseOrderId
                    = purchaseOrderDAO.savePurchaseOrderWithLines(
                            purchaseOrder,
                            linesToSave
                    );

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Purchase order saved successfully."
            );

            loadPurchaseOrders();
            selectPurchaseOrder(purchaseOrderId);

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.WARNING,
                    e.getMessage()
            );

        } catch (Exception e) {
            showError(
                    "Unable to save the purchase order.",
                    e
            );
        }
    }

    /**
     * Creates a purchase order object from the form values.
     *
     * The supplier, order date, optional expected delivery date and line items
     * are validated before the purchase order object is created.
     *
     * @return the purchase order created from the current form values
     */
    private PurchaseOrder createPurchaseOrderFromForm() {
        Supplier supplier
                = supplierComboBox.getValue();

        LocalDate orderDate
                = orderDatePicker.getValue();

        LocalDate expectedDeliveryDate
                = expectedDeliveryDatePicker.getValue();

        if (supplier == null) {
            throw new IllegalArgumentException("Select a supplier.");
        }

        if (orderDate == null) {
            throw new IllegalArgumentException("Select an order date.");
        }

        if (expectedDeliveryDate != null
                && expectedDeliveryDate.isBefore(orderDate)) {

            throw new IllegalArgumentException(
                    "The expected delivery date cannot be before the order date."
            );
        }

        if (currentOrderLines.isEmpty()) {
            throw new IllegalArgumentException(
                    "Add at least one inventory item to the purchase order."
            );
        }

        int purchaseOrderId
                = getCurrentPurchaseOrderId();

        return new PurchaseOrder(
                purchaseOrderId,
                supplier.getSupplierId(),
                orderDate.toString(),
                expectedDeliveryDate == null
                        ? null
                        : expectedDeliveryDate.toString(),
                STATUS_DRAFT,
                calculateCurrentOrderTotal(),
                blankToNull(notesArea.getText())
        );
    }

    /**
     * Shows the keyboard shortcuts available on the purchase orders screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Purchase Orders Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + B: Back to Dashboard\n"
                + "Alt + R: Refresh purchase orders\n"
                + "Alt + S: Save draft order\n"
                + "Alt + N: New purchase order\n"
                + "Alt + M: Mark as ordered\n"
                + "Alt + V: Receive order\n"
                + "Alt + C: Cancel order\n"
                + "Alt + D: Delete order\n"
                + "Alt + O: Add item to order\n"
                + "Alt + U: Update selected item\n"
                + "Alt + I: Remove selected item\n"
                + "Alt + L: Clear item form"
        );

        alert.showAndWait();
    }

    /**
     * Creates copies of the current purchase order lines.
     *
     * Copies are used so the saved data is based on the current form state
     * without directly exposing the observable list.
     *
     * @return a list of copied purchase order lines
     */
    private List<PurchaseOrderLine> createCopiesOfCurrentLines() {
        return currentOrderLines.stream()
                .map(line -> new PurchaseOrderLine(
                line.getPurchaseOrderLineId(),
                getCurrentPurchaseOrderId(),
                line.getInventoryItemId(),
                line.getQuantityOrdered(),
                line.getUnitCost()
        ))
                .toList();
    }

    /**
     * Marks the current draft purchase order as ordered.
     *
     * The order must already be saved and must still have a DRAFT status. Once
     * marked as ordered, the purchase order is no longer editable.
     */
    @FXML
    private void markPurchaseOrderAsOrdered() {
        if (currentPurchaseOrder == null
                || currentPurchaseOrder.getPurchaseOrderId() <= 0) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "Save the purchase order before marking it as ordered."
            );

            return;
        }

        if (!STATUS_DRAFT.equals(currentPurchaseOrder.getStatus())) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only draft purchase orders can be marked as ordered."
            );

            return;
        }

        Alert confirmation
                = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Mark Purchase Order as Ordered");
        confirmation.setHeaderText(
                "Mark purchase order #"
                + currentPurchaseOrder.getPurchaseOrderId()
                + " as ordered?"
        );
        confirmation.setContentText(
                "The purchase order will no longer be editable."
        );

        Optional<ButtonType> result
                = confirmation.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            int purchaseOrderId
                    = currentPurchaseOrder.getPurchaseOrderId();

            purchaseOrderDAO.updatePurchaseOrderStatus(
                    purchaseOrderId,
                    STATUS_ORDERED
            );

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Purchase order marked as ordered."
            );

            loadPurchaseOrders();
            selectPurchaseOrder(purchaseOrderId);

        } catch (Exception e) {
            showError(
                    "Unable to mark the purchase order as ordered.",
                    e
            );
        }
    }

    /**
     * Receives the current ordered purchase order into stock.
     *
     * The purchase order must have an ORDERED status. Receiving the order
     * creates inventory batches, increases inventory quantities, adds stock
     * movement records and changes the order status to RECEIVED in one database
     * transaction.
     */
    @FXML
    private void receivePurchaseOrder() {
        if (currentPurchaseOrder == null
                || currentPurchaseOrder.getPurchaseOrderId() <= 0) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "Select a purchase order to receive."
            );

            return;
        }

        if (STATUS_RECEIVED.equals(currentPurchaseOrder.getStatus())) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "This purchase order has already been received."
            );

            return;
        }

        if (!STATUS_ORDERED.equals(currentPurchaseOrder.getStatus())) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only purchase orders with an ORDERED status can be received."
            );

            return;
        }

        Alert confirmation
                = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Receive Purchase Order");
        confirmation.setHeaderText(
                "Receive purchase order #"
                + currentPurchaseOrder.getPurchaseOrderId()
                + "?"
        );
        confirmation.setContentText(
                "Inventory batches will be created and stock quantities will be increased."
        );

        Optional<ButtonType> result
                = confirmation.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        int purchaseOrderId
                = currentPurchaseOrder.getPurchaseOrderId();

        try (Connection conn = DBConnection.getConnection()) {
            boolean originalAutoCommit
                    = conn.getAutoCommit();

            conn.setAutoCommit(false);

            try {
                PurchaseOrder purchaseOrder
                        = purchaseOrderDAO.getPurchaseOrderById(
                                conn,
                                purchaseOrderId
                        );

                if (purchaseOrder == null) {
                    throw new IllegalStateException(
                            "The purchase order could not be found."
                    );
                }

                if (STATUS_RECEIVED.equals(purchaseOrder.getStatus())) {
                    throw new IllegalStateException(
                            "This purchase order has already been received."
                    );
                }

                if (!STATUS_ORDERED.equals(purchaseOrder.getStatus())) {
                    throw new IllegalStateException(
                            "Only an ordered purchase order can be received."
                    );
                }

                List<PurchaseOrderLine> orderLines
                        = purchaseOrderDAO.getPurchaseOrderLines(
                                conn,
                                purchaseOrderId
                        );

                if (orderLines.isEmpty()) {
                    throw new IllegalStateException(
                            "The purchase order has no items."
                    );
                }

                LocalDate receivedDate
                        = LocalDate.now();

                for (PurchaseOrderLine line : orderLines) {
                    createInventoryBatchFromOrderLine(
                            conn,
                            purchaseOrder,
                            line,
                            receivedDate
                    );
                }

                purchaseOrderDAO.updatePurchaseOrderStatus(
                        conn,
                        purchaseOrderId,
                        STATUS_RECEIVED
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
                    "Purchase order received successfully. "
                    + "The inventory quantities and batches have been updated."
            );

            loadPurchaseOrders();
            selectPurchaseOrder(purchaseOrderId);

        } catch (Exception e) {
            showError(
                    "Unable to receive the purchase order.",
                    e
            );
        }
    }

    /**
     * Creates an inventory batch from a purchase order line.
     *
     * The ordered purchase quantity is converted into recipe stock quantity
     * using the inventory item's conversion factor. The method then creates a
     * new inventory batch, increases the inventory quantity and records a stock
     * movement.
     *
     * @param conn the active database connection
     * @param purchaseOrder the purchase order being received
     * @param line the purchase order line being converted into stock
     * @param receivedDate the date the stock was received
     * @throws Exception if the inventory item or database update fails
     */
    private void createInventoryBatchFromOrderLine(
            Connection conn,
            PurchaseOrder purchaseOrder,
            PurchaseOrderLine line,
            LocalDate receivedDate
    ) throws Exception {

        InventoryItem inventoryItem
                = inventoryItemsById.get(line.getInventoryItemId());

        if (inventoryItem == null) {
            inventoryItem
                    = inventoryDAO.getInventoryItemById(
                            line.getInventoryItemId()
                    );
        }

        if (inventoryItem == null) {
            throw new IllegalStateException(
                    "Inventory item could not be found for purchase order line."
            );
        }

        double purchaseQuantity
                = line.getQuantityOrdered();

        double stockQuantity
                = inventoryItem.convertPurchaseQuantityToRecipeQuantity(
                        purchaseQuantity
                );

        double convertedUnitCost
                = line.getUnitCost()
                / inventoryItem.getConversionFactor();

        String batchCode
                = "PO-"
                + purchaseOrder.getPurchaseOrderId()
                + "-LINE-"
                + line.getPurchaseOrderLineId();

        InventoryBatch inventoryBatch
                = new InventoryBatch(
                        0,
                        line.getInventoryItemId(),
                        purchaseOrder.getSupplierId(),
                        batchCode,
                        receivedDate.toString(),
                        null,
                        stockQuantity,
                        stockQuantity,
                        convertedUnitCost
                );

        inventoryBatchDAO.addBatch(conn, inventoryBatch);

        inventoryDAO.updateInventoryQuantity(
                conn,
                line.getInventoryItemId(),
                stockQuantity
        );

        stockMovementDAO.addStockMovement(
                conn,
                line.getInventoryItemId(),
                stockQuantity,
                null,
                "PURCHASE",
                "Purchase order #"
                + purchaseOrder.getPurchaseOrderId()
                + " received. "
                + formatQuantity(purchaseQuantity)
                + " "
                + inventoryItem.getPurchaseUnit()
                + " converted to "
                + formatQuantity(stockQuantity)
                + " "
                + inventoryItem.getRecipeUnit()
                + ". Batch: "
                + batchCode
        );
    }

    /**
     * Cancels the current purchase order.
     *
     * Received purchase orders cannot be cancelled because their stock has
     * already been added. Cancelling an order prevents stock from being added
     * for that order.
     */
    @FXML
    private void cancelPurchaseOrder() {
        if (currentPurchaseOrder == null
                || currentPurchaseOrder.getPurchaseOrderId() <= 0) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "Select a purchase order to cancel."
            );

            return;
        }

        String status
                = currentPurchaseOrder.getStatus();

        if (STATUS_RECEIVED.equals(status)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "A received purchase order cannot be cancelled."
            );

            return;
        }

        if (STATUS_CANCELLED.equals(status)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "This purchase order is already cancelled."
            );

            return;
        }

        Alert confirmation
                = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Cancel Purchase Order");
        confirmation.setHeaderText(
                "Cancel purchase order #"
                + currentPurchaseOrder.getPurchaseOrderId()
                + "?"
        );
        confirmation.setContentText(
                "No stock will be added for this order."
        );

        Optional<ButtonType> result
                = confirmation.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            int purchaseOrderId
                    = currentPurchaseOrder.getPurchaseOrderId();

            purchaseOrderDAO.updatePurchaseOrderStatus(
                    purchaseOrderId,
                    STATUS_CANCELLED
            );

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Purchase order cancelled."
            );

            loadPurchaseOrders();
            selectPurchaseOrder(purchaseOrderId);

        } catch (Exception e) {
            showError(
                    "Unable to cancel the purchase order.",
                    e
            );
        }
    }

    /**
     * Deletes the current purchase order.
     *
     * Only draft or cancelled purchase orders can be deleted. Ordered or
     * received purchase orders are protected so that stock and ordering history
     * are not accidentally removed.
     */
    @FXML
    private void deletePurchaseOrder() {
        if (currentPurchaseOrder == null
                || currentPurchaseOrder.getPurchaseOrderId() <= 0) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "Select a purchase order to delete."
            );

            return;
        }

        String status
                = currentPurchaseOrder.getStatus();

        if (!STATUS_DRAFT.equals(status)
                && !STATUS_CANCELLED.equals(status)) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "Only draft or cancelled purchase orders can be deleted."
            );

            return;
        }

        Alert confirmation
                = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Delete Purchase Order");
        confirmation.setHeaderText(
                "Delete purchase order #"
                + currentPurchaseOrder.getPurchaseOrderId()
                + "?"
        );
        confirmation.setContentText(
                "The purchase order and all of its items will be permanently removed."
        );

        Optional<ButtonType> result
                = confirmation.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            purchaseOrderDAO.deletePurchaseOrder(
                    currentPurchaseOrder.getPurchaseOrderId()
            );

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Purchase order deleted successfully."
            );

            loadPurchaseOrders();
            clearOrderForm();

        } catch (Exception e) {
            showError(
                    "Unable to delete the purchase order.",
                    e
            );
        }
    }

    /**
     * Clears the purchase order form.
     *
     * This resets the screen ready for a new draft purchase order and disables
     * or enables controls based on the new draft state.
     */
    @FXML
    private void clearOrderForm() {
        currentPurchaseOrder = null;

        purchaseOrderTable.getSelectionModel().clearSelection();

        supplierComboBox.setValue(null);
        orderDatePicker.setValue(LocalDate.now());
        expectedDeliveryDatePicker.setValue(null);
        notesArea.clear();

        currentOrderLines.clear();

        currentOrderLabel.setText("New Purchase Order");
        orderStatusLabel.setText(STATUS_DRAFT);

        clearLineForm();
        updateOrderTotal();
        updateControlStates();
    }

    /**
     * Updates the enabled and disabled state of the form controls.
     *
     * Draft orders can be edited, saved and marked as ordered. Ordered orders
     * can be received or cancelled. Received orders and most non-draft orders
     * are protected from editing.
     */
    private void updateControlStates() {
        String status
                = currentPurchaseOrder == null
                        ? STATUS_DRAFT
                        : currentPurchaseOrder.getStatus();

        boolean editable
                = STATUS_DRAFT.equals(status);

        supplierComboBox.setDisable(!editable);
        orderDatePicker.setDisable(!editable);
        expectedDeliveryDatePicker.setDisable(!editable);
        notesArea.setDisable(!editable);
        inventoryItemComboBox.setDisable(!editable);
        quantityOrderedField.setDisable(!editable);
        unitCostField.setDisable(!editable);
        addLineButton.setDisable(!editable);
        updateLineButton.setDisable(!editable);
        removeLineButton.setDisable(!editable);
        saveOrderButton.setDisable(!editable);

        markOrderedButton.setDisable(
                currentPurchaseOrder == null
                || !STATUS_DRAFT.equals(status)
        );

        receiveOrderButton.setDisable(
                currentPurchaseOrder == null
                || !STATUS_ORDERED.equals(status)
        );

        cancelOrderButton.setDisable(
                currentPurchaseOrder == null
                || STATUS_RECEIVED.equals(status)
                || STATUS_CANCELLED.equals(status)
        );

        deleteOrderButton.setDisable(
                currentPurchaseOrder == null
                || (!STATUS_DRAFT.equals(status)
                && !STATUS_CANCELLED.equals(status))
        );
    }

    /**
     * Checks whether the current purchase order can be edited.
     *
     * A new unsaved order and saved draft orders are editable.
     *
     * @return true if the current order is editable, otherwise false
     */
    private boolean isCurrentOrderEditable() {
        return currentPurchaseOrder == null
                || STATUS_DRAFT.equals(currentPurchaseOrder.getStatus());
    }

    /**
     * Updates the displayed purchase order total.
     */
    private void updateOrderTotal() {
        orderTotalLabel.setText(
                "Order total: " + formatMoney(calculateCurrentOrderTotal())
        );
    }

    /**
     * Calculates the total cost of the current purchase order lines.
     *
     * @return the total cost of the current order
     */
    private double calculateCurrentOrderTotal() {
        return currentOrderLines.stream()
                .mapToDouble(PurchaseOrderLine::getLineTotal)
                .sum();
    }

    /**
     * Selects a purchase order in the table by its ID.
     *
     * This is used after saving or updating a purchase order so the same record
     * remains selected.
     *
     * @param purchaseOrderId the purchase order ID to select
     */
    private void selectPurchaseOrder(int purchaseOrderId) {
        for (PurchaseOrder purchaseOrder : purchaseOrderTable.getItems()) {
            if (purchaseOrder.getPurchaseOrderId() == purchaseOrderId) {
                purchaseOrderTable.getSelectionModel().select(purchaseOrder);
                purchaseOrderTable.scrollTo(purchaseOrder);
                return;
            }
        }
    }

    /**
     * Gets the current purchase order ID.
     *
     * Unsaved purchase orders return 0.
     *
     * @return the current purchase order ID, or 0 if no order is selected
     */
    private int getCurrentPurchaseOrderId() {
        if (currentPurchaseOrder == null) {
            return 0;
        }

        return currentPurchaseOrder.getPurchaseOrderId();
    }

    /**
     * Gets the display name for the supplier linked to a purchase order.
     *
     * If the supplier cannot be found in the lookup map, fallback text using
     * the supplier ID is returned.
     *
     * @param purchaseOrder the purchase order to read
     * @return the supplier name or fallback supplier ID text
     */
    private String getSupplierDisplayName(PurchaseOrder purchaseOrder) {
        int supplierId
                = purchaseOrder.getSupplierId();

        Supplier supplier
                = suppliersById.get(supplierId);

        if (supplier == null) {
            return "Supplier ID " + supplierId;
        }

        return supplier.getSupplierName();
    }

    /**
     * Parses and validates a positive decimal number.
     *
     * @param text the text entered by the user
     * @param fieldName the field name used in validation messages
     * @return the parsed positive number
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
     * If rollback fails, the rollback exception is added to the original
     * exception as a suppressed exception.
     *
     * @param conn the active database connection
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
     * general messages on the purchase orders screen.
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

        alert.setTitle("Purchase Orders");
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
