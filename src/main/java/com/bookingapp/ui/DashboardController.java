package com.bookingapp.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.bookingapp.dao.BookingDAO;
import com.bookingapp.dao.BookingRequestDAO;
import com.bookingapp.dao.InventoryDAO;
import com.bookingapp.model.InventoryItem;
import com.bookingapp.security.UserSession;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller for the dashboard screen.
 *
 * This class manages the main navigation screen of the staff-facing desktop
 * application. It opens the different system pages, displays low-stock
 * warnings, shows incoming booking request notifications, displays bookings due
 * on the selected dashboard date and handles user logout.
 */
public class DashboardController {

    /**
     * Date format used by the booking database.
     */
    private static final DateTimeFormatter BOOKING_DATE_FORMATTER
            = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Data access object used for inventory database operations.
     */
    private final InventoryDAO inventoryDAO
            = new InventoryDAO();

    /**
     * Timeline used to refresh the incoming website request notification while
     * the dashboard is open.
     */
    private Timeline incomingRequestTimeline;

    /**
     * List view used to display inventory items that are at or below their
     * reorder level.
     */
    @FXML
    private ListView<String> lowStockListView;

    /**
     * Label used to show the number of incoming website booking requests
     * waiting for review.
     */
    @FXML
    private Label incomingRequestLabel;

    /**
     * Date picker used on the dashboard to choose which day's bookings should
     * be shown.
     */
    @FXML
    private DatePicker dashboardDatePicker;

    /**
     * List view used to display confirmed bookings due on the selected
     * dashboard date.
     */
    @FXML
    private ListView<String> dueBookingsListView;

    /**
     * Shows the keyboard shortcuts available on the dashboard screen.
     *
     * This keeps the page less cluttered while still making keyboard support
     * available to users who cannot easily use a mouse.
     */
    @FXML
    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Dashboard Keyboard Shortcuts");

        alert.setContentText(
                "Tab / Shift + Tab: Move between controls\n"
                + "Enter or Space: Select the focused button\n\n"
                + "Alt + K: Show keyboard shortcuts\n"
                + "Alt + B: Open Bookings\n"
                + "Alt + I: Open Inventory\n"
                + "Alt + A: Open Inventory Batches\n"
                + "Alt + O: Open Purchase Orders\n"
                + "Alt + W: Open Expiry and Waste\n"
                + "Alt + P: Open Products\n"
                + "Alt + D: Open Data Retention\n"
                + "Alt + E: Open Recipe Items\n"
                + "Alt + C: Open Customers\n"
                + "Alt + R: Open Booking Requests\n"
                + "Alt + H: Open Request History\n"
                + "Alt + S: Open Suppliers\n"
                + "Alt + X: Change Password\n"
                + "Alt + L: Log out"
        );

        alert.showAndWait();
    }

    /**
     * Initialises the dashboard when the FXML screen is opened.
     *
     * This loads low-stock warnings, sets the dashboard calendar to today's
     * date, loads bookings due today and starts the incoming request
     * notification refresh.
     */
    @FXML
    private void initialize() {
        loadLowStockItems();
        initialiseDashboardCalendar();
        loadIncomingRequestNotification();
        startIncomingRequestMonitor();
    }

    /**
     * Opens the Data Retention screen.
     *
     * This screen allows the business owner to review customers whose latest
     * confirmed or completed order was more than seven years ago.
     */
    @FXML
    private void openDataRetention() {
        openPage("/com/bookingapp/ui/data_retention.fxml");
    }

    /**
     * Opens the bookings screen.
     */
    @FXML
    private void openBookings() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/bookings.fxml"
        );
    }

    /**
     * Opens the inventory screen.
     */
    @FXML
    private void openInventory() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/inventory.fxml"
        );
    }

    /**
     * Opens the inventory batches screen.
     *
     * This screen is used to manage stock batches, purchase dates, expiry
     * dates, quantities and batch unit costs.
     */
    @FXML
    private void openInventoryBatches() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/inventory_batches.fxml"
        );
    }

    /**
     * Opens the purchase orders screen.
     *
     * This screen is used to record supplier orders for inventory items.
     */
    @FXML
    private void openPurchaseOrders() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/purchase_orders.fxml"
        );
    }

    /**
     * Opens the expiry and waste screen.
     *
     * This screen is used to record expired, damaged or wasted stock.
     */
    @FXML
    private void openExpiryWaste() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/expiry_waste.fxml"
        );
    }

    /**
     * Opens the products screen.
     */
    @FXML
    private void openProducts() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/products.fxml"
        );
    }

    /**
     * Opens the recipe items screen.
     *
     * This screen links products to the inventory items required to make them.
     */
    @FXML
    private void openRecipeItems() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/recipe_items.fxml"
        );
    }

    /**
     * Opens the customers screen.
     */
    @FXML
    private void openCustomers() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/customers.fxml"
        );
    }

    /**
     * Opens the website booking requests screen.
     *
     * This screen allows staff to review customer requests submitted through
     * the website.
     */
    @FXML
    private void openBookingRequests() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/booking_requests.fxml"
        );
    }

    /**
     * Opens the booking request history screen.
     *
     * This screen displays confirmed and rejected website booking requests.
     */
    @FXML
    private void openBookingRequestHistory() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/"
                + "booking_request_history.fxml"
        );
    }

    /**
     * Opens the suppliers screen.
     */
    @FXML
    private void openSuppliers() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/suppliers.fxml"
        );
    }

    /**
     * Opens the change password screen.
     */
    @FXML
    private void openChangePassword() {
        stopIncomingRequestMonitor();

        openPage(
                "/com/bookingapp/ui/change_password.fxml"
        );
    }

    /**
     * Logs the current user out of the system.
     *
     * The current user session is cleared and the login screen is opened.
     */
    @FXML
    private void logout() {
        stopIncomingRequestMonitor();

        UserSession.clear();

        openPage(
                "/com/bookingapp/ui/login.fxml"
        );
    }

    /**
     * Loads low-stock inventory items and displays them on the dashboard.
     *
     * If there are no low-stock items, a clear message is shown. If the items
     * cannot be loaded, an error message is displayed in the list view.
     */
    @FXML
    private void loadLowStockItems() {
        try {
            List<InventoryItem> lowStockItems
                    = inventoryDAO.getLowStockItems();

            List<String> displayItems
                    = lowStockItems.stream()
                            .map(item
                                    -> item.getName()
                            + " — "
                            + formatQuantity(
                                    item
                                            .getCurrentQuantity()
                            )
                            + " "
                            + item.getUnit()
                            + " remaining"
                            + " (reorder level: "
                            + formatQuantity(
                                    item
                                            .getReorderLevel()
                            )
                            + ")"
                            )
                            .toList();

            if (displayItems.isEmpty()) {
                lowStockListView.setItems(
                        FXCollections.observableArrayList(
                                "No low-stock items"
                        )
                );

            } else {
                lowStockListView.setItems(
                        FXCollections.observableArrayList(
                                displayItems
                        )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();

            lowStockListView.setItems(
                    FXCollections.observableArrayList(
                            "Unable to load low-stock items"
                    )
            );
        }
    }

    /**
     * Sets the dashboard calendar to today's date and loads today's bookings.
     */
    private void initialiseDashboardCalendar() {
        if (dashboardDatePicker == null
                || dueBookingsListView == null) {

            return;
        }

        dashboardDatePicker.setValue(
                LocalDate.now()
        );

        loadBookingsForSelectedDate();
    }

    /**
     * Loads confirmed bookings due on the date selected in the dashboard
     * calendar.
     */
    @FXML
    private void loadBookingsForSelectedDate() {
        if (dashboardDatePicker == null
                || dueBookingsListView == null) {

            return;
        }

        dueBookingsListView.getItems().clear();

        LocalDate selectedDate
                = dashboardDatePicker.getValue();

        if (selectedDate == null) {
            dueBookingsListView.setItems(
                    FXCollections.observableArrayList(
                            "Please select a date."
                    )
            );

            return;
        }

        String bookingDate
                = selectedDate.format(
                        BOOKING_DATE_FORMATTER
                );

        try {
            List<String> dueBookings
                    = BookingDAO.getDashboardBookingsByDate(
                            bookingDate
                    );

            if (dueBookings.isEmpty()) {
                dueBookingsListView.setItems(
                        FXCollections.observableArrayList(
                                "No confirmed bookings due on "
                                + bookingDate
                                + "."
                        )
                );

            } else {
                dueBookingsListView.setItems(
                        FXCollections.observableArrayList(
                                dueBookings
                        )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();

            dueBookingsListView.setItems(
                    FXCollections.observableArrayList(
                            "Unable to load bookings for "
                            + bookingDate
                            + "."
                    )
            );
        }
    }

    /**
     * Manually refreshes the incoming request notification on the dashboard.
     *
     * This method is called by the Refresh Requests button in dashboard.fxml.
     */
    @FXML
    private void refreshIncomingRequestNotification() {
        loadIncomingRequestNotification();
    }

    /**
     * Loads the number of incoming website booking requests waiting for review.
     *
     * If the notification count cannot be loaded, the dashboard still opens and
     * shows a clear message instead of preventing access to the system.
     */
    private void loadIncomingRequestNotification() {
        if (incomingRequestLabel == null) {
            return;
        }

        try {
            int pendingRequestCount
                    = BookingRequestDAO.countPendingRequests();

            if (pendingRequestCount <= 0) {
                incomingRequestLabel.setText(
                        "No new booking requests"
                );

                incomingRequestLabel.setStyle(
                        "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #1f1f1f;"
                );

            } else if (pendingRequestCount == 1) {
                incomingRequestLabel.setText(
                        "1 new booking request needs review"
                );

                incomingRequestLabel.setStyle(
                        "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #a94442;"
                );

            } else {
                incomingRequestLabel.setText(
                        pendingRequestCount
                        + " new booking requests need review"
                );

                incomingRequestLabel.setStyle(
                        "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #a94442;"
                );
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace();

            incomingRequestLabel.setText(
                    "Unable to load request notifications"
            );

            incomingRequestLabel.setStyle(
                    "-fx-font-size: 14px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #a94442;"
            );
        }
    }

    /**
     * Starts a repeated check for incoming booking requests while the dashboard
     * is open.
     */
    private void startIncomingRequestMonitor() {
        if (incomingRequestLabel == null) {
            return;
        }

        stopIncomingRequestMonitor();

        incomingRequestTimeline
                = new Timeline(
                        new KeyFrame(
                                Duration.seconds(30),
                                event -> loadIncomingRequestNotification()
                        )
                );

        incomingRequestTimeline.setCycleCount(
                Timeline.INDEFINITE
        );

        incomingRequestTimeline.play();
    }

    /**
     * Stops the incoming request notification refresh.
     *
     * This prevents the dashboard notification timer continuing after the user
     * has moved to another screen.
     */
    private void stopIncomingRequestMonitor() {
        if (incomingRequestTimeline != null) {
            incomingRequestTimeline.stop();
            incomingRequestTimeline = null;
        }
    }

    /**
     * Opens a different FXML page in the current application window.
     *
     * The existing scene is reused and only the root node is replaced. This
     * preserves the current window size, position and maximised state.
     *
     * @param fxmlPath the path of the FXML file to open
     */
    private void openPage(
            String fxmlPath
    ) {
        try {
            FXMLLoader loader
                    = new FXMLLoader(
                            getClass().getResource(
                                    fxmlPath
                            )
                    );

            Parent root = loader.load();

            Stage stage
                    = getCurrentStage();

            /*
             * Keep the existing Scene and replace only its root. This preserves
             * the window size, position and maximised state.
             */
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();

            Throwable cause = e;

            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            String errorMessage
                    = cause.getMessage() == null
                    ? "No further details are available."
                    : cause.getMessage();

            showMessage(
                    "Unable to open page.\n\n"
                    + "Page: "
                    + fxmlPath
                    + "\n\nError: "
                    + cause.getClass()
                            .getSimpleName()
                    + "\n"
                    + errorMessage
            );
        }
    }

    /**
     * Gets the current dashboard stage.
     *
     * The stage is found through the low-stock list view, which is already part
     * of the dashboard scene.
     *
     * @return the current JavaFX stage
     * @throws IllegalStateException if the dashboard stage is not available
     */
    private Stage getCurrentStage() {
        if (lowStockListView == null
                || lowStockListView.getScene() == null
                || lowStockListView
                        .getScene()
                        .getWindow() == null) {

            throw new IllegalStateException(
                    "The dashboard stage is not available."
            );
        }

        return (Stage) lowStockListView
                .getScene()
                .getWindow();
    }

    /**
     * Formats a stock quantity for display.
     *
     * Whole numbers are shown without decimal places. Other values are shown to
     * two decimal places.
     *
     * @param quantity the quantity to format
     * @return the formatted quantity
     */
    private String formatQuantity(
            double quantity
    ) {
        if (quantity == Math.rint(quantity)) {
            return String.format(
                    "%.0f",
                    quantity
            );
        }

        return String.format(
                "%.2f",
                quantity
        );
    }

    /**
     * Shows a dashboard error message to the user.
     *
     * This is used when a page cannot be opened or another dashboard action
     * fails.
     *
     * @param message the message to display
     */
    private void showMessage(
            String message
    ) {
        Alert alert
                = new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle("Dashboard");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
