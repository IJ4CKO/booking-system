package com.bookingapp.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bookingapp.dao.BookingDAO;
import com.bookingapp.dao.BookingLineDAO;
import com.bookingapp.dao.CustomerDAO;
import com.bookingapp.dao.ProductDAO;
import com.bookingapp.model.Booking;
import com.bookingapp.model.BookingLine;
import com.bookingapp.model.Customer;
import com.bookingapp.model.Product;
import com.bookingapp.security.UserSession;
import com.bookingapp.service.BookingService;

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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the bookings screen.
 *
 * This class manages the booking user interface. It allows the user to create
 * bookings, search existing bookings, view stock requirements, allocate stock
 * to bookings awaiting stock, cancel bookings and delete cancelled bookings. It
 * also connects the user interface to the booking service and data access
 * classes.
 */
public class BookingController {

    /**
     * Status used when a booking has been fully confirmed.
     */
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    /**
     * Status used when a booking has been accepted but is waiting for stock.
     */
    private static final String STATUS_AWAITING_STOCK = "AWAITING_STOCK";

    /**
     * Status used when a booking has been cancelled.
     */
    private static final String STATUS_CANCELLED = "CANCELLED";

    /**
     * Date format used when storing and displaying booking dates.
     */
    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Combo box used to select the customer for a booking.
     */
    @FXML
    private ComboBox<Customer> customerComboBox;

    /**
     * Date picker used to select the collection date for a booking.
     */
    @FXML
    private DatePicker bookingDatePicker;

    /**
     * Text field used to enter the collection time for a booking.
     */
    @FXML
    private TextField bookingTimeField;

    /**
     * Combo box used to select the product for a booking.
     */
    @FXML
    private ComboBox<Product> productComboBox;

    /**
     * Text field used to enter the product quantity.
     */
    @FXML
    private TextField quantityField;

    /**
     * Date picker used to select the drop-off date, if delivery is required.
     */
    @FXML
    private DatePicker dropoffDatePicker;

    /**
     * Text field used to enter the drop-off time, if delivery is required.
     */
    @FXML
    private TextField dropoffTimeField;

    /**
     * Text field used to enter the drop-off address, if delivery is required.
     */
    @FXML
    private TextField dropoffAddressField;

    /**
     * Text field used to search the bookings table.
     */
    @FXML
    private TextField searchField;

    /**
     * Table used to display booking records.
     */
    @FXML
    private TableView<Booking> bookingTable;

    /**
     * Table column used to display the booking ID.
     */
    @FXML
    private TableColumn<Booking, Number> bookingIdColumn;

    /**
     * Table column used to display the customer name.
     */
    @FXML
    private TableColumn<Booking, String> customerNameColumn;

    /**
     * Table column used to display the collection date.
     */
    @FXML
    private TableColumn<Booking, String> collectionDateColumn;

    /**
     * Table column used to display the collection time.
     */
    @FXML
    private TableColumn<Booking, String> collectionTimeColumn;

    /**
     * Table column used to display the drop-off date.
     */
    @FXML
    private TableColumn<Booking, String> dropoffDateColumn;

    /**
     * Table column used to display the drop-off time.
     */
    @FXML
    private TableColumn<Booking, String> dropoffTimeColumn;

    /**
     * Table column used to display the drop-off address.
     */
    @FXML
    private TableColumn<Booking, String> dropoffAddressColumn;

    /**
     * Table column used to display product quantities linked to a booking.
     */
    @FXML
    private TableColumn<Booking, String> productQuantitiesColumn;

    /**
     * Table column used to display the items required for a booking.
     */
    @FXML
    private TableColumn<Booking, String> itemsRequiredColumn;

    /**
     * Table column used to display the booking status.
     */
    @FXML
    private TableColumn<Booking, String> statusColumn;

    /**
     * Button used to view stock requirements for a booking awaiting stock.
     */
    @FXML
    private Button viewStockRequirementsButton;

    /**
     * Button used to allocate stock and confirm a booking awaiting stock.
     */
    @FXML
    private Button allocateStockButton;

    /**
     * Button used to cancel a selected booking.
     */
    @FXML
    private Button cancelBookingButton;

    /**
     * Button used to delete a selected cancelled booking.
     */
    @FXML
    private Button deleteCancelledBookingButton;

    /**
     * Master list of bookings used by the table, search and sort functions.
     */
    private final ObservableList<Booking> masterBookings
            = FXCollections.observableArrayList();

    /**
     * Service used to run booking workflow logic.
     */
    private final BookingService bookingService
            = new BookingService();

    /**
     * Data access object used for customer database operations.
     */
    private final CustomerDAO customerDAO
            = new CustomerDAO();

    /**
     * Data access object used for product database operations.
     */
    private final ProductDAO productDAO
            = new ProductDAO();

    /**
     * Data access object used for booking database operations.
     */
    private final BookingDAO bookingDAO
            = new BookingDAO();

    /**
     * Data access object used for booking line database operations.
     */
    private final BookingLineDAO bookingLineDAO
            = new BookingLineDAO();

    /**
     * Initialises the bookings screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the table, search box, table selection handling,
     * combo boxes and initial data.
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        configureSearch();
        configureTableSelection();

        customerComboBox.setPromptText("Select customer");
        productComboBox.setPromptText("Select product");

        setupCustomerComboBox();
        setupProductComboBox();

        loadCustomers();
        loadProducts();
        loadBookings();

        updateActionButtons(null);
    }

    /**
     * Configures the booking table columns.
     *
     * Each table column is linked to the correct booking value. Some values are
     * formatted before they are displayed, such as the booking status.
     */
    private void configureTableColumns() {
        bookingIdColumn.setCellValueFactory(data
                -> new SimpleIntegerProperty(
                        data.getValue().getBookingId()
                )
        );

        customerNameColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        nullSafe(data.getValue().getCustomerName())
                )
        );

        collectionDateColumn.setCellValueFactory(data -> {
            Booking booking = data.getValue();

            if (hasText(booking.getDropoffDate())) {
                return new SimpleStringProperty("");
            }

            return new SimpleStringProperty(
                    nullSafe(booking.getCollectionDate())
            );
        });

        collectionTimeColumn.setCellValueFactory(data -> {
            Booking booking = data.getValue();

            if (hasText(booking.getDropoffTime())) {
                return new SimpleStringProperty("");
            }

            return new SimpleStringProperty(
                    nullSafe(booking.getCollectionTime())
            );
        });

        dropoffDateColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        nullSafe(data.getValue().getDropoffDate())
                )
        );

        dropoffTimeColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        nullSafe(data.getValue().getDropoffTime())
                )
        );

        dropoffAddressColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        nullSafe(data.getValue().getDropoffAddress())
                )
        );

        productQuantitiesColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        nullSafe(
                                data.getValue()
                                        .getProductQuantitiesSummary()
                        )
                )
        );

        itemsRequiredColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        nullSafe(data.getValue().getItemsRequired())
                )
        );

        statusColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        formatStatus(data.getValue().getStatus())
                )
        );
    }

    /**
     * Configures the search box for the bookings table.
     *
     * The search box filters the master bookings list and allows the user to
     * search across booking ID, customer name, dates, times, product details,
     * item details and booking status.
     */
    private void configureSearch() {
        FilteredList<Booking> filteredBookings
                = new FilteredList<>(masterBookings, booking -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue)
                -> filteredBookings.setPredicate(booking
                        -> matchesSearch(booking, newValue)
                )
        );

        SortedList<Booking> sortedBookings
                = new SortedList<>(filteredBookings);

        sortedBookings.comparatorProperty()
                .bind(bookingTable.comparatorProperty());

        bookingTable.setItems(sortedBookings);
    }

    /**
     * Checks whether a booking matches the search text.
     *
     * @param booking the booking to check
     * @param searchText the text entered by the user
     * @return true if the booking matches the search text, otherwise false
     */
    private boolean matchesSearch(
            Booking booking,
            String searchText
    ) {
        if (booking == null) {
            return false;
        }

        String searchValue = nullSafe(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(booking.getBookingId()),
                searchValue
        )
                || containsSearchValue(
                        booking.getCustomerName(),
                        searchValue
                )
                || containsSearchValue(
                        booking.getCollectionDate(),
                        searchValue
                )
                || containsSearchValue(
                        booking.getCollectionTime(),
                        searchValue
                )
                || containsSearchValue(
                        booking.getDropoffDate(),
                        searchValue
                )
                || containsSearchValue(
                        booking.getDropoffTime(),
                        searchValue
                )
                || containsSearchValue(
                        booking.getDropoffAddress(),
                        searchValue
                )
                || containsSearchValue(
                        booking.getProductQuantitiesSummary(),
                        searchValue
                )
                || containsSearchValue(
                        booking.getItemsRequired(),
                        searchValue
                )
                || containsSearchValue(
                        booking.getStatus(),
                        searchValue
                )
                || containsSearchValue(
                        formatStatus(booking.getStatus()),
                        searchValue
                );
    }

    /**
     * Checks whether a text value contains the search value.
     *
     * Null values are handled safely by converting them to an empty string
     * before checking.
     *
     * @param value the text value to check
     * @param searchValue the search value to find
     * @return true if the value contains the search value, otherwise false
     */
    private boolean containsSearchValue(
            String value,
            String searchValue
    ) {
        return nullSafe(value)
                .toLowerCase()
                .contains(searchValue);
    }

    /**
     * Configures the booking table selection listener.
     *
     * When the selected booking changes, the action buttons are enabled or
     * disabled depending on the selected booking status.
     */
    private void configureTableSelection() {
        bookingTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldBooking, newBooking)
                        -> updateActionButtons(newBooking)
                );
    }

    /**
     * Configures how customers are displayed in the customer combo box.
     *
     * This keeps the prompt text visible when no customer is selected and shows
     * a readable customer value when a customer is selected.
     */
    private void setupCustomerComboBox() {
        customerComboBox.setButtonCell(
                new ListCell<>() {

            /**
             * Updates the displayed customer text in the combo box.
             *
             * @param item the customer item to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    Customer item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                setText(
                        empty || item == null
                                ? "Select customer"
                                : item.toString()
                );
            }
        }
        );

        customerComboBox.setCellFactory(
                listView -> new ListCell<>() {

            /**
             * Updates the displayed customer text in the drop-down list.
             *
             * @param item the customer item to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    Customer item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                setText(
                        empty || item == null
                                ? null
                                : item.toString()
                );
            }
        }
        );
    }

    /**
     * Configures how products are displayed in the product combo box.
     *
     * This keeps the prompt text visible when no product is selected and shows
     * a readable product value when a product is selected.
     */
    private void setupProductComboBox() {
        productComboBox.setButtonCell(
                new ListCell<>() {

            /**
             * Updates the displayed product text in the combo box.
             *
             * @param item the product item to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    Product item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                setText(
                        empty || item == null
                                ? "Select product"
                                : item.toString()
                );
            }
        }
        );

        productComboBox.setCellFactory(
                listView -> new ListCell<>() {

            /**
             * Updates the displayed product text in the drop-down list.
             *
             * @param item the product item to display
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(
                    Product item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                setText(
                        empty || item == null
                                ? null
                                : item.toString()
                );
            }
        }
        );
    }

    /**
     * Loads customers from the database into the customer combo box.
     *
     * If the customers cannot be loaded, an error message is shown to the user.
     */
    private void loadCustomers() {
        try {
            customerComboBox.setItems(
                    FXCollections.observableArrayList(
                            customerDAO.getAllCustomers()
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading customers.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Loads products from the database into the product combo box.
     *
     * If the products cannot be loaded, an error message is shown to the user.
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
                    "Error loading products.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Creates a new booking from the values entered on the form.
     *
     * The method validates the selected customer, product, date, time and
     * quantity before checking for duplicate bookings. It then creates the
     * booking and booking line through the booking service. Depending on stock
     * availability, the booking may be confirmed immediately or saved as
     * awaiting stock.
     */
    @FXML
    public void createBooking() {
        Integer ownerUserId = getAuthenticatedOwnerUserId();

        if (ownerUserId == null) {
            return;
        }

        try {
            Customer selectedCustomer = customerComboBox.getValue();
            Product selectedProduct = productComboBox.getValue();
            LocalDate selectedDate = bookingDatePicker.getValue();

            String bookingTime = bookingTimeField.getText();
            String enteredQuantity = quantityField.getText();

            LocalDate dropoffDateValue = dropoffDatePicker.getValue();

            String dropoffTime = dropoffTimeField.getText();
            String dropoffAddress = dropoffAddressField.getText();

            if (selectedCustomer == null) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "Please select a customer."
                );

                return;
            }

            if (selectedProduct == null) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "Please select a product."
                );

                return;
            }

            if (selectedDate == null) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "Please select a collection date."
                );

                return;
            }

            if (bookingTime == null || bookingTime.isBlank()) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "Collection time is required."
                );

                return;
            }

            if (enteredQuantity == null || enteredQuantity.isBlank()) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "Quantity is required."
                );

                return;
            }

            int quantity
                    = Integer.parseInt(
                            enteredQuantity.trim()
                    );

            if (quantity <= 0) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "Quantity must be greater than zero."
                );

                return;
            }

            String collectionDate
                    = selectedDate.format(DATE_FORMATTER);

            String cleanBookingTime
                    = bookingTime.trim();

            String dropoffDate
                    = dropoffDateValue == null
                            ? null
                            : dropoffDateValue.format(DATE_FORMATTER);

            String cleanDropoffTime
                    = hasText(dropoffTime)
                    ? dropoffTime.trim()
                    : null;

            String cleanDropoffAddress
                    = hasText(dropoffAddress)
                    ? dropoffAddress.trim()
                    : null;

            boolean duplicateBooking
                    = bookingDAO.bookingAlreadyExists(
                            selectedCustomer.getCustomerId(),
                            collectionDate,
                            cleanBookingTime,
                            selectedProduct.getProductId()
                    );

            if (duplicateBooking) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "A booking for this customer, product, "
                        + "collection date and time already exists."
                );

                return;
            }

            String itemsRequired
                    = selectedProduct.getName()
                    + " x"
                    + quantity;

            Booking booking
                    = new Booking(
                            0,
                            selectedCustomer.getCustomerId(),
                            ownerUserId,
                            collectionDate,
                            cleanBookingTime,
                            dropoffDate,
                            cleanDropoffTime,
                            cleanDropoffAddress,
                            itemsRequired,
                            STATUS_CONFIRMED
                    );

            BookingLine bookingLine
                    = new BookingLine(
                            0,
                            0,
                            selectedProduct.getProductId(),
                            quantity
                    );

            List<BookingLine> bookingLines
                    = new ArrayList<>();

            bookingLines.add(bookingLine);

            int bookingId
                    = bookingService.createConfirmedBooking(
                            booking,
                            bookingLines
                    );

            Booking createdBooking
                    = bookingDAO.getBookingById(bookingId);

            clearBookingForm();
            loadBookings();

            if (createdBooking != null
                    && STATUS_AWAITING_STOCK.equalsIgnoreCase(
                            createdBooking.getStatus()
                    )) {
                String stockSummary
                        = bookingService.getStockShortfallSummary(
                                bookingId
                        );

                showMessage(
                        Alert.AlertType.WARNING,
                        "The advance booking has been created.\n\n"
                        + "Booking ID: "
                        + bookingId
                        + "\nStatus: AWAITING STOCK"
                        + "\n\nNo stock has been deducted."
                        + "\n\nStock requirements:\n"
                        + stockSummary
                        + "\n\nOrder and receive the required "
                        + "stock, then select Allocate Stock "
                        + "and Confirm."
                );

            } else {
                showMessage(
                        Alert.AlertType.INFORMATION,
                        "Booking created successfully.\n\n"
                        + "Booking ID: "
                        + bookingId
                        + "\nStatus: CONFIRMED"
                        + "\n\nThe required stock has been "
                        + "deducted using FIFO."
                );
            }

        } catch (NumberFormatException e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Quantity must be a whole number."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to create the booking.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Shows the stock requirements for the selected booking.
     *
     * This action is only used for bookings with an AWAITING_STOCK status. It
     * displays a readable stock shortfall summary to help the user understand
     * which stock needs to be ordered or received.
     */
    @FXML
    public void viewSelectedStockRequirements() {
        Booking selectedBooking
                = getSelectedBooking(
                        "Please select a booking to view."
                );

        if (selectedBooking == null) {
            return;
        }

        if (!STATUS_AWAITING_STOCK.equalsIgnoreCase(
                selectedBooking.getStatus()
        )) {
            showMessage(
                    Alert.AlertType.INFORMATION,
                    "This booking is not awaiting stock."
            );

            return;
        }

        try {
            String stockSummary
                    = bookingService.getStockShortfallSummary(
                            selectedBooking.getBookingId()
                    );

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Booking ID: "
                    + selectedBooking.getBookingId()
                    + "\nStatus: AWAITING STOCK"
                    + "\n\nStock requirements:\n"
                    + stockSummary
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to load the stock requirements.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Allocates stock to the selected awaiting-stock booking and confirms it.
     *
     * The method asks the user to confirm the action before rechecking stock.
     * If sufficient stock is available, the booking service deducts stock using
     * FIFO and updates the booking status to CONFIRMED.
     */
    @FXML
    public void allocateStockAndConfirmSelectedBooking() {
        Booking selectedBooking
                = getSelectedBooking(
                        "Please select a booking awaiting stock."
                );

        if (selectedBooking == null) {
            return;
        }

        if (!STATUS_AWAITING_STOCK.equalsIgnoreCase(
                selectedBooking.getStatus()
        )) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only a booking with an AWAITING STOCK status "
                    + "can use this action."
            );

            return;
        }

        try {
            String stockSummary
                    = bookingService.getStockShortfallSummary(
                            selectedBooking.getBookingId()
                    );

            Alert confirmation
                    = new Alert(
                            Alert.AlertType.CONFIRMATION
                    );

            confirmation.setTitle("Allocate Stock and Confirm");
            confirmation.setHeaderText("Recheck and allocate stock?");

            confirmation.setContentText(
                    "Booking ID: "
                    + selectedBooking.getBookingId()
                    + "\nCustomer: "
                    + nullSafe(selectedBooking.getCustomerName())
                    + "\n\nCurrent stock assessment:\n"
                    + stockSummary
                    + "\n\nThe booking will only be confirmed "
                    + "when sufficient stock is physically "
                    + "available."
            );

            confirmation.getButtonTypes().setAll(
                    ButtonType.YES,
                    ButtonType.NO
            );

            Optional<ButtonType> result
                    = confirmation.showAndWait();

            if (result.isEmpty()
                    || result.get() != ButtonType.YES) {

                return;
            }

            bookingService.allocateStockAndConfirm(
                    selectedBooking.getBookingId()
            );

            loadBookings();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Stock has been allocated successfully.\n\n"
                    + "Booking ID: "
                    + selectedBooking.getBookingId()
                    + "\nStatus: CONFIRMED"
                    + "\n\nThe required stock has been "
                    + "deducted using FIFO."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "The booking could not be confirmed.\n\n"
                    + safeExceptionMessage(e)
            );

            loadBookings();
        }
    }

    /**
     * Cancels the selected booking.
     *
     * A confirmation message is shown before the booking is cancelled. If the
     * booking was confirmed, the booking service restores the original FIFO
     * stock. If the booking was awaiting stock, no stock is restored because no
     * stock was deducted.
     */
    @FXML
    public void cancelSelectedBooking() {
        Booking selectedBooking
                = getSelectedBooking(
                        "Please select a booking to cancel."
                );

        if (selectedBooking == null) {
            return;
        }

        if (STATUS_CANCELLED.equalsIgnoreCase(
                selectedBooking.getStatus()
        )) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "This booking is already cancelled."
            );

            return;
        }

        Alert confirmation
                = new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle("Cancel Booking");
        confirmation.setHeaderText("Cancel the selected booking?");

        confirmation.setContentText(
                "Booking ID: "
                + selectedBooking.getBookingId()
                + "\nCustomer: "
                + nullSafe(selectedBooking.getCustomerName())
                + "\n\nConfirmed bookings will have their "
                + "original FIFO stock returned."
                + "\nBookings awaiting stock have no stock "
                + "to return."
        );

        confirmation.getButtonTypes().setAll(
                ButtonType.YES,
                ButtonType.NO
        );

        Optional<ButtonType> result
                = confirmation.showAndWait();

        if (result.isEmpty()
                || result.get() != ButtonType.YES) {

            return;
        }

        try {
            bookingService.cancelBooking(
                    selectedBooking.getBookingId()
            );

            loadBookings();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Booking cancelled successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to cancel the booking.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Deletes the selected cancelled booking.
     *
     * Only bookings with a CANCELLED status can be deleted. A confirmation
     * message is shown before the cancelled booking is permanently removed.
     */
    @FXML
    public void deleteSelectedCancelledBooking() {
        Booking selectedBooking
                = getSelectedBooking(
                        "Please select a booking to delete."
                );

        if (selectedBooking == null) {
            return;
        }

        if (!STATUS_CANCELLED.equalsIgnoreCase(
                selectedBooking.getStatus()
        )) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Only cancelled bookings can be deleted."
            );

            return;
        }

        Alert confirmation
                = new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle("Delete Cancelled Booking");

        confirmation.setHeaderText(
                "Permanently delete this cancelled booking?"
        );

        confirmation.setContentText(
                "Booking ID: "
                + selectedBooking.getBookingId()
                + "\nCustomer: "
                + nullSafe(selectedBooking.getCustomerName())
        );

        confirmation.getButtonTypes().setAll(
                ButtonType.YES,
                ButtonType.NO
        );

        Optional<ButtonType> result
                = confirmation.showAndWait();

        if (result.isEmpty()
                || result.get() != ButtonType.YES) {

            return;
        }

        try {
            bookingService.deleteCancelledBooking(
                    selectedBooking.getBookingId()
            );

            loadBookings();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Cancelled booking deleted successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to delete the cancelled booking.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Shows the keyboard shortcuts available on the bookings screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Bookings Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + B: Back to Dashboard\n"
                + "Alt + C: Create Booking\n"
                + "Alt + V: View Stock Requirements\n"
                + "Alt + A: Allocate Stock and Confirm\n"
                + "Alt + N: Cancel Selected Booking\n"
                + "Alt + D: Delete Selected Cancelled Booking\n"
                + "Alt + R: Refresh Bookings"
        );

        alert.showAndWait();
    }

    /**
     * Loads all bookings from the database into the table.
     *
     * Product quantity summaries are added to each booking before the table is
     * refreshed. The current table selection is cleared after loading.
     */
    @FXML
    public void loadBookings() {
        try {
            List<Booking> bookings
                    = bookingDAO.getAllBookings();

            for (Booking booking : bookings) {
                if (booking == null) {
                    continue;
                }

                booking.setProductQuantitiesSummary(
                        buildProductQuantitySummary(
                                booking.getBookingId()
                        )
                );
            }

            masterBookings.setAll(bookings);

            bookingTable
                    .getSelectionModel()
                    .clearSelection();

            updateActionButtons(null);

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading bookings.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Returns the user to the dashboard screen.
     *
     * @param event the JavaFX action event from the Back button
     */
    @FXML
    private void goBack(
            ActionEvent event
    ) {
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

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to return to the dashboard.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Gets the currently selected booking from the table.
     *
     * If no booking is selected, an error message is shown to the user.
     *
     * @param missingSelectionMessage the message shown when no booking is
     * selected
     * @return the selected booking, or null if no booking is selected
     */
    private Booking getSelectedBooking(
            String missingSelectionMessage
    ) {
        Booking selectedBooking
                = bookingTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedBooking == null) {
            showMessage(
                    Alert.AlertType.ERROR,
                    missingSelectionMessage
            );
        }

        return selectedBooking;
    }

    /**
     * Enables or disables action buttons based on the selected booking.
     *
     * Awaiting-stock bookings can have their stock requirements viewed and
     * stock allocated. Cancelled bookings can be deleted. Active bookings can
     * be cancelled.
     *
     * @param booking the currently selected booking, or null if nothing is
     * selected
     */
    private void updateActionButtons(
            Booking booking
    ) {
        boolean hasSelection = booking != null;

        boolean awaitingStock
                = hasSelection
                && STATUS_AWAITING_STOCK.equalsIgnoreCase(
                        booking.getStatus()
                );

        boolean cancelled
                = hasSelection
                && STATUS_CANCELLED.equalsIgnoreCase(
                        booking.getStatus()
                );

        viewStockRequirementsButton.setDisable(!awaitingStock);
        allocateStockButton.setDisable(!awaitingStock);
        cancelBookingButton.setDisable(!hasSelection || cancelled);
        deleteCancelledBookingButton.setDisable(!cancelled);
    }

    /**
     * Gets the authenticated owner user ID from the current session.
     *
     * If the user is not logged in, an error message is shown and null is
     * returned.
     *
     * @return the signed-in owner user ID, or null if no valid session exists
     */
    private Integer getAuthenticatedOwnerUserId() {
        if (!UserSession.isLoggedIn()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Your login session is no longer available. "
                    + "Please log in again before creating "
                    + "a booking."
            );

            return null;
        }

        try {
            return UserSession.getOwnerUserId();

        } catch (IllegalStateException e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Your login session is no longer available. "
                    + "Please log in again before creating "
                    + "a booking."
            );

            return null;
        }
    }

    /**
     * Clears the booking form.
     *
     * This resets the selected customer, selected product, date fields, time
     * fields, quantity field and drop-off address field after a booking has
     * been created.
     */
    private void clearBookingForm() {
        customerComboBox
                .getSelectionModel()
                .clearSelection();

        customerComboBox.setValue(null);

        bookingDatePicker.setValue(null);
        bookingTimeField.clear();

        productComboBox
                .getSelectionModel()
                .clearSelection();

        productComboBox.setValue(null);

        quantityField.clear();

        dropoffDatePicker.setValue(null);
        dropoffTimeField.clear();
        dropoffAddressField.clear();
    }

    /**
     * Formats a booking status for display.
     *
     * Underscores are replaced with spaces so statuses such as AWAITING_STOCK
     * are shown in a more readable format.
     *
     * @param status the status value to format
     * @return the formatted status text
     */
    private String formatStatus(
            String status
    ) {
        if (status == null || status.isBlank()) {
            return "";
        }

        return status.trim()
                .replace("_", " ");
    }

    /**
     * Checks whether a text value contains non-blank text.
     *
     * @param value the text value to check
     * @return true if the value is not null and not blank, otherwise false
     */
    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.isBlank();
    }

    /**
     * Safely converts a text value into a non-null string.
     *
     * @param value the text value to check
     * @return the original value, or an empty string if the value is null
     */
    private String nullSafe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }

    /**
     * Gets a safe error message from an exception.
     *
     * If the exception or its message is missing, a generic error message is
     * returned instead.
     *
     * @param exception the exception to read
     * @return a readable error message
     */
    private String safeExceptionMessage(
            Exception exception
    ) {
        if (exception == null
                || exception.getMessage() == null
                || exception.getMessage().isBlank()) {

            return "An unexpected error occurred.";
        }

        return exception.getMessage();
    }

    /**
     * Builds a product quantity summary for a booking.
     *
     * The booking lines are loaded from the database and converted into a
     * readable summary, such as product name and quantity.
     *
     * @param bookingId the booking ID to build the summary for
     * @return a readable product quantity summary
     */
    private String buildProductQuantitySummary(
            int bookingId
    ) {
        try {
            List<BookingLine> bookingLines
                    = bookingLineDAO.getBookingLinesByBookingId(
                            bookingId
                    );

            List<String> productQuantities
                    = new ArrayList<>();

            for (BookingLine line : bookingLines) {
                if (line == null) {
                    continue;
                }

                String productName
                        = hasText(line.getProductName())
                        ? line.getProductName()
                        : "Product ID " + line.getProductId();

                productQuantities.add(
                        productName
                        + ": "
                        + line.getQuantity()
                );
            }

            return String.join(
                    "; ",
                    productQuantities
            );

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Shows a message dialog to the user.
     *
     * This is used for validation errors, confirmation results, warnings and
     * general information messages on the bookings screen.
     *
     * @param type the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert = new Alert(type);

        alert.setTitle("Bookings");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
