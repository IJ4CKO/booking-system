package com.bookingapp.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.bookingapp.dao.BookingDAO;
import com.bookingapp.dao.BookingRequestDAO;
import com.bookingapp.model.Booking;
import com.bookingapp.model.BookingRequest;
import com.bookingapp.security.UserSession;
import com.bookingapp.service.BookingService;
import com.bookingapp.service.DepositCalculator;
import com.bookingapp.service.EmailService;

import javafx.application.Platform;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/**
 * Controller for the website booking requests screen.
 *
 * This class manages the staff-facing screen where website booking requests are
 * reviewed. It allows the business owner or staff to calculate deposits,
 * provisionally accept requests, mark deposits as paid, confirm requests as
 * bookings, reject requests, edit unpaid requests and delete unpaid requests.
 */
public class BookingRequestController {

    /**
     * Status used when a booking request has been converted into a confirmed
     * booking.
     */
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    /**
     * Status used when a booking has been accepted internally but is waiting
     * for stock to be received before stock can be allocated.
     */
    private static final String STATUS_AWAITING_STOCK = "AWAITING_STOCK";

    /**
     * Date format used by the website booking request form.
     */
    private static final DateTimeFormatter WEBSITE_DATE_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Date format used for displaying dates in the desktop application.
     */
    private static final DateTimeFormatter DESKTOP_DATE_FORMAT
            = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Date and time format used for values stored in the database.
     */
    private static final DateTimeFormatter DATABASE_DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Date and time format used when showing stored date and time values to the
     * user.
     */
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Table used to display website booking requests.
     */
    @FXML
    private TableView<BookingRequest> bookingRequestTable;

    /**
     * Table column used to display the request ID.
     */
    @FXML
    private TableColumn<BookingRequest, Number> requestIdColumn;

    /**
     * Table column used to display the customer's full name.
     */
    @FXML
    private TableColumn<BookingRequest, String> fullNameColumn;

    /**
     * Table column used to display the customer's email address.
     */
    @FXML
    private TableColumn<BookingRequest, String> emailColumn;

    /**
     * Table column used to display the customer's phone number.
     */
    @FXML
    private TableColumn<BookingRequest, String> phoneColumn;

    /**
     * Table column used to display the requested service type summary.
     */
    @FXML
    private TableColumn<BookingRequest, String> serviceTypeColumn;

    /**
     * Table column used to display requested product names and quantities.
     */
    @FXML
    private TableColumn<BookingRequest, String> productQuantitiesColumn;

    /**
     * Table column used to display whether the request is for collection or
     * delivery.
     */
    @FXML
    private TableColumn<BookingRequest, String> collectionOrDeliveryColumn;

    /**
     * Table column used to display the delivery address, if delivery is needed.
     */
    @FXML
    private TableColumn<BookingRequest, String> dropoffAddressColumn;

    /**
     * Table column used to display the customer's extra request details.
     */
    @FXML
    private TableColumn<BookingRequest, String> itemsRequiredColumn;

    /**
     * Table column used to display the requested event date.
     */
    @FXML
    private TableColumn<BookingRequest, String> eventDateColumn;

    /**
     * Table column used to display the requested event time.
     */
    @FXML
    private TableColumn<BookingRequest, String> eventTimeColumn;

    /**
     * Table column used to display the final order total.
     */
    @FXML
    private TableColumn<BookingRequest, String> orderTotalColumn;

    /**
     * Table column used to display the required deposit.
     */
    @FXML
    private TableColumn<BookingRequest, String> requiredDepositColumn;

    /**
     * Table column used to display the current deposit status.
     */
    @FXML
    private TableColumn<BookingRequest, String> depositStatusColumn;

    /**
     * Table column used to display when the deposit was paid.
     */
    @FXML
    private TableColumn<BookingRequest, String> depositPaidDateColumn;

    /**
     * Table column used to display the customer's payment reference.
     */
    @FXML
    private TableColumn<BookingRequest, String> paymentReferenceColumn;

    /**
     * Table column used to display the current request status.
     */
    @FXML
    private TableColumn<BookingRequest, String> statusColumn;

    /**
     * Text field used to search the booking request table.
     */
    @FXML
    private TextField searchField;

    /**
     * Text field used to enter the final order total.
     */
    @FXML
    private TextField orderTotalField;

    /**
     * Text field used to enter the customer's payment reference.
     */
    @FXML
    private TextField paymentReferenceField;

    /**
     * Label used to display the calculated required deposit.
     */
    @FXML
    private Label requiredDepositLabel;

    /**
     * Button used to calculate and save the required deposit.
     */
    @FXML
    private Button calculateDepositButton;

    /**
     * Button used to provisionally accept a request and move it to awaiting
     * deposit.
     */
    @FXML
    private Button provisionallyAcceptButton;

    /**
     * Button used to mark a pending deposit as paid.
     */
    @FXML
    private Button markDepositPaidButton;

    /**
     * Button used to confirm a paid request as a booking.
     */
    @FXML
    private Button confirmBookingButton;

    /**
     * Button used to reject a booking request.
     */
    @FXML
    private Button rejectRequestButton;

    /**
     * Button used to edit an unpaid and unconfirmed booking request.
     */
    @FXML
    private Button editRequestButton;

    /**
     * Button used to delete an unpaid and unconfirmed booking request.
     */
    @FXML
    private Button deleteRequestButton;

    /**
     * Master list of booking requests used by the table, search and sort
     * functions.
     */
    private final ObservableList<BookingRequest> masterBookingRequests
            = FXCollections.observableArrayList();

    /**
     * Data access object used for booking request database operations.
     */
    private final BookingRequestDAO bookingRequestDAO
            = new BookingRequestDAO();

    /**
     * Data access object used for booking database operations.
     */
    private final BookingDAO bookingDAO
            = new BookingDAO();

    /**
     * Service used to process booking and stock workflow logic.
     */
    private final BookingService bookingService
            = new BookingService();

    /**
     * Service used to send customer emails.
     */
    private final EmailService emailService
            = new EmailService();

    /**
     * Initialises the booking requests screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares the table columns, search box, table selection
     * behaviour and initial booking request data.
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        configureSearch();
        configureTableSelection();
        loadBookingRequests();
    }

    /**
     * Configures the booking request table columns.
     *
     * Each table column is linked to the correct booking request value. Some
     * values are formatted before display, such as currency, status text and
     * database date-time values.
     */
    private void configureTableColumns() {
        requestIdColumn.setCellValueFactory(data
                -> new SimpleIntegerProperty(data.getValue().getRequestId())
        );

        fullNameColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getFullName())
                )
        );

        emailColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getEmail())
                )
        );

        phoneColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getPhone())
                )
        );

        serviceTypeColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(
                                data.getValue()
                                        .getRequestedServicesSummary()
                        )
                )
        );

        productQuantitiesColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(
                                data.getValue()
                                        .getProductQuantitiesSummary()
                        )
                )
        );

        collectionOrDeliveryColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(
                                data.getValue()
                                        .getCollectionOrDelivery()
                        )
                )
        );

        dropoffAddressColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(
                                data.getValue()
                                        .getDropoffAddress()
                        )
                )
        );

        itemsRequiredColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getDetails())
                )
        );

        eventDateColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getEventDate())
                )
        );

        eventTimeColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getEventTime())
                )
        );

        orderTotalColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        formatCurrency(data.getValue().getOrderTotal())
                )
        );

        requiredDepositColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        formatCurrency(
                                data.getValue().getRequiredDeposit()
                        )
                )
        );

        depositStatusColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(data.getValue().getDepositStatus())
                )
        );

        depositPaidDateColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        formatDatabaseDateTime(
                                data.getValue().getDepositPaidDate()
                        )
                )
        );

        paymentReferenceColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        safeText(
                                data.getValue().getPaymentReference()
                        )
                )
        );

        statusColumn.setCellValueFactory(data
                -> new SimpleStringProperty(
                        formatStatus(data.getValue().getStatus())
                )
        );
    }

    /**
     * Configures the search box for the booking request table.
     *
     * The search box filters the master request list and allows the user to
     * search across customer details, request details, dates, status, deposit
     * information and payment reference values.
     */
    private void configureSearch() {
        FilteredList<BookingRequest> filteredRequests
                = new FilteredList<>(masterBookingRequests, request -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue)
                -> filteredRequests.setPredicate(request
                        -> matchesSearch(request, newValue)
                )
        );

        SortedList<BookingRequest> sortedRequests
                = new SortedList<>(filteredRequests);

        sortedRequests.comparatorProperty()
                .bind(bookingRequestTable.comparatorProperty());

        bookingRequestTable.setItems(sortedRequests);
    }

    /**
     * Checks whether a booking request matches the search text.
     *
     * @param request    the booking request to check
     * @param searchText the text entered by the user
     * @return true if the request matches the search text, otherwise false
     */
    private boolean matchesSearch(
            BookingRequest request,
            String searchText
    ) {
        if (request == null) {
            return false;
        }

        String searchValue = safeText(searchText).toLowerCase();

        if (searchValue.isBlank()) {
            return true;
        }

        return containsSearchValue(
                String.valueOf(request.getRequestId()),
                searchValue
        )
                || containsSearchValue(
                        request.getFullName(),
                        searchValue
                )
                || containsSearchValue(
                        request.getEmail(),
                        searchValue
                )
                || containsSearchValue(
                        request.getPhone(),
                        searchValue
                )
                || containsSearchValue(
                        request.getRequestedServicesSummary(),
                        searchValue
                )
                || containsSearchValue(
                        request.getProductQuantitiesSummary(),
                        searchValue
                )
                || containsSearchValue(
                        request.getCollectionOrDelivery(),
                        searchValue
                )
                || containsSearchValue(
                        request.getDropoffAddress(),
                        searchValue
                )
                || containsSearchValue(
                        request.getDetails(),
                        searchValue
                )
                || containsSearchValue(
                        request.getEventDate(),
                        searchValue
                )
                || containsSearchValue(
                        request.getEventTime(),
                        searchValue
                )
                || containsSearchValue(
                        formatCurrency(request.getOrderTotal()),
                        searchValue
                )
                || containsSearchValue(
                        formatCurrency(request.getRequiredDeposit()),
                        searchValue
                )
                || containsSearchValue(
                        request.getDepositStatus(),
                        searchValue
                )
                || containsSearchValue(
                        formatDatabaseDateTime(
                                request.getDepositPaidDate()
                        ),
                        searchValue
                )
                || containsSearchValue(
                        request.getPaymentReference(),
                        searchValue
                )
                || containsSearchValue(
                        request.getStatus(),
                        searchValue
                )
                || containsSearchValue(
                        formatStatus(request.getStatus()),
                        searchValue
                );
    }

    /**
 * Shows the keyboard shortcuts available on the booking requests screen.
 *
 * This keeps the page less cluttered while still making keyboard support
 * available to users who cannot easily use a mouse.
 */
@FXML
private void showKeyboardShortcuts() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);

    alert.setTitle("Keyboard Shortcuts");
    alert.setHeaderText("Booking Requests Keyboard Shortcuts");

    alert.setContentText(
            "Tab / Shift + Tab: Move between controls\n"
            + "Enter or Space: Select the focused button\n\n"
            + "Alt + K: Show keyboard shortcuts\n"
            + "Alt + B: Back to Dashboard\n"
            + "Alt + L: Calculate and save deposit\n"
            + "Alt + M: Mark deposit as paid\n"
            + "Alt + P: Provisionally accept request\n"
            + "Alt + C: Confirm booking\n"
            + "Alt + J: Reject request\n"
            + "Alt + E: Edit request\n"
            + "Alt + D: Delete request\n"
            + "Alt + R: Refresh requests"
    );

    alert.showAndWait();
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
     * When the selected request changes, the input fields and action buttons are
     * updated to match the selected request's status.
     */
    private void configureTableSelection() {
        bookingRequestTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldRequest, newRequest)
                        -> displaySelectedRequest(newRequest)
                );
    }

    /**
     * Loads pending and active booking requests from the database.
     *
     * The table selection is cleared after loading and the input fields are
     * reset. If loading fails, an error message is shown to the user.
     */
    @FXML
    public void loadBookingRequests() {
        try {
            masterBookingRequests.setAll(
                    bookingRequestDAO.getPendingBookingRequests()
            );

            bookingRequestTable
                    .getSelectionModel()
                    .clearSelection();

            clearInputFields();

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Error loading booking requests.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Calculates and saves the required deposit for the selected request.
     *
     * The request must still be awaiting review before the price can be
     * changed. The final order total is validated, the required deposit is
     * calculated and both values are saved to the database.
     */
    @FXML
    public void calculateAndSaveDeposit() {
        BookingRequest selectedRequest = getSelectedRequest();

        if (selectedRequest == null) {
            return;
        }

        String currentStatus = safeText(selectedRequest.getStatus());

        boolean pricingCanBeChanged
                = "PENDING".equalsIgnoreCase(currentStatus)
                || "PENDING_REVIEW".equalsIgnoreCase(currentStatus);

        if (!pricingCanBeChanged) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "The price cannot be changed because "
                    + "this request has already been reviewed."
            );

            return;
        }

        BigDecimal orderTotal;

        try {
            orderTotal = parseCurrency(orderTotalField.getText());

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    e.getMessage()
            );

            orderTotalField.requestFocus();
            return;
        }

        BigDecimal requiredDeposit;

        try {
            requiredDeposit
                    = DepositCalculator.calculateRequiredDeposit(orderTotal);

        } catch (IllegalArgumentException e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    e.getMessage()
            );

            return;
        }

        try {
            bookingRequestDAO.updateOrderTotalAndDeposit(
                    selectedRequest.getRequestId(),
                    orderTotal,
                    requiredDeposit
            );

            selectedRequest.setOrderTotal(orderTotal);
            selectedRequest.setRequiredDeposit(requiredDeposit);

            bookingRequestTable.refresh();
            displaySelectedRequest(selectedRequest);

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "The order total and required deposit have been saved."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to save the order total and required deposit.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Provisionally accepts the selected booking request.
     *
     * The request must still be awaiting review and must already have a saved
     * order total and deposit. Stock and product checks are completed before the
     * request is moved to AWAITING_DEPOSIT and before the customer is asked to
     * pay a deposit.
     */
    @FXML
    public void acceptSelectedRequest() {
        BookingRequest selectedRequest = getSelectedRequest();

        if (selectedRequest == null) {
            return;
        }

        String currentStatus = safeText(selectedRequest.getStatus());

        boolean isAwaitingReview
                = "PENDING".equalsIgnoreCase(currentStatus)
                || "PENDING_REVIEW".equalsIgnoreCase(currentStatus);

        if (!isAwaitingReview) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "This request has already been reviewed."
            );

            return;
        }

        if (selectedRequest.getOrderTotal() == null
                || selectedRequest.getRequiredDeposit() == null) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "Enter the final order total and save the required "
                    + "deposit before provisionally accepting "
                    + "the request."
            );

            orderTotalField.requestFocus();
            return;
        }

        LocalDate eventDate = parseRequestEventDate(selectedRequest);

        if (eventDate == null) {
            return;
        }

        if (safeText(selectedRequest.getEventTime()).isBlank()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "This request does not have a valid time."
            );

            return;
        }

        /*
         * Product and stock validation happens before the request is moved to
         * AWAITING_DEPOSIT and before the customer is asked to pay a deposit.
         */
        try {
            bookingService.validateWebsiteRequestBeforeDeposit(
                    selectedRequest
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    safeExceptionMessage(e)
            );

            return;
        }

        BigDecimal orderTotal = selectedRequest.getOrderTotal();
        BigDecimal requiredDeposit = selectedRequest.getRequiredDeposit();

        BigDecimal remainingBalance
                = calculateRemainingBalance(orderTotal, requiredDeposit);

        String customerEmail = selectedRequest.getEmail();
        String customerName = selectedRequest.getFullName();
        String eventTime = selectedRequest.getEventTime();

        String formattedEmailDate
                = eventDate.format(DESKTOP_DATE_FORMAT);

        try {
            boolean updated
                    = bookingRequestDAO.markAwaitingDeposit(
                            selectedRequest.getRequestId()
                    );

            if (!updated) {
                showMessage(
                        Alert.AlertType.WARNING,
                        "The request could not be updated. "
                        + "It may already have been reviewed."
                );

                loadBookingRequests();
                return;
            }

            loadBookingRequests();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "The request has been provisionally accepted and is now "
                    + "awaiting a deposit of "
                    + formatCurrency(requiredDeposit)
                    + "."
            );

            sendEmailInBackground(
                    () -> {
                        try {
                            emailService.sendBookingAwaitingDepositEmail(
                                    customerEmail,
                                    customerName,
                                    formattedEmailDate,
                                    eventTime,
                                    orderTotal,
                                    requiredDeposit,
                                    remainingBalance
                            );

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    "The request was updated, but the deposit-request "
                    + "email could not be sent."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to provisionally accept the booking request.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Marks the selected request's deposit as paid.
     *
     * The request must be awaiting deposit and the deposit status must still be
     * PENDING. Once recorded, the request can move towards confirmation.
     */
    @FXML
    public void markDepositPaid() {
        BookingRequest selectedRequest = getSelectedRequest();

        if (selectedRequest == null) {
            return;
        }

        String requestStatus = safeText(selectedRequest.getStatus());
        String depositStatus = safeText(selectedRequest.getDepositStatus());

        if (!"AWAITING_DEPOSIT".equalsIgnoreCase(requestStatus)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only a request awaiting its deposit can be marked "
                    + "as paid."
            );

            return;
        }

        if ("PAID".equalsIgnoreCase(depositStatus)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "This deposit has already been recorded as paid."
            );

            return;
        }

        if (!"PENDING".equalsIgnoreCase(depositStatus)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "This request does not currently have a pending deposit."
            );

            return;
        }

        String paymentReference = paymentReferenceField.getText();

        try {
            boolean updated
                    = bookingRequestDAO.markDepositPaid(
                            selectedRequest.getRequestId(),
                            paymentReference
                    );

            if (!updated) {
                showMessage(
                        Alert.AlertType.WARNING,
                        "The deposit could not be recorded. "
                        + "It may already have been updated."
                );

                loadBookingRequests();
                return;
            }

            loadBookingRequests();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "The deposit has been recorded as paid. "
                    + "The request can now be processed."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to record the deposit payment.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Confirms the selected paid booking request as a booking.
     *
     * The request must be READY_TO_CONFIRM, the deposit must be PAID, and the
     * request must not already be linked to a booking. The booking service then
     * creates the booking, checks stock and deducts stock using FIFO when
     * stock is available.
     */
    @FXML
    public void confirmSelectedBooking() {
        BookingRequest selectedRequest = getSelectedRequest();

        if (selectedRequest == null) {
            return;
        }

        String requestStatus = safeText(selectedRequest.getStatus());
        String depositStatus = safeText(selectedRequest.getDepositStatus());

        if (!"READY_TO_CONFIRM"
                .equalsIgnoreCase(
                        requestStatus
                )) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "Only a request ready to confirm "
                    + "can be processed."
            );

            return;
        }

        if (!"PAID".equalsIgnoreCase(depositStatus)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "The deposit must be recorded as paid before "
                    + "the booking is processed."
            );

            return;
        }

        if (selectedRequest.getBookingId() != null) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "This request is already linked to booking ID "
                    + selectedRequest.getBookingId()
                    + "."
            );

            return;
        }

        Integer ownerUserId = getAuthenticatedOwnerUserId();

        if (ownerUserId == null) {
            return;
        }

        LocalDate eventDate = parseRequestEventDate(selectedRequest);

        if (eventDate == null) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Process Booking Request");
        confirmation.setHeaderText("Create a booking from this request?");

        confirmation.setContentText(
                "Request ID: "
                + selectedRequest.getRequestId()
                + "\nCustomer: "
                + safeText(selectedRequest.getFullName())
                + "\nDate: "
                + eventDate.format(DESKTOP_DATE_FORMAT)
                + "\nTime: "
                + safeText(selectedRequest.getEventTime())
                + "\nOrder total: "
                + formatCurrency(selectedRequest.getOrderTotal())
                + "\nDeposit received: "
                + formatCurrency(selectedRequest.getRequiredDeposit())
                + "\n\nWhen sufficient stock is available, the "
                + "booking will be confirmed and stock will be "
                + "deducted using FIFO."
                + "\n\nWhen stock is currently insufficient, an "
                + "advance booking can be recorded internally as "
                + "AWAITING STOCK. The customer will still receive "
                + "a booking confirmation because the deposit has "
                + "been paid."
                + "\n\nA short notice booking with insufficient "
                + "stock will be blocked."
        );

        confirmation.getButtonTypes().setAll(
                ButtonType.YES,
                ButtonType.NO
        );

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        String customerEmail = selectedRequest.getEmail();
        String customerName = selectedRequest.getFullName();
        int requestId = selectedRequest.getRequestId();

        String formattedDate = eventDate.format(DESKTOP_DATE_FORMAT);
        String eventTime = selectedRequest.getEventTime();
        String fulfilmentType = selectedRequest.getCollectionOrDelivery();
        String dropoffAddress = selectedRequest.getDropoffAddress();

        BigDecimal orderTotal = selectedRequest.getOrderTotal();
        BigDecimal depositPaid = selectedRequest.getRequiredDeposit();

        BigDecimal remainingBalance
                = calculateRemainingBalance(orderTotal, depositPaid);

        String paymentReference = selectedRequest.getPaymentReference();

        try {
            int bookingId
                    = bookingService.createConfirmedBookingFromRequest(
                            selectedRequest,
                            ownerUserId
                    );

            String createdBookingStatus
                    = loadCreatedBookingStatus(bookingId);

            loadBookingRequests();

            if (STATUS_AWAITING_STOCK.equalsIgnoreCase(
                    createdBookingStatus
            )) {
                String stockSummary = loadStockSummary(bookingId);

                showMessage(
                        Alert.AlertType.WARNING,
                        "The website request has been processed.\n\n"
                        + "Request ID: "
                        + requestId
                        + "\nBooking ID: "
                        + bookingId
                        + "\nCustomer-facing status: CONFIRMED"
                        + "\nInternal stock status: AWAITING STOCK"
                        + "\n\nThe customer has paid the deposit, "
                        + "so they will receive a booking "
                        + "confirmation email."
                        + "\n\nThe booking is far enough in advance "
                        + "to be accepted, but there is not "
                        + "currently enough usable stock to "
                        + "allocate internally."
                        + "\n\nNo stock has been deducted yet."
                        + "\n\nStock requirements:\n"
                        + stockSummary
                        + "\n\nOrder or receive the required stock, "
                        + "then use Allocate Stock and Confirm on "
                        + "the Bookings page to deduct the stock "
                        + "using FIFO."
                );

                sendEmailInBackground(
                        () -> {
                            try {
                                emailService.sendBookingConfirmedEmail(
                                        customerEmail,
                                        customerName,
                                        requestId,
                                        bookingId,
                                        formattedDate,
                                        eventTime,
                                        fulfilmentType,
                                        dropoffAddress,
                                        orderTotal,
                                        depositPaid,
                                        remainingBalance,
                                        paymentReference
                                );

                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        "The booking was recorded as awaiting stock, "
                        + "but the customer confirmation email "
                        + "could not be sent."
                );

            } else if (STATUS_CONFIRMED.equalsIgnoreCase(
                    createdBookingStatus
            )) {
                showMessage(
                        Alert.AlertType.INFORMATION,
                        "The request has been confirmed.\n\n"
                        + "Request ID: "
                        + requestId
                        + "\nBooking ID: "
                        + bookingId
                        + "\nStatus: CONFIRMED"
                        + "\n\nThe booking has been added to the "
                        + "booking list and the required stock "
                        + "has been deducted using FIFO."
                );

                sendEmailInBackground(
                        () -> {
                            try {
                                emailService.sendBookingConfirmedEmail(
                                        customerEmail,
                                        customerName,
                                        requestId,
                                        bookingId,
                                        formattedDate,
                                        eventTime,
                                        fulfilmentType,
                                        dropoffAddress,
                                        orderTotal,
                                        depositPaid,
                                        remainingBalance,
                                        paymentReference
                                );

                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        "The booking was confirmed, but the confirmation "
                        + "email could not be sent."
                );

            } else {
                showMessage(
                        Alert.AlertType.WARNING,
                        "The website request has been processed and "
                        + "linked to booking ID "
                        + bookingId
                        + ", but the booking status could not "
                        + "be reloaded."
                        + "\n\nOpen the Bookings page and verify "
                        + "its status before sending a final "
                        + "confirmation to the customer."
                );
            }

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "The booking request could not be processed.\n\n"
                    + safeExceptionMessage(e)
            );

            loadBookingRequests();
        }
    }

    /**
     * Rejects the selected booking request.
     *
     * The user must enter a rejection reason before the request is rejected.
     * Requests with paid deposits or final statuses cannot be rejected through
     * this action.
     */
    @FXML
    public void rejectSelectedRequest() {
        BookingRequest selectedRequest = getSelectedRequest();

        if (selectedRequest == null) {
            return;
        }

        String currentStatus = safeText(selectedRequest.getStatus());
        String depositStatus = safeText(selectedRequest.getDepositStatus());

        if ("PAID".equalsIgnoreCase(depositStatus)) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "This request has a paid deposit. Use the refund "
                    + "process instead of rejecting it."
            );

            return;
        }

        if ("REJECTED".equalsIgnoreCase(currentStatus)
                || STATUS_CONFIRMED.equalsIgnoreCase(currentStatus)
                || "CANCELLED".equalsIgnoreCase(currentStatus)) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "This request can no longer be rejected."
            );

            return;
        }

        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Reject Booking Request");

        dialog.setHeaderText(
                "Enter a reason for rejecting this booking request:"
        );

        dialog.setContentText("Reason:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) {
            return;
        }

        String rejectionReason = result.get().trim();

        if (rejectionReason.isBlank()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Please enter a rejection reason."
            );

            return;
        }

        try {
            bookingRequestDAO.rejectRequest(
                    selectedRequest.getRequestId(),
                    rejectionReason
            );

            loadBookingRequests();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Booking request rejected."
            );

            sendEmailInBackground(
                    () -> {
                        try {
                            emailService.sendBookingRejectedEmail(
                                    selectedRequest.getEmail(),
                                    selectedRequest.getFullName(),
                                    rejectionReason
                            );

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    "The request was rejected, but the rejection email "
                    + "could not be sent."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to reject booking request.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Edits the selected unpaid and unconfirmed booking request.
     *
     * Only requests that have not been linked to a booking and do not have a
     * paid deposit can be edited. The method prompts the user for the editable
     * customer, fulfilment, date, time and detail values before updating the
     * database.
     */
    @FXML
    public void editSelectedRequest() {
        BookingRequest selectedRequest = getSelectedRequest();

        if (selectedRequest == null) {
            return;
        }

        String requestStatus = safeText(selectedRequest.getStatus());
        String depositStatus = safeText(selectedRequest.getDepositStatus());

        boolean canEdit
                = selectedRequest.getBookingId() == null
                && !"PAID".equalsIgnoreCase(depositStatus)
                && ("PENDING".equalsIgnoreCase(requestStatus)
                || "PENDING_REVIEW".equalsIgnoreCase(requestStatus)
                || "AWAITING_DEPOSIT".equalsIgnoreCase(requestStatus));

        if (!canEdit) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only unconfirmed requests without a paid deposit "
                    + "can be edited."
            );

            return;
        }

        Optional<String> fullNameResult = promptForText(
                "Edit Booking Request",
                "Update the customer name:",
                "Full name:",
                selectedRequest.getFullName(),
                true
        );

        if (fullNameResult.isEmpty()) {
            return;
        }

        Optional<String> emailResult = promptForText(
                "Edit Booking Request",
                "Update the customer email address:",
                "Email:",
                selectedRequest.getEmail(),
                true
        );

        if (emailResult.isEmpty()) {
            return;
        }

        Optional<String> phoneResult = promptForText(
                "Edit Booking Request",
                "Update the customer phone number:",
                "Phone:",
                selectedRequest.getPhone(),
                true
        );

        if (phoneResult.isEmpty()) {
            return;
        }

        Optional<String> fulfilmentResult = promptForText(
                "Edit Booking Request",
                "Update the fulfilment type:",
                "COLLECTION or DELIVERY:",
                selectedRequest.getCollectionOrDelivery(),
                true
        );

        if (fulfilmentResult.isEmpty()) {
            return;
        }

        Optional<String> addressResult = promptForText(
                "Edit Booking Request",
                "Update the delivery address if required:",
                "Delivery address:",
                selectedRequest.getDropoffAddress(),
                false
        );

        if (addressResult.isEmpty()) {
            return;
        }

        Optional<String> dateResult = promptForText(
                "Edit Booking Request",
                "Update the event date:",
                "Event date yyyy-MM-dd:",
                selectedRequest.getEventDate(),
                true
        );

        if (dateResult.isEmpty()) {
            return;
        }

        try {
            LocalDate.parse(
                    dateResult.get(),
                    WEBSITE_DATE_FORMAT
            );

        } catch (Exception e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Enter a valid event date using yyyy-MM-dd."
            );

            return;
        }

        Optional<String> timeResult = promptForText(
                "Edit Booking Request",
                "Update the event time:",
                "Event time:",
                selectedRequest.getEventTime(),
                true
        );

        if (timeResult.isEmpty()) {
            return;
        }

        Optional<String> detailsResult = promptForText(
                "Edit Booking Request",
                "Update any additional details:",
                "Additional details:",
                selectedRequest.getDetails(),
                false
        );

        if (detailsResult.isEmpty()) {
            return;
        }

        try {
            boolean updated = bookingRequestDAO.updateEditableRequest(
                    selectedRequest.getRequestId(),
                    fullNameResult.get(),
                    emailResult.get(),
                    phoneResult.get(),
                    fulfilmentResult.get().toUpperCase(),
                    addressResult.get(),
                    dateResult.get(),
                    timeResult.get(),
                    detailsResult.get()
            );

            if (!updated) {
                showMessage(
                        Alert.AlertType.WARNING,
                        "The request could not be edited. It may already "
                        + "have been paid, confirmed or linked to a booking."
                );

                loadBookingRequests();
                return;
            }

            loadBookingRequests();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "The booking request has been updated."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to edit the booking request.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Deletes the selected unpaid and unconfirmed booking request.
     *
     * Only requests that have not been linked to a booking and do not have a
     * paid deposit can be deleted. The user must confirm before the request and
     * its requested product lines are removed.
     */
    @FXML
    public void deleteSelectedRequest() {
        BookingRequest selectedRequest = getSelectedRequest();

        if (selectedRequest == null) {
            return;
        }

        String requestStatus = safeText(selectedRequest.getStatus());
        String depositStatus = safeText(selectedRequest.getDepositStatus());

        boolean canDelete
                = selectedRequest.getBookingId() == null
                && !"PAID".equalsIgnoreCase(depositStatus)
                && ("PENDING".equalsIgnoreCase(requestStatus)
                || "PENDING_REVIEW".equalsIgnoreCase(requestStatus)
                || "AWAITING_DEPOSIT".equalsIgnoreCase(requestStatus));

        if (!canDelete) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Only unconfirmed requests without a paid deposit "
                    + "can be deleted."
            );

            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Delete Booking Request");
        confirmation.setHeaderText("Delete this booking request?");
        confirmation.setContentText(
                "Request ID: "
                + selectedRequest.getRequestId()
                + "\nCustomer: "
                + safeText(selectedRequest.getFullName())
                + "\n\nThis will remove the website request and its "
                + "requested product lines."
        );

        confirmation.getButtonTypes().setAll(
                ButtonType.YES,
                ButtonType.NO
        );

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        try {
            boolean deleted = bookingRequestDAO.deleteEditableRequest(
                    selectedRequest.getRequestId()
            );

            if (!deleted) {
                showMessage(
                        Alert.AlertType.WARNING,
                        "The request could not be deleted. It may already "
                        + "have been paid, confirmed or linked to a booking."
                );

                loadBookingRequests();
                return;
            }

            loadBookingRequests();

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "The booking request has been deleted."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to delete the booking request.\n\n"
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

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to return to the dashboard.\n\n"
                    + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Gets the currently selected booking request from the table.
     *
     * If no request is selected, an error message is shown to the user.
     *
     * @return the selected booking request, or null if no request is selected
     */
    private BookingRequest getSelectedRequest() {
        BookingRequest selectedRequest
                = bookingRequestTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedRequest == null) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Please select a booking request."
            );
        }

        return selectedRequest;
    }

    /**
     * Displays the selected request details in the input controls.
     *
     * This method also enables or disables the action buttons depending on the
     * request status, deposit status and whether the request is already linked
     * to a booking.
     *
     * @param request the selected booking request, or null if no request is selected
     */
    private void displaySelectedRequest(BookingRequest request) {
        if (request == null) {
            clearInputFields();
            return;
        }

        if (request.getOrderTotal() == null) {
            orderTotalField.clear();

        } else {
            orderTotalField.setText(
                    request.getOrderTotal()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
                            .toPlainString()
            );
        }

        requiredDepositLabel.setText(
                request.getRequiredDeposit() == null
                ? "Not calculated"
                : formatCurrency(request.getRequiredDeposit())
        );

        paymentReferenceField.setText(
                safeText(request.getPaymentReference())
        );

        String requestStatus = safeText(request.getStatus());
        String depositStatus = safeText(request.getDepositStatus());

        boolean canEditPrice
                = "PENDING".equalsIgnoreCase(requestStatus)
                || "PENDING_REVIEW"
                        .equalsIgnoreCase(requestStatus);

        boolean priceHasBeenSaved
                = request.getOrderTotal() != null
                && request.getRequiredDeposit() != null;

        boolean canRecordDeposit
                = "AWAITING_DEPOSIT".equalsIgnoreCase(requestStatus)
                && "PENDING".equalsIgnoreCase(depositStatus);

        boolean canConfirm
                = "READY_TO_CONFIRM".equalsIgnoreCase(requestStatus)
                && "PAID".equalsIgnoreCase(depositStatus)
                && request.getBookingId() == null;

        boolean canReject
                = !"PAID".equalsIgnoreCase(depositStatus)
                && !STATUS_CONFIRMED
                        .equalsIgnoreCase(requestStatus)
                && !"CANCELLED".equalsIgnoreCase(requestStatus)
                && !"REJECTED".equalsIgnoreCase(requestStatus);

        boolean canEditRequest
                = request.getBookingId() == null
                && !"PAID".equalsIgnoreCase(depositStatus)
                && ("PENDING".equalsIgnoreCase(requestStatus)
                || "PENDING_REVIEW".equalsIgnoreCase(requestStatus)
                || "AWAITING_DEPOSIT".equalsIgnoreCase(requestStatus));

        boolean canDeleteRequest
                = canEditRequest;

        orderTotalField.setDisable(!canEditPrice);
        paymentReferenceField.setDisable(!canRecordDeposit);

        calculateDepositButton.setDisable(!canEditPrice);
        provisionallyAcceptButton.setDisable(
                !(canEditPrice && priceHasBeenSaved)
        );

        markDepositPaidButton.setDisable(!canRecordDeposit);
        confirmBookingButton.setDisable(!canConfirm);
        rejectRequestButton.setDisable(!canReject);

        editRequestButton.setDisable(!canEditRequest);
        deleteRequestButton.setDisable(!canDeleteRequest);
    }

    /**
     * Clears the input fields and disables the action buttons.
     *
     * This is used when no request is selected or after the table data has been
     * reloaded.
     */
    private void clearInputFields() {
        orderTotalField.clear();
        paymentReferenceField.clear();

        requiredDepositLabel.setText("Not calculated");

        orderTotalField.setDisable(true);
        paymentReferenceField.setDisable(true);

        calculateDepositButton.setDisable(true);
        provisionallyAcceptButton.setDisable(true);
        markDepositPaidButton.setDisable(true);
        confirmBookingButton.setDisable(true);
        rejectRequestButton.setDisable(true);
        editRequestButton.setDisable(true);
        deleteRequestButton.setDisable(true);
    }

    /**
     * Parses the event date stored in a booking request.
     *
     * If the request has no valid date, an error message is shown and null is
     * returned.
     *
     * @param request the booking request containing the event date
     * @return the parsed event date, or null if the date is invalid
     */
    private LocalDate parseRequestEventDate(
            BookingRequest request
    ) {
        if (request == null
                || safeText(request.getEventDate()).isBlank()) {

            showMessage(
                    Alert.AlertType.ERROR,
                    "This request does not have a valid date."
            );

            return null;
        }

        try {
            return LocalDate.parse(
                    request.getEventDate(),
                    WEBSITE_DATE_FORMAT
            );

        } catch (Exception e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "The request contains an invalid event date."
            );

            return null;
        }
    }

    /**
     * Parses a currency value entered by the user.
     *
     * The method removes pound signs and commas before converting the value to a
     * BigDecimal. The value must be greater than £0.00.
     *
     * @param enteredValue the text entered by the user
     * @return the parsed currency value
     * @throws IllegalArgumentException if the value is missing, invalid or not greater than zero
     */
    private BigDecimal parseCurrency(String enteredValue) {
        if (enteredValue == null || enteredValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter the final order total."
            );
        }

        String cleanedValue
                = enteredValue.trim()
                        .replace("£", "")
                        .replace(",", "");

        try {
            BigDecimal value
                    = new BigDecimal(cleanedValue)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "The order total must be greater than £0.00."
                );
            }

            return value;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Enter a valid order total, for example 175.00."
            );
        }
    }

    /**
     * Calculates the remaining balance after a deposit.
     *
     * If the order total is missing, zero is returned. If the deposit is larger
     * than the order total, the remaining balance is also treated as zero.
     *
     * @param orderTotal the final order total
     * @param deposit    the deposit amount paid or required
     * @return the remaining balance
     */
    private BigDecimal calculateRemainingBalance(
            BigDecimal orderTotal,
            BigDecimal deposit
    ) {
        if (orderTotal == null) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal safeDeposit
                = deposit == null
                        ? BigDecimal.ZERO
                        : deposit;

        BigDecimal balance = orderTotal.subtract(safeDeposit);

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balance = BigDecimal.ZERO;
        }

        return balance.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    /**
     * Loads the status of a booking that has just been created.
     *
     * This is used after confirming a website request so the screen can show
     * whether the new booking is CONFIRMED or AWAITING_STOCK.
     *
     * @param bookingId the booking ID to check
     * @return the booking status, or an empty string if it cannot be loaded
     */
    private String loadCreatedBookingStatus(int bookingId) {
        try {
            Booking booking = bookingDAO.getBookingById(bookingId);

            if (booking == null) {
                return "";
            }

            return safeText(booking.getStatus());

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Loads the stock shortfall summary for a booking.
     *
     * If the stock summary cannot be loaded, a readable fallback message is
     * returned so the user knows what to do next.
     *
     * @param bookingId the booking ID to check
     * @return the stock summary or fallback message
     */
    private String loadStockSummary(int bookingId) {
        try {
            return bookingService.getStockShortfallSummary(bookingId);

        } catch (Exception e) {
            e.printStackTrace();

            return "The stock requirement summary could not be loaded. "
                    + "Open the Bookings page and select View Stock "
                    + "Requirements.";
        }
    }

    /**
     * Formats a money value for display.
     *
     * Null values are displayed as an empty string. Other values are rounded to
     * two decimal places and shown with the pound symbol.
     *
     * @param value the currency value to format
     * @return the formatted currency value
     */
    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "";
        }

        return "£"
                + value.setScale(
                        2,
                        RoundingMode.HALF_UP
                ).toPlainString();
    }

    /**
     * Formats a database date-time value for display.
     *
     * If the date-time cannot be parsed, the original database value is returned
     * so that the user can still see the stored information.
     *
     * @param databaseValue the date-time value stored in the database
     * @return the formatted date-time value
     */
    private String formatDatabaseDateTime(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return "";
        }

        try {
            LocalDateTime dateTime
                    = LocalDateTime.parse(
                            databaseValue,
                            DATABASE_DATE_TIME_FORMAT
                    );

            return dateTime.format(DISPLAY_DATE_TIME_FORMAT);

        } catch (Exception e) {
            return databaseValue;
        }
    }

    /**
     * Formats a request status for display.
     *
     * Underscores are replaced with spaces so statuses such as
     * READY_TO_CONFIRM are shown in a more readable form.
     *
     * @param status the status value to format
     * @return the formatted status text
     */
    private String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        return status.trim().replace("_", " ");
    }

    /**
     * Sends an email on a background thread.
     *
     * This prevents the JavaFX user interface from freezing while an email is
     * being sent. If sending fails, a warning message is shown on the JavaFX
     * application thread.
     *
     * @param emailTask      the email task to run
     * @param warningMessage the warning message to show if sending fails
     */
    private void sendEmailInBackground(
            Runnable emailTask,
            String warningMessage
    ) {
        Thread thread = new Thread(() -> {
            try {
                emailTask.run();

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(()
                        -> showMessage(
                                Alert.AlertType.WARNING,
                                warningMessage
                        )
                );
            }
        });

        thread.setDaemon(true);
        thread.start();
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
                    + "Please log in again before confirming "
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
                    + "Please log in again before confirming "
                    + "a booking."
            );

            return null;
        }
    }

    /**
     * Prompts the user to enter or update a text value.
     *
     * If the value is required and the user enters a blank value, an error
     * message is shown and an empty Optional is returned.
     *
     * @param title        the dialog title
     * @param header       the dialog header text
     * @param label        the label shown beside the text input
     * @param currentValue the current value to show in the text input
     * @param required     whether the value is required
     * @return the entered text, or an empty Optional if cancelled or invalid
     */
    private Optional<String> promptForText(
            String title,
            String header,
            String label,
            String currentValue,
            boolean required
    ) {
        TextInputDialog dialog = new TextInputDialog(
                safeText(currentValue)
        );

        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(label);

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) {
            return Optional.empty();
        }

        String value = safeText(result.get());

        if (required && value.isBlank()) {
            showMessage(
                    Alert.AlertType.ERROR,
                    label.replace(":", "")
                    + " is required."
            );

            return Optional.empty();
        }

        return Optional.of(value);
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
    private String safeExceptionMessage(Exception exception) {
        if (exception == null
                || exception.getMessage() == null
                || exception.getMessage().isBlank()) {

            return "An unexpected error occurred.";
        }

        return exception.getMessage();
    }

    /**
     * Safely trims text.
     *
     * Null values are converted into an empty string so that validation and
     * display logic can handle text safely.
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
     * This is used for validation errors, warnings, confirmations and general
     * information messages on the booking requests screen.
     *
     * @param type    the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert = new Alert(type);

        alert.setTitle("Booking Requests");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}