package com.bookingapp.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.bookingapp.dao.BookingRequestDAO;
import com.bookingapp.model.BookingRequest;

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
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the booking request history screen.
 *
 * This class manages the view-only history screen for confirmed and rejected
 * website booking requests. It displays separate tables for confirmed and
 * rejected requests, supports searching across both tables, updates count
 * labels and allows the user to return to the dashboard.
 */
public class BookingRequestHistoryController {

    /**
     * Date format used by dates stored in the database.
     */
    private static final DateTimeFormatter DATABASE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Date format used when displaying dates to the user.
     */
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Date and time format used by date-time values stored in the database.
     */
    private static final DateTimeFormatter DATABASE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Date and time format used when displaying date-time values to the user.
     */
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Text field used to search both confirmed and rejected request history.
     */
    @FXML
    private TextField searchField;

    /**
     * Table used to display confirmed booking requests.
     */
    @FXML
    private TableView<BookingRequest> confirmedRequestTable;

    /**
     * Table column used to display the confirmed request reference.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedRequestReferenceColumn;

    /**
     * Table column used to display the confirmed booking reference.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedBookingReferenceColumn;

    /**
     * Table column used to display the confirmed request customer name.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedCustomerColumn;

    /**
     * Table column used to display the confirmed request customer email.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedEmailColumn;

    /**
     * Table column used to display the confirmed request service summary.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedServicesColumn;

    /**
     * Table column used to display product names and quantities for confirmed
     * requests.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedProductQuantitiesColumn;

    /**
     * Table column used to display whether the confirmed request was for
     * collection or delivery.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedFulfilmentColumn;

    /**
     * Table column used to display the confirmed request event date.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedEventDateColumn;

    /**
     * Table column used to display the confirmed request event time.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedEventTimeColumn;

    /**
     * Table column used to display the final order total for confirmed
     * requests.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedOrderTotalColumn;

    /**
     * Table column used to display the required deposit for confirmed requests.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedDepositColumn;

    /**
     * Table column used to display the deposit paid date for confirmed requests.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedPaidDateColumn;

    /**
     * Table column used to display the payment reference for confirmed requests.
     */
    @FXML
    private TableColumn<BookingRequest, String> confirmedPaymentReferenceColumn;

    /**
     * Table used to display rejected booking requests.
     */
    @FXML
    private TableView<BookingRequest> rejectedRequestTable;

    /**
     * Table column used to display the rejected request reference.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedRequestReferenceColumn;

    /**
     * Table column used to display the rejected request customer name.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedCustomerColumn;

    /**
     * Table column used to display the rejected request customer email.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedEmailColumn;

    /**
     * Table column used to display the rejected request service summary.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedServicesColumn;

    /**
     * Table column used to display product names and quantities for rejected
     * requests.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedProductQuantitiesColumn;

    /**
     * Table column used to display the rejected request event date.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedEventDateColumn;

    /**
     * Table column used to display the rejected request event time.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedEventTimeColumn;

    /**
     * Table column used to display the final order total for rejected requests.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedOrderTotalColumn;

    /**
     * Table column used to display when the rejected request was submitted.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedSubmittedColumn;

    /**
     * Table column used to display the rejection reason.
     */
    @FXML
    private TableColumn<BookingRequest, String> rejectedReasonColumn;

    /**
     * Label used to display the number of confirmed requests currently shown.
     */
    @FXML
    private Label confirmedCountLabel;

    /**
     * Label used to display the number of rejected requests currently shown.
     */
    @FXML
    private Label rejectedCountLabel;

    /**
     * Master list of confirmed requests used by the confirmed history table.
     */
    private final ObservableList<BookingRequest> confirmedMasterRequests =
            FXCollections.observableArrayList();

    /**
     * Master list of rejected requests used by the rejected history table.
     */
    private final ObservableList<BookingRequest> rejectedMasterRequests =
            FXCollections.observableArrayList();

    /**
     * Filtered list used to search confirmed request history.
     */
    private FilteredList<BookingRequest> filteredConfirmedRequests;

    /**
     * Filtered list used to search rejected request history.
     */
    private FilteredList<BookingRequest> filteredRejectedRequests;

    /**
     * Data access object used for booking request history database operations.
     */
    private final BookingRequestDAO bookingRequestDAO =
            new BookingRequestDAO();

    /**
     * Initialises the booking request history screen.
     *
     * This method is called automatically by JavaFX after the FXML file has
     * loaded. It prepares both history tables, configures searching and loads
     * the confirmed and rejected request history.
     */
    @FXML
    public void initialize() {
        configureConfirmedTable();
        configureRejectedTable();
        configureSearch();
        loadHistory();
    }

    /**
     * Configures the confirmed request history table.
     *
     * Each table column is linked to the correct booking request value. Some
     * values are formatted before display, such as request references, booking
     * references, dates and currency values.
     */
    private void configureConfirmedTable() {
        confirmedRequestReferenceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatRequestReference(data.getValue().getRequestId())
                )
        );

        confirmedBookingReferenceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatBookingReference(data.getValue().getBookingId())
                )
        );

        confirmedCustomerColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getFullName())
                )
        );

        confirmedEmailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getEmail())
                )
        );

        confirmedServicesColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getRequestedServicesSummary())
                )
        );

        confirmedProductQuantitiesColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getProductQuantitiesSummary())
                )
        );

        confirmedFulfilmentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getCollectionOrDelivery())
                )
        );

        confirmedEventDateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatDate(data.getValue().getEventDate())
                )
        );

        confirmedEventTimeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getEventTime())
                )
        );

        confirmedOrderTotalColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatCurrency(data.getValue().getOrderTotal())
                )
        );

        confirmedDepositColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatCurrency(data.getValue().getRequiredDeposit())
                )
        );

        confirmedPaidDateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatDateTime(data.getValue().getDepositPaidDate())
                )
        );

        confirmedPaymentReferenceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getPaymentReference())
                )
        );
    }

    /**
     * Configures the rejected request history table.
     *
     * Each table column is linked to the correct booking request value. Some
     * values are formatted before display, such as request references, dates,
     * submitted date-time values and currency values.
     */
    private void configureRejectedTable() {
        rejectedRequestReferenceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatRequestReference(data.getValue().getRequestId())
                )
        );

        rejectedCustomerColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getFullName())
                )
        );

        rejectedEmailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getEmail())
                )
        );

        rejectedServicesColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getRequestedServicesSummary())
                )
        );

        rejectedProductQuantitiesColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getProductQuantitiesSummary())
                )
        );

        rejectedEventDateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatDate(data.getValue().getEventDate())
                )
        );

        rejectedEventTimeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getEventTime())
                )
        );

        rejectedOrderTotalColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatCurrency(data.getValue().getOrderTotal())
                )
        );

        rejectedSubmittedColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatDateTime(data.getValue().getSubmittedAt())
                )
        );

        rejectedReasonColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safeText(data.getValue().getRejectionReason())
                )
        );
    }

    /**
     * Configures searching and sorting for both history tables.
     *
     * The search field filters the confirmed and rejected request lists at the
     * same time. Each filtered list is wrapped in a sorted list so that table
     * sorting still works.
     */
    private void configureSearch() {
        filteredConfirmedRequests =
                new FilteredList<>(confirmedMasterRequests, request -> true);

        filteredRejectedRequests =
                new FilteredList<>(rejectedMasterRequests, request -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> applySearch(newValue)
        );

        SortedList<BookingRequest> sortedConfirmedRequests =
                new SortedList<>(filteredConfirmedRequests);

        sortedConfirmedRequests.comparatorProperty()
                .bind(confirmedRequestTable.comparatorProperty());

        confirmedRequestTable.setItems(sortedConfirmedRequests);

        SortedList<BookingRequest> sortedRejectedRequests =
                new SortedList<>(filteredRejectedRequests);

        sortedRejectedRequests.comparatorProperty()
                .bind(rejectedRequestTable.comparatorProperty());

        rejectedRequestTable.setItems(sortedRejectedRequests);
    }

    /**
     * Applies the search text to both confirmed and rejected request lists.
     *
     * After filtering both lists, the count labels are updated so the user can
     * see how many matching records are currently displayed.
     *
     * @param searchText the text entered into the search field
     */
    private void applySearch(String searchText) {
        filteredConfirmedRequests.setPredicate(request ->
                matchesSearch(request, searchText)
        );

        filteredRejectedRequests.setPredicate(request ->
                matchesSearch(request, searchText)
        );

        updateCountLabels();
    }

    /**
     * Checks whether a booking request matches the search text.
     *
     * The search checks references, customer details, request details, dates,
     * fulfilment information, payment information, rejection reason and status.
     *
     * @param request    the booking request to check
     * @param searchText the search text entered by the user
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
                formatRequestReference(request.getRequestId()),
                searchValue
        )
                || containsSearchValue(
                        formatBookingReference(request.getBookingId()),
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
                        request.getEventDate(),
                        searchValue
                )
                || containsSearchValue(
                        formatDate(request.getEventDate()),
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
                        formatDateTime(request.getDepositPaidDate()),
                        searchValue
                )
                || containsSearchValue(
                        request.getPaymentReference(),
                        searchValue
                )
                || containsSearchValue(
                        request.getRejectionReason(),
                        searchValue
                )
                || containsSearchValue(
                        formatDateTime(request.getSubmittedAt()),
                        searchValue
                )
                || containsSearchValue(
                        request.getStatus(),
                        searchValue
                );
    }

    /**
 * Shows the keyboard shortcuts available on the booking request history screen.
 *
 * This keeps the page less cluttered while still making keyboard support
 * available to users who cannot easily use a mouse.
 */
@FXML
private void showKeyboardShortcuts() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);

    alert.setTitle("Keyboard Shortcuts");
    alert.setHeaderText("Booking Request History Keyboard Shortcuts");

    alert.setContentText(
            "Tab / Shift + Tab: Move between controls\n"
            + "Enter or Space: Select the focused button\n\n"
            + "Alt + K: Show keyboard shortcuts\n"
            + "Alt + R: Refresh history\n"
            + "Alt + B: Back to Dashboard"
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
     * Loads confirmed and rejected booking request history from the database.
     *
     * The confirmed and rejected master lists are refreshed, the current search
     * value is reapplied and the count labels are updated.
     */
    @FXML
    public void loadHistory() {
        try {
            List<BookingRequest> confirmedRequests =
                    bookingRequestDAO.getConfirmedBookingRequestHistory();

            List<BookingRequest> rejectedRequests =
                    bookingRequestDAO.getRejectedBookingRequestHistory();

            confirmedMasterRequests.setAll(confirmedRequests);
            rejectedMasterRequests.setAll(rejectedRequests);

            applySearch(searchField == null ? "" : searchField.getText());

        } catch (Exception e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to load the booking request history.\n\n"
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

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to return to the dashboard.\n\n"
                            + safeExceptionMessage(e)
            );
        }
    }

    /**
     * Updates the confirmed and rejected request count labels.
     *
     * If filtered lists are available, the counts are based on the filtered
     * search results. Otherwise, the counts are based on the full master lists.
     */
    private void updateCountLabels() {
        int confirmedCount =
                filteredConfirmedRequests == null
                        ? confirmedMasterRequests.size()
                        : filteredConfirmedRequests.size();

        int rejectedCount =
                filteredRejectedRequests == null
                        ? rejectedMasterRequests.size()
                        : filteredRejectedRequests.size();

        confirmedCountLabel.setText(
                confirmedCount
                        + (confirmedCount == 1
                        ? " confirmed request"
                        : " confirmed requests")
        );

        rejectedCountLabel.setText(
                rejectedCount
                        + (rejectedCount == 1
                        ? " rejected request"
                        : " rejected requests")
        );
    }

    /**
     * Formats a booking request ID as a customer-friendly request reference.
     *
     * @param requestId the booking request ID
     * @return the formatted request reference
     */
    private String formatRequestReference(int requestId) {
        return "FJ-REQ-" + requestId;
    }

    /**
     * Formats a booking ID as a customer-friendly booking reference.
     *
     * If the booking ID is null, an empty string is returned because rejected
     * requests may not be linked to a confirmed booking.
     *
     * @param bookingId the booking ID, or null if no booking is linked
     * @return the formatted booking reference, or an empty string
     */
    private String formatBookingReference(Integer bookingId) {
        if (bookingId == null) {
            return "";
        }

        return "FJ-BKG-" + bookingId;
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
     * Formats a database date for display.
     *
     * If the date cannot be parsed, the original database value is returned so
     * the user can still see the stored information.
     *
     * @param databaseValue the date value stored in the database
     * @return the formatted date, or the original value if parsing fails
     */
    private String formatDate(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return "";
        }

        try {
            return LocalDate.parse(
                    databaseValue,
                    DATABASE_DATE_FORMAT
            ).format(DISPLAY_DATE_FORMAT);

        } catch (Exception e) {
            return databaseValue;
        }
    }

    /**
     * Formats a database date-time value for display.
     *
     * The method first tries the expected database date-time format. If that
     * fails, it tries Java's default LocalDateTime parsing. If both fail, the
     * original value is returned.
     *
     * @param databaseValue the date-time value stored in the database
     * @return the formatted date-time, or the original value if parsing fails
     */
    private String formatDateTime(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return "";
        }

        try {
            return LocalDateTime.parse(
                    databaseValue,
                    DATABASE_DATE_TIME_FORMAT
            ).format(DISPLAY_DATE_TIME_FORMAT);

        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(databaseValue)
                        .format(DISPLAY_DATE_TIME_FORMAT);

            } catch (Exception e) {
                return databaseValue;
            }
        }
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
     * Shows a message dialog to the user.
     *
     * This is used for validation errors, loading errors and general messages
     * on the booking request history screen.
     *
     * @param type    the type of alert to show
     * @param message the message to display
     */
    private void showMessage(
            Alert.AlertType type,
            String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle("Booking Request History");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}