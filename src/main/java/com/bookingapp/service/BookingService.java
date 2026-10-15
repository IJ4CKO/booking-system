package com.bookingapp.service;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bookingapp.dao.BookingDAO;
import com.bookingapp.dao.BookingLineDAO;
import com.bookingapp.dao.BookingRequestDAO;
import com.bookingapp.dao.CustomerDAO;
import com.bookingapp.dao.DBConnection;
import com.bookingapp.dao.InventoryBatchDAO;
import com.bookingapp.dao.InventoryDAO;
import com.bookingapp.dao.RecipeItemDAO;
import com.bookingapp.dao.StockBatchUsageDAO;
import com.bookingapp.dao.StockMovementDAO;
import com.bookingapp.model.Booking;
import com.bookingapp.model.BookingLine;
import com.bookingapp.model.BookingRequest;
import com.bookingapp.model.Customer;
import com.bookingapp.model.InventoryBatch;
import com.bookingapp.model.InventoryItem;
import com.bookingapp.model.RecipeItem;
import com.bookingapp.model.StockBatchUsage;

/**
 * Provides the main booking workflow logic for the booking and inventory system.
 *
 * This service class connects the booking, booking request, customer, recipe,
 * inventory batch, stock movement and stock batch usage parts of the system.
 * It manages booking creation, website request confirmation, FIFO stock
 * deduction, stock restoration, awaiting-stock bookings and booking
 * cancellation.
 */
public class BookingService {

    /**
     * Allows small rounding differences when comparing stock quantities.
     */
    private static final double QUANTITY_TOLERANCE = 0.000001;

    /**
     * Stores the number of days used to decide whether a booking is treated as
     * short notice.
     *
     * A booking with insufficient stock can be accepted when it is at least
     * 14 days away.
     */
    private static final int SHORT_NOTICE_DAYS = 14;

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
     * Date format used by website booking requests.
     */
    private static final DateTimeFormatter WEBSITE_DATE_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Date format used by bookings stored in the desktop application.
     */
    private static final DateTimeFormatter BOOKING_DATE_FORMAT
            = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Data access object used for booking database operations.
     */
    private final BookingDAO bookingDAO = new BookingDAO();

    /**
     * Data access object used for booking line database operations.
     */
    private final BookingLineDAO bookingLineDAO
            = new BookingLineDAO();

    /**
     * Data access object used for website booking request database operations.
     */
    private final BookingRequestDAO bookingRequestDAO
            = new BookingRequestDAO();

    /**
     * Data access object used for customer database operations.
     */
    private final CustomerDAO customerDAO
            = new CustomerDAO();

    /**
     * Data access object used for recipe item database operations.
     */
    private final RecipeItemDAO recipeItemDAO
            = new RecipeItemDAO();

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
     * Data access object used for stock batch usage database operations.
     */
    private final StockBatchUsageDAO stockBatchUsageDAO
            = new StockBatchUsageDAO();

    /**
     * Creates a booking entered directly through the desktop application.
     *
     * When enough stock is available, the booking is saved as CONFIRMED and
     * stock is deducted using FIFO. When stock is insufficient, short-notice
     * bookings are blocked and advance bookings are saved as AWAITING_STOCK.
     *
     * @param booking      the booking details entered by the user
     * @param bookingLines the product lines linked to the booking
     * @return the database ID of the created booking
     * @throws Exception if the booking is invalid, stock is insufficient for a
     *                   short-notice booking or the database operation fails
     */
    public int createConfirmedBooking(
            Booking booking,
            List<BookingLine> bookingLines
    ) throws Exception {

        if (booking == null) {
            throw new IllegalArgumentException(
                    "Booking cannot be null."
            );
        }

        LocalDate bookingDate
                = parseStoredBookingDate(
                        booking.getCollectionDate()
                );

        validateNewBookingDate(
                bookingDate
        );

        Map<Integer, Double> requiredInventory
                = calculateRequiredInventory(
                        bookingLines
                );

        StockAssessment stockAssessment
                = assessStock(
                        requiredInventory
                );

        String bookingStatus
                = determineInitialBookingStatus(
                        bookingDate,
                        stockAssessment
                );

        Booking bookingToSave
                = copyBookingWithStatus(
                        booking,
                        bookingStatus
                );

        try (Connection conn
                = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int bookingId
                        = bookingDAO.addBooking(
                                conn,
                                bookingToSave
                        );

                if (bookingLines != null
                        && !bookingLines.isEmpty()) {

                    saveBookingLines(
                            conn,
                            bookingId,
                            bookingLines
                    );

                    if (STATUS_CONFIRMED.equals(
                            bookingStatus
                    )) {
                        deductRequiredInventory(
                                conn,
                                bookingId,
                                requiredInventory,
                                stockAssessment
                                        .getAvailableBatchesByItem(),
                                "Stock deducted for "
                                + "confirmed booking"
                        );
                    }
                }

                conn.commit();

                return bookingId;

            } catch (Exception e) {
                rollbackTransaction(
                        conn,
                        e
                );

                throw e;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Validates a website booking request before a deposit is requested.
     *
     * This prevents the system from asking the customer to pay a deposit for a
     * short-notice booking that cannot be fulfilled because of insufficient
     * inventory. For bookings at least 14 days away, the request may continue
     * to the deposit stage because stock can still be ordered or received
     * before the event.
     *
     * @param request the website booking request to validate
     * @throws Exception if the request is invalid, the product cannot be matched
     *                   or stock is insufficient for a short-notice request
     */
    public void validateWebsiteRequestBeforeDeposit(
            BookingRequest request
    ) throws Exception {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Booking request cannot be null."
            );
        }

        LocalDate eventDate;

        try {
            eventDate
                    = LocalDate.parse(
                            request.getEventDate(),
                            WEBSITE_DATE_FORMAT
                    );

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The request contains an invalid event date."
            );
        }

        validateNewBookingDate(
                eventDate
        );

        List<BookingLine> bookingLines;
        List<String> unmatchedProductNames;

        try (Connection readConnection
                = DBConnection.getConnection()) {

            unmatchedProductNames
                    = bookingRequestDAO
                            .getUnmatchedProductNames(
                                    readConnection,
                                    request.getRequestId()
                            );

            bookingLines
                    = bookingRequestDAO
                            .getBookingLinesForRequest(
                                    readConnection,
                                    request.getRequestId()
                            );
        }

        if (!unmatchedProductNames.isEmpty()) {
            throw new IllegalStateException(
                    "The following requested products do not "
                    + "match products in the desktop "
                    + "system: "
                    + String.join(
                            ", ",
                            unmatchedProductNames
                    )
                    + ". Ensure that the website and "
                    + "desktop product names match."
            );
        }

        if (bookingLines == null || bookingLines.isEmpty()) {
            throw new IllegalStateException(
                    "This request does not contain any valid "
                    + "products."
            );
        }

        Map<Integer, Double> requiredInventory
                = calculateRequiredInventory(
                        bookingLines
                );

        if (requiredInventory.isEmpty()) {
            throw new IllegalStateException(
                    "No recipe items were found for the selected "
                    + "product or products. Add recipe items "
                    + "before requesting a deposit."
            );
        }

        StockAssessment stockAssessment
                = assessStock(
                        requiredInventory
                );

        long daysUntilBooking
                = ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        eventDate
                );

        boolean isShortNoticeBooking
                = daysUntilBooking < SHORT_NOTICE_DAYS;

        if (isShortNoticeBooking
                && !stockAssessment.hasSufficientStock()) {

            throw new IllegalStateException(
                    "This request cannot be moved to deposit yet. "
                    + "It is a short-notice booking because it "
                    + "is only "
                    + daysUntilBooking
                    + (daysUntilBooking == 1
                            ? " day"
                            : " days")
                    + " away. Stock must be available before "
                    + "a deposit is requested.\n\n"
                    + stockAssessment.getShortfallMessage()
            );
        }
    }

    /**
     * Creates a booking from a paid website request.
     *
     * The request claim, customer creation, booking creation, booking lines,
     * optional FIFO stock deduction and request link are completed in one
     * database transaction.
     *
     * @param request     the paid website booking request
     * @param ownerUserId the owner user ID linked to the confirmed booking
     * @return the database ID of the created booking
     * @throws Exception if the request cannot be confirmed or the database
     *                   transaction fails
     */
    public int createConfirmedBookingFromRequest(
            BookingRequest request,
            int ownerUserId
    ) throws Exception {

        validateRequestForConfirmation(
                request,
                ownerUserId
        );

        LocalDate eventDate;

        try {
            eventDate
                    = LocalDate.parse(
                            request.getEventDate(),
                            WEBSITE_DATE_FORMAT
                    );

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The request contains an invalid "
                    + "event date."
            );
        }

        validateNewBookingDate(
                eventDate
        );

        String bookingDate
                = eventDate.format(
                        BOOKING_DATE_FORMAT
                );

        List<BookingLine> bookingLines;
        List<String> unmatchedProductNames;

        /*
         * Validate product matching before making any database changes.
         */
        try (Connection readConnection
                = DBConnection.getConnection()) {

            unmatchedProductNames
                    = bookingRequestDAO
                            .getUnmatchedProductNames(
                                    readConnection,
                                    request.getRequestId()
                            );

            bookingLines
                    = bookingRequestDAO
                            .getBookingLinesForRequest(
                                    readConnection,
                                    request.getRequestId()
                            );
        }

        if (!unmatchedProductNames.isEmpty()) {
            throw new IllegalStateException(
                    "The following requested products do not "
                    + "match products in the desktop "
                    + "system: "
                    + String.join(
                            ", ",
                            unmatchedProductNames
                    )
                    + ". Ensure that the website and "
                    + "desktop product names match."
            );
        }

        Customer existingCustomer
                = customerDAO.getCustomerByDetails(
                        request.getFullName(),
                        request.getPhone(),
                        request.getEmail()
                );

        if (existingCustomer != null) {
            validateNoDuplicateBookings(
                    existingCustomer.getCustomerId(),
                    bookingDate,
                    request.getEventTime(),
                    bookingLines
            );
        }

        Map<Integer, Double> requiredInventory
                = calculateRequiredInventory(
                        bookingLines
                );

        StockAssessment stockAssessment
                = assessStock(
                        requiredInventory
                );

        String bookingStatus
                = determineInitialBookingStatus(
                        eventDate,
                        stockAssessment
                );

        String fulfilmentType
                = safeText(
                        request.getCollectionOrDelivery()
                );

        boolean isDelivery
                = "DELIVERY".equalsIgnoreCase(
                        fulfilmentType
                );

        if (isDelivery
                && safeText(
                        request.getDropoffAddress()
                ).isBlank()) {

            throw new IllegalStateException(
                    "A delivery address is required before "
                    + "this request can be confirmed."
            );
        }

        String itemsRequiredText
                = buildItemsRequiredText(
                        request
                );

        Booking booking
                = new Booking(
                        0,
                        0,
                        ownerUserId,
                        bookingDate,
                        request.getEventTime(),
                        isDelivery
                                ? bookingDate
                                : null,
                        isDelivery
                                ? request.getEventTime()
                                : null,
                        isDelivery
                                ? request.getDropoffAddress()
                                : null,
                        itemsRequiredText,
                        bookingStatus
                );

        try (Connection conn
                = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                boolean claimed
                        = bookingRequestDAO
                                .claimForConfirmation(
                                        conn,
                                        request.getRequestId()
                                );

                if (!claimed) {
                    throw new IllegalStateException(
                            "The request could not be "
                            + "confirmed. It may already "
                            + "have been confirmed, or its "
                            + "deposit is not recorded "
                            + "as paid."
                    );
                }

                Customer transactionCustomer
                        = customerDAO
                                .getCustomerByDetails(
                                        conn,
                                        request.getFullName(),
                                        request.getPhone(),
                                        request.getEmail()
                                );

                int customerId;

                if (transactionCustomer != null) {
                    customerId
                            = transactionCustomer
                                    .getCustomerId();

                } else {
                    Customer newCustomer
                            = new Customer(
                                    0,
                                    request.getFullName(),
                                    request.getPhone(),
                                    request.getEmail()
                            );

                    customerId
                            = customerDAO
                                    .addCustomerAndReturnId(
                                            conn,
                                            newCustomer
                                    );
                }

                booking.setCustomerId(
                        customerId
                );

                int bookingId
                        = bookingDAO.addBooking(
                                conn,
                                booking
                        );

                if (bookingLines != null
                        && !bookingLines.isEmpty()) {

                    saveBookingLines(
                            conn,
                            bookingId,
                            bookingLines
                    );

                    if (STATUS_CONFIRMED.equals(
                            bookingStatus
                    )) {
                        deductRequiredInventory(
                                conn,
                                bookingId,
                                requiredInventory,
                                stockAssessment
                                        .getAvailableBatchesByItem(),
                                "Stock deducted for confirmed "
                                + "website booking request "
                                + request.getRequestId()
                        );
                    }
                }

                /*
                 * The request has now been processed and linked to the booking.
                 * The linked booking may be CONFIRMED or AWAITING_STOCK.
                 */
                bookingRequestDAO
                        .completeConfirmation(
                                conn,
                                request.getRequestId(),
                                bookingId
                        );

                conn.commit();

                request.setBookingId(
                        bookingId
                );

                request.setStatus(
                        STATUS_CONFIRMED
                );

                return bookingId;

            } catch (Exception e) {
                rollbackTransaction(
                        conn,
                        e
                );

                throw e;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Rechecks an AWAITING_STOCK booking after new inventory has been received.
     *
     * When sufficient stock is available, the stock is deducted using FIFO and
     * the booking becomes CONFIRMED.
     *
     * @param bookingId the booking ID to recheck and confirm
     * @throws Exception if the booking is not awaiting stock, stock is still
     *                   insufficient or the database operation fails
     */
    public void allocateStockAndConfirm(
            int bookingId
    ) throws Exception {

        Booking booking
                = bookingDAO.getBookingById(
                        bookingId
                );

        if (booking == null) {
            throw new IllegalStateException(
                    "Booking not found."
            );
        }

        if (STATUS_CONFIRMED.equalsIgnoreCase(
                booking.getStatus()
        )) {
            throw new IllegalStateException(
                    "This booking is already confirmed."
            );
        }

        if (STATUS_CANCELLED.equalsIgnoreCase(
                booking.getStatus()
        )) {
            throw new IllegalStateException(
                    "A cancelled booking cannot be confirmed."
            );
        }

        if (!STATUS_AWAITING_STOCK.equalsIgnoreCase(
                booking.getStatus()
        )) {
            throw new IllegalStateException(
                    "Only a booking awaiting stock can use "
                    + "this action."
            );
        }

        List<BookingLine> bookingLines
                = bookingLineDAO
                        .getBookingLinesByBookingId(
                                bookingId
                        );

        Map<Integer, Double> requiredInventory
                = calculateRequiredInventory(
                        bookingLines
                );

        StockAssessment stockAssessment
                = assessStock(
                        requiredInventory
                );

        if (!stockAssessment.hasSufficientStock()) {
            throw new IllegalStateException(
                    "The booking still cannot be confirmed "
                    + "because sufficient stock has not "
                    + "been received.\n\n"
                    + stockAssessment
                            .getShortfallMessage()
            );
        }

        try (Connection conn
                = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                if (!requiredInventory.isEmpty()) {
                    deductRequiredInventory(
                            conn,
                            bookingId,
                            requiredInventory,
                            stockAssessment
                                    .getAvailableBatchesByItem(),
                            "Stock allocated when an advance "
                            + "booking was confirmed"
                    );
                }

                bookingDAO.updateBookingStatus(
                        conn,
                        bookingId,
                        STATUS_CONFIRMED
                );

                conn.commit();

            } catch (Exception e) {
                rollbackTransaction(
                        conn,
                        e
                );

                throw e;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Returns a readable stock-shortfall summary for a booking.
     *
     * @param bookingId the booking ID to check
     * @return a stock shortfall message, or a message confirming sufficient stock
     * @throws Exception if the booking cannot be found or the stock check fails
     */
    public String getStockShortfallSummary(
            int bookingId
    ) throws Exception {

        Booking booking
                = bookingDAO.getBookingById(
                        bookingId
                );

        if (booking == null) {
            throw new IllegalStateException(
                    "Booking not found."
            );
        }

        List<BookingLine> bookingLines
                = bookingLineDAO
                        .getBookingLinesByBookingId(
                                bookingId
                        );

        Map<Integer, Double> requiredInventory
                = calculateRequiredInventory(
                        bookingLines
                );

        StockAssessment stockAssessment
                = assessStock(
                        requiredInventory
                );

        if (stockAssessment.hasSufficientStock()) {
            return "Sufficient stock is currently available.";
        }

        return stockAssessment.getShortfallMessage();
    }

    /**
     * Cancels a booking.
     *
     * Confirmed bookings restore the exact FIFO batches that were originally
     * used. AWAITING_STOCK bookings do not restore stock because no inventory
     * was deducted when they were created.
     *
     * @param bookingId the booking ID to cancel
     * @throws Exception if the booking cannot be found, is already cancelled or
     *                   the database transaction fails
     */
    public void cancelBooking(
            int bookingId
    ) throws Exception {

        Booking booking
                = bookingDAO.getBookingById(
                        bookingId
                );

        if (booking == null) {
            throw new Exception(
                    "Booking not found."
            );
        }

        if (STATUS_CANCELLED.equalsIgnoreCase(
                booking.getStatus()
        )) {
            throw new Exception(
                    "Booking is already cancelled."
            );
        }

        /*
         * No inventory has been deducted for this status.
         */
        if (STATUS_AWAITING_STOCK.equalsIgnoreCase(
                booking.getStatus()
        )) {
            try (Connection conn
                    = DBConnection.getConnection()) {

                conn.setAutoCommit(false);

                try {
                    bookingDAO.updateBookingStatus(
                            conn,
                            bookingId,
                            STATUS_CANCELLED
                    );

                    conn.commit();

                    return;

                } catch (Exception e) {
                    rollbackTransaction(
                            conn,
                            e
                    );

                    throw e;

                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }

        List<StockBatchUsage> batchUsages
                = stockBatchUsageDAO
                        .getBatchUsageForBooking(
                                bookingId
                        );

        Map<Integer, Double> inventoryToRestore;

        if (batchUsages != null
                && !batchUsages.isEmpty()) {

            inventoryToRestore
                    = calculateInventoryFromBatchUsage(
                            batchUsages
                    );

        } else {
            /*
             * Legacy fallback for confirmed bookings created before FIFO usage
             * records were available.
             */
            List<BookingLine> bookingLines
                    = bookingLineDAO
                            .getBookingLinesByBookingId(
                                    bookingId
                            );

            inventoryToRestore
                    = calculateRequiredInventory(
                            bookingLines
                    );
        }

        try (Connection conn
                = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                if (batchUsages != null
                        && !batchUsages.isEmpty()) {

                    restoreInventoryBatches(
                            conn,
                            batchUsages
                    );
                }

                for (Map.Entry<Integer, Double> entry
                        : inventoryToRestore.entrySet()) {

                    int inventoryItemId
                            = entry.getKey();

                    double quantityToRestore
                            = entry.getValue();

                    if (quantityToRestore
                            <= QUANTITY_TOLERANCE) {

                        continue;
                    }

                    inventoryDAO
                            .updateInventoryQuantity(
                                    conn,
                                    inventoryItemId,
                                    quantityToRestore
                            );

                    stockMovementDAO
                            .addStockMovement(
                                    conn,
                                    inventoryItemId,
                                    quantityToRestore,
                                    bookingId,
                                    "CANCELLATION_RETURN",
                                    "Stock and FIFO batch "
                                    + "quantities returned "
                                    + "after booking "
                                    + "cancellation"
                            );
                }

                bookingDAO.updateBookingStatus(
                        conn,
                        bookingId,
                        STATUS_CANCELLED
                );

                conn.commit();

            } catch (Exception e) {
                rollbackTransaction(
                        conn,
                        e
                );

                throw e;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Deletes a booking only after it has been cancelled.
     *
     * @param bookingId the cancelled booking ID to delete
     * @throws Exception if the booking cannot be found, has not been cancelled
     *                   or the database transaction fails
     */
    public void deleteCancelledBooking(
            int bookingId
    ) throws Exception {

        Booking booking
                = bookingDAO.getBookingById(
                        bookingId
                );

        if (booking == null) {
            throw new Exception(
                    "Booking not found."
            );
        }

        if (!STATUS_CANCELLED.equalsIgnoreCase(
                booking.getStatus()
        )) {
            throw new Exception(
                    "Only cancelled bookings can be deleted."
            );
        }

        try (Connection conn
                = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                bookingLineDAO
                        .deleteBookingLinesByBookingId(
                                conn,
                                bookingId
                        );

                bookingDAO.deleteBooking(
                        conn,
                        bookingId
                );

                conn.commit();

            } catch (Exception e) {
                rollbackTransaction(
                        conn,
                        e
                );

                throw e;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Saves all product lines belonging to a booking.
     *
     * Each booking line is linked to the booking ID before it is saved.
     *
     * @param conn         the active database connection
     * @param bookingId    the booking ID to link to each line
     * @param bookingLines the booking lines to save
     * @throws Exception if a line quantity is invalid or saving fails
     */
    private void saveBookingLines(
            Connection conn,
            int bookingId,
            List<BookingLine> bookingLines
    ) throws Exception {

        for (BookingLine line : bookingLines) {

            if (line == null) {
                continue;
            }

            if (line.getQuantity() <= 0) {
                throw new IllegalStateException(
                        "Booking line quantity must be "
                        + "greater than zero."
                );
            }

            line.setBookingId(
                    bookingId
            );

            bookingLineDAO.addBookingLine(
                    conn,
                    line
            );
        }
    }

    /**
     * Deducts required stock and records stock movements.
     *
     * The overall inventory quantity is reduced, a stock movement is recorded
     * and inventory batches are consumed using FIFO.
     *
     * @param conn                   the active database connection
     * @param bookingId              the booking ID linked to the stock use
     * @param requiredInventory      the inventory quantities required
     * @param availableBatchesByItem the available batches grouped by inventory item
     * @param movementNotes          the notes to store against the stock movement
     * @throws Exception if stock deduction, movement recording or FIFO use fails
     */
    private void deductRequiredInventory(
            Connection conn,
            int bookingId,
            Map<Integer, Double> requiredInventory,
            Map<Integer, List<InventoryBatch>> availableBatchesByItem,
            String movementNotes
    ) throws Exception {

        for (Map.Entry<Integer, Double> entry
                : requiredInventory.entrySet()) {

            int inventoryItemId
                    = entry.getKey();

            double totalRequired
                    = entry.getValue();

            if (totalRequired
                    <= QUANTITY_TOLERANCE) {

                continue;
            }

            inventoryDAO
                    .updateInventoryQuantity(
                            conn,
                            inventoryItemId,
                            -totalRequired
                    );

            stockMovementDAO
                    .addStockMovement(
                            conn,
                            inventoryItemId,
                            -totalRequired,
                            bookingId,
                            "BOOKING_USE",
                            movementNotes
                    );

            consumeInventoryUsingFIFO(
                    conn,
                    bookingId,
                    inventoryItemId,
                    totalRequired,
                    availableBatchesByItem.get(
                            inventoryItemId
                    )
            );
        }
    }

    /**
     * Calculates inventory requirements from booking products and recipe
     * definitions.
     *
     * Each booking line is matched with recipe items for the selected product.
     * The required inventory quantity is calculated by multiplying the booking
     * quantity by the recipe quantity required per product unit.
     *
     * @param bookingLines the booking lines to calculate stock requirements for
     * @return a map of inventory item IDs and required quantities
     * @throws Exception if recipe data cannot be loaded or a booking line is invalid
     */
    private Map<Integer, Double>
            calculateRequiredInventory(
                    List<BookingLine> bookingLines
            ) throws Exception {

        Map<Integer, Double> requiredInventory
                = new HashMap<>();

        if (bookingLines == null
                || bookingLines.isEmpty()) {

            return requiredInventory;
        }

        List<RecipeItem> allRecipeItems
                = recipeItemDAO.getAllRecipeItems();

        for (BookingLine line : bookingLines) {

            if (line == null) {
                continue;
            }

            if (line.getQuantity() <= 0) {
                throw new IllegalStateException(
                        "Booking line quantity must be "
                        + "greater than zero."
                );
            }

            for (RecipeItem recipeItem
                    : allRecipeItems) {

                if (recipeItem.getProductId()
                        == line.getProductId()) {

                    double totalRequired
                            = line.getQuantity()
                            * recipeItem
                                    .getQuantityRequiredPerUnit();

                    requiredInventory.merge(
                            recipeItem
                                    .getInventoryItemId(),
                            totalRequired,
                            Double::sum
                    );
                }
            }
        }

        return requiredInventory;
    }

    /**
     * Calculates inventory totals from recorded FIFO batch usage.
     *
     * This is used when cancelling a confirmed booking so that the exact
     * inventory quantities originally deducted can be restored.
     *
     * @param batchUsages the recorded FIFO batch usage records
     * @return a map of inventory item IDs and quantities to restore
     */
    private Map<Integer, Double>
            calculateInventoryFromBatchUsage(
                    List<StockBatchUsage> batchUsages
            ) {

        Map<Integer, Double> inventoryToRestore
                = new HashMap<>();

        for (StockBatchUsage usage : batchUsages) {

            if (usage == null) {
                continue;
            }

            if (usage.getQuantityUsed()
                    <= QUANTITY_TOLERANCE) {

                throw new IllegalStateException(
                        "Invalid stock batch usage "
                        + "quantity for batch ID "
                        + usage.getBatchId()
                );
            }

            inventoryToRestore.merge(
                    usage.getInventoryItemId(),
                    usage.getQuantityUsed(),
                    Double::sum
            );
        }

        return inventoryToRestore;
    }

    /**
     * Checks current overall stock and available FIFO batches.
     *
     * Stock is treated as usable only when it exists in both the overall
     * inventory total and the available inventory batches.
     *
     * @param requiredInventory the inventory quantities required
     * @return the result of the stock check
     * @throws Exception if stock or batch information cannot be loaded
     */
    private StockAssessment assessStock(
            Map<Integer, Double> requiredInventory
    ) throws Exception {

        Map<Integer, List<InventoryBatch>> availableBatchesByItem
                = loadAvailableBatches(
                        requiredInventory
                );

        boolean sufficientStock = true;

        StringBuilder shortfallMessage
                = new StringBuilder();

        for (Map.Entry<Integer, Double> entry
                : requiredInventory.entrySet()) {

            int inventoryItemId
                    = entry.getKey();

            double requiredQuantity
                    = entry.getValue();

            if (requiredQuantity
                    <= QUANTITY_TOLERANCE) {

                continue;
            }

            double overallQuantity
                    = inventoryDAO.getCurrentQuantity(
                            inventoryItemId
                    );

            List<InventoryBatch> batches
                    = availableBatchesByItem.get(
                            inventoryItemId
                    );

            double availableBatchQuantity = 0.0;

            if (batches != null) {
                for (InventoryBatch batch : batches) {

                    if (batch != null) {
                        availableBatchQuantity
                                += batch.getQuantityRemaining();
                    }
                }
            }

            /*
             * Stock is only usable when it exists in both the overall inventory
             * total and the usable batches.
             */
            double usableQuantity
                    = Math.max(
                            0.0,
                            Math.min(
                                    overallQuantity,
                                    availableBatchQuantity
                            )
                    );

            if (usableQuantity
                    + QUANTITY_TOLERANCE
                    >= requiredQuantity) {

                continue;
            }

            sufficientStock = false;

            double shortage
                    = Math.max(
                            0.0,
                            requiredQuantity
                            - usableQuantity
                    );

            if (shortfallMessage.length() > 0) {
                shortfallMessage.append("\n");
            }

            InventoryItem inventoryItem
                    = inventoryDAO.getInventoryItemById(
                            inventoryItemId
                    );

            String itemName;

            String itemUnit;

            if (inventoryItem == null) {
                itemName
                        = "Inventory item ID "
                        + inventoryItemId;

                itemUnit = "";

            } else {
                itemName
                        = inventoryItem.getName();

                itemUnit
                        = inventoryItem.getUnit();
            }

            String unitText
                    = itemUnit == null
                    || itemUnit.isBlank()
                    ? ""
                    : " " + itemUnit;

            shortfallMessage
                    .append(itemName)
                    .append(": required ")
                    .append(
                            formatStockQuantity(
                                    requiredQuantity
                            )
                    )
                    .append(unitText)
                    .append(", currently usable ")
                    .append(
                            formatStockQuantity(
                                    usableQuantity
                            )
                    )
                    .append(unitText)
                    .append(", short by ")
                    .append(
                            formatStockQuantity(
                                    shortage
                            )
                    )
                    .append(unitText)
                    .append(". This stock must be ordered "
                            + "and received before the booking "
                            + "can be fully confirmed.");
        }

        return new StockAssessment(
                sufficientStock,
                availableBatchesByItem,
                shortfallMessage.toString()
        );
    }

    /**
     * Loads available FIFO batches for every required item.
     *
     * @param requiredInventory the inventory item IDs that need available batches
     * @return available batches grouped by inventory item ID
     * @throws Exception if batch information cannot be loaded
     */
    private Map<Integer, List<InventoryBatch>>
            loadAvailableBatches(
                    Map<Integer, Double> requiredInventory
            ) throws Exception {

        Map<Integer, List<InventoryBatch>> batchesByInventoryItem
                = new HashMap<>();

        for (Integer inventoryItemId
                : requiredInventory.keySet()) {

            batchesByInventoryItem.put(
                    inventoryItemId,
                    inventoryBatchDAO
                            .getAvailableBatchesForInventoryItem(
                                    inventoryItemId
                            )
            );
        }

        return batchesByInventoryItem;
    }

    /**
     * Deducts inventory from the oldest usable batches first.
     *
     * This supports FIFO stock management by reducing the quantity remaining
     * in older batches before newer batches are used.
     *
     * @param conn             the active database connection
     * @param bookingId        the booking ID linked to the stock use
     * @param inventoryItemId  the inventory item being deducted
     * @param quantityRequired the quantity that needs to be deducted
     * @param availableBatches the available batches for the inventory item
     * @throws Exception if FIFO deduction cannot be completed
     */
    private void consumeInventoryUsingFIFO(
            Connection conn,
            int bookingId,
            int inventoryItemId,
            double quantityRequired,
            List<InventoryBatch> availableBatches
    ) throws Exception {

        List<InventoryBatch> safeAvailableBatches
                = availableBatches == null
                        ? List.of()
                        : availableBatches;

        double remainingToUse
                = quantityRequired;

        for (InventoryBatch batch
                : safeAvailableBatches) {

            if (remainingToUse
                    <= QUANTITY_TOLERANCE) {

                break;
            }

            double batchAvailable
                    = batch.getQuantityRemaining();

            if (batchAvailable
                    <= QUANTITY_TOLERANCE) {

                continue;
            }

            double quantityUsedFromBatch
                    = Math.min(
                            batchAvailable,
                            remainingToUse
                    );

            double newBatchQuantity
                    = batchAvailable
                    - quantityUsedFromBatch;

            if (newBatchQuantity
                    < QUANTITY_TOLERANCE) {

                newBatchQuantity = 0.0;
            }

            inventoryBatchDAO
                    .updateBatchQuantityRemaining(
                            conn,
                            batch.getBatchId(),
                            newBatchQuantity
                    );

            StockBatchUsage usage
                    = new StockBatchUsage(
                            0,
                            batch.getBatchId(),
                            bookingId,
                            inventoryItemId,
                            quantityUsedFromBatch,
                            batch.getUnitCost(),
                            LocalDateTime.now()
                                    .toString()
                    );

            stockBatchUsageDAO.addBatchUsage(
                    conn,
                    usage
            );

            remainingToUse
                    -= quantityUsedFromBatch;
        }

        if (remainingToUse
                > QUANTITY_TOLERANCE) {

            throw new IllegalStateException(
                    "Unable to complete FIFO stock "
                    + "deduction for inventory item ID "
                    + inventoryItemId
                    + ". Quantity still required: "
                    + formatStockQuantity(
                            remainingToUse
                    )
            );
        }
    }

    /**
     * Restores the exact FIFO batches originally used.
     *
     * This is used when a confirmed booking is cancelled and stock needs to be
     * returned to the same batches that were previously deducted.
     *
     * @param conn        the active database connection
     * @param batchUsages the FIFO usage records linked to the booking
     * @throws Exception if the batch records cannot be restored
     */
    private void restoreInventoryBatches(
            Connection conn,
            List<StockBatchUsage> batchUsages
    ) throws Exception {

        Map<Integer, Double> quantityByBatch
                = new LinkedHashMap<>();

        Map<Integer, Integer> inventoryItemByBatch
                = new HashMap<>();

        for (StockBatchUsage usage : batchUsages) {

            if (usage == null
                    || usage.getQuantityUsed()
                    <= QUANTITY_TOLERANCE) {

                continue;
            }

            quantityByBatch.merge(
                    usage.getBatchId(),
                    usage.getQuantityUsed(),
                    Double::sum
            );

            Integer previousInventoryItemId
                    = inventoryItemByBatch.putIfAbsent(
                            usage.getBatchId(),
                            usage.getInventoryItemId()
                    );

            if (previousInventoryItemId != null
                    && previousInventoryItemId
                    != usage.getInventoryItemId()) {

                throw new IllegalStateException(
                        "Batch ID "
                        + usage.getBatchId()
                        + " is linked to more than "
                        + "one inventory item."
                );
            }
        }

        for (Map.Entry<Integer, Double> entry
                : quantityByBatch.entrySet()) {

            int batchId
                    = entry.getKey();

            double quantityToRestore
                    = entry.getValue();

            InventoryBatch batch
                    = inventoryBatchDAO.getBatchById(
                            batchId
                    );

            if (batch == null) {
                throw new IllegalStateException(
                        "Unable to restore stock because "
                        + "batch ID "
                        + batchId
                        + " could not be found."
                );
            }

            Integer expectedInventoryItemId
                    = inventoryItemByBatch.get(
                            batchId
                    );

            if (expectedInventoryItemId != null
                    && batch.getInventoryItemId()
                    != expectedInventoryItemId) {

                throw new IllegalStateException(
                        "Inventory batch "
                        + batchId
                        + " does not match inventory "
                        + "item ID "
                        + expectedInventoryItemId
                );
            }

            double restoredBatchQuantity
                    = batch.getQuantityRemaining()
                    + quantityToRestore;

            inventoryBatchDAO
                    .updateBatchQuantityRemaining(
                            conn,
                            batchId,
                            restoredBatchQuantity
                    );
        }
    }

    /**
     * Determines whether a booking is confirmed immediately or saved as
     * awaiting stock.
     *
     * If stock is sufficient, the booking is confirmed. If stock is not
     * sufficient and the booking is short notice, an exception is thrown. If the
     * booking is far enough in the future, it is saved as AWAITING_STOCK.
     *
     * @param bookingDate     the date of the booking
     * @param stockAssessment the result of the stock check
     * @return CONFIRMED or AWAITING_STOCK
     */
    private String determineInitialBookingStatus(
            LocalDate bookingDate,
            StockAssessment stockAssessment
    ) {

        if (stockAssessment.hasSufficientStock()) {
            return STATUS_CONFIRMED;
        }

        long daysUntilBooking
                = ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        bookingDate
                );

        if (daysUntilBooking
                < SHORT_NOTICE_DAYS) {

            throw new IllegalStateException(
                    "This is a short-notice booking because it "
                    + "is only "
                    + daysUntilBooking
                    + (daysUntilBooking == 1
                            ? " day"
                            : " days")
                    + " away. At least "
                    + SHORT_NOTICE_DAYS
                    + " days are required to accept a "
                    + "booking with insufficient stock."
                    + "\n\n"
                    + stockAssessment
                            .getShortfallMessage()
            );
        }

        return STATUS_AWAITING_STOCK;
    }

    /**
     * Parses a desktop booking date stored as dd/MM/yyyy.
     *
     * @param storedDate the stored booking date text
     * @return the parsed booking date
     */
    private LocalDate parseStoredBookingDate(
            String storedDate
    ) {

        if (storedDate == null
                || storedDate.isBlank()) {

            throw new IllegalArgumentException(
                    "A booking date is required."
            );
        }

        try {
            return LocalDate.parse(
                    storedDate.trim(),
                    BOOKING_DATE_FORMAT
            );

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The booking date is invalid."
            );
        }
    }

    /**
     * Validates a date when a new booking is created.
     *
     * The date must exist and cannot be in the past.
     *
     * @param bookingDate the booking date to validate
     */
    private void validateNewBookingDate(
            LocalDate bookingDate
    ) {

        if (bookingDate == null) {
            throw new IllegalArgumentException(
                    "A booking date is required."
            );
        }

        if (bookingDate.isBefore(
                LocalDate.now()
        )) {
            throw new IllegalArgumentException(
                    "The booking date cannot be in the past."
            );
        }
    }

    /**
     * Copies a booking while replacing its status.
     *
     * This avoids changing the original booking object directly before the
     * status decision has been made.
     *
     * @param booking the original booking to copy
     * @param status  the status to store in the copied booking
     * @return a new booking object with the supplied status
     */
    private Booking copyBookingWithStatus(
            Booking booking,
            String status
    ) {

        return new Booking(
                0,
                booking.getCustomerId(),
                booking.getOwnerUserId(),
                booking.getCollectionDate(),
                booking.getCollectionTime(),
                booking.getDropoffDate(),
                booking.getDropoffTime(),
                booking.getDropoffAddress(),
                booking.getItemsRequired(),
                status
        );
    }

    /**
     * Validates that a website booking request is ready to be confirmed.
     *
     * The request must be marked as READY_TO_CONFIRM, its deposit must be PAID,
     * it must not already be linked to a booking, and the required customer,
     * date, time and payment information must be present.
     *
     * @param request     the booking request to validate
     * @param ownerUserId the owner user ID confirming the request
     */
    private void validateRequestForConfirmation(
            BookingRequest request,
            int ownerUserId
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "A booking request is required."
            );
        }

        if (ownerUserId <= 0) {
            throw new IllegalArgumentException(
                    "A valid owner user is required."
            );
        }

        if (!"READY_TO_CONFIRM".equalsIgnoreCase(
                safeText(
                        request.getStatus()
                )
        )) {
            throw new IllegalStateException(
                    "Only a request ready to confirm "
                    + "can be confirmed."
            );
        }

        if (!"PAID".equalsIgnoreCase(
                safeText(
                        request.getDepositStatus()
                )
        )) {
            throw new IllegalStateException(
                    "The deposit must be recorded as paid "
                    + "before the booking is confirmed."
            );
        }

        if (request.getBookingId() != null) {
            throw new IllegalStateException(
                    "This request is already linked to "
                    + "booking ID "
                    + request.getBookingId()
                    + "."
            );
        }

        if (request.getOrderTotal() == null) {
            throw new IllegalStateException(
                    "The order total is missing."
            );
        }

        if (request.getRequiredDeposit() == null) {
            throw new IllegalStateException(
                    "The required deposit is missing."
            );
        }

        if (safeText(
                request.getFullName()
        ).isBlank()) {

            throw new IllegalStateException(
                    "The customer name is missing."
            );
        }

        if (safeText(
                request.getEmail()
        ).isBlank()) {

            throw new IllegalStateException(
                    "The customer email address is missing."
            );
        }

        if (safeText(
                request.getEventDate()
        ).isBlank()) {

            throw new IllegalStateException(
                    "The event date is missing."
            );
        }

        if (safeText(
                request.getEventTime()
        ).isBlank()) {

            throw new IllegalStateException(
                    "The event time is missing."
            );
        }
    }

    /**
     * Checks that a customer does not already have an active booking for the
     * same date, time and product.
     *
     * Each product ID is checked once to avoid repeated duplicate checks for the
     * same product.
     *
     * @param customerId   the customer ID to check
     * @param bookingDate  the booking date
     * @param bookingTime  the booking time
     * @param bookingLines the product lines from the booking request
     * @throws Exception if a duplicate active booking is found or the database
     *                   check fails
     */
    private void validateNoDuplicateBookings(
            int customerId,
            String bookingDate,
            String bookingTime,
            List<BookingLine> bookingLines
    ) throws Exception {

        if (bookingLines == null
                || bookingLines.isEmpty()) {

            return;
        }

        Set<Integer> checkedProductIds
                = new HashSet<>();

        for (BookingLine line : bookingLines) {

            if (line == null
                    || !checkedProductIds.add(
                            line.getProductId()
                    )) {

                continue;
            }

            boolean duplicate
                    = bookingDAO.bookingAlreadyExists(
                            customerId,
                            bookingDate,
                            bookingTime,
                            line.getProductId()
                    );

            if (duplicate) {
                throw new IllegalStateException(
                        "An active booking already exists "
                        + "for this customer, date, "
                        + "time and product ID "
                        + line.getProductId()
                        + "."
                );
            }
        }
    }

    /**
     * Builds the text describing the items required for a booking.
     *
     * Website request service information and extra details are combined so
     * they can be saved into the booking record.
     *
     * @param request the website booking request
     * @return the item details text to store against the booking
     */
    private String buildItemsRequiredText(
            BookingRequest request
    ) {

        String services
                = safeText(
                        request
                                .getRequestedServicesSummary()
                );

        String details
                = safeText(
                        request.getDetails()
                );

        if (!services.isBlank()
                && !details.isBlank()) {

            return services
                    + " | "
                    + details;
        }

        if (!services.isBlank()) {
            return services;
        }

        return details;
    }

    /**
     * Formats a stock quantity for display in messages.
     *
     * Whole numbers are displayed without decimal places. Other values are
     * displayed to two decimal places.
     *
     * @param quantity the stock quantity to format
     * @return the formatted stock quantity
     */
    private String formatStockQuantity(
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
     * Safely trims text.
     *
     * Null values are converted into an empty string so that validation checks
     * can be carried out safely.
     *
     * @param value the text value to clean
     * @return the trimmed text, or an empty string if the value is null
     */
    private String safeText(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    /**
     * Rolls a transaction back without hiding the original exception.
     *
     * If the rollback itself fails, that rollback exception is added to the
     * original exception as a suppressed exception.
     *
     * @param conn              the active database connection
     * @param originalException the original exception that caused the rollback
     */
    private void rollbackTransaction(
            Connection conn,
            Exception originalException
    ) {

        try {
            conn.rollback();

        } catch (Exception rollbackException) {
            originalException.addSuppressed(
                    rollbackException
            );
        }
    }

    /**
     * Holds the result of a stock check.
     *
     * This private class stores whether stock is sufficient, the available
     * batches grouped by inventory item and the readable shortfall message.
     */
    private static final class StockAssessment {

        private final boolean sufficientStock;

        private final Map<Integer, List<InventoryBatch>> availableBatchesByItem;

        private final String shortfallMessage;

        /**
         * Creates a stock assessment result.
         *
         * @param sufficientStock       whether enough usable stock is available
         * @param availableBatchesByItem available batches grouped by inventory item
         * @param shortfallMessage      readable stock shortfall information
         */
        private StockAssessment(
                boolean sufficientStock,
                Map<Integer, List<InventoryBatch>> availableBatchesByItem,
                String shortfallMessage
        ) {
            this.sufficientStock
                    = sufficientStock;

            this.availableBatchesByItem
                    = availableBatchesByItem;

            this.shortfallMessage
                    = shortfallMessage == null
                            ? ""
                            : shortfallMessage;
        }

        /**
         * Checks whether enough usable stock is available.
         *
         * @return true if stock is sufficient, otherwise false
         */
        private boolean hasSufficientStock() {
            return sufficientStock;
        }

        /**
         * Gets the available batches grouped by inventory item.
         *
         * @return available batches grouped by inventory item ID
         */
        private Map<Integer, List<InventoryBatch>>
                getAvailableBatchesByItem() {

            return availableBatchesByItem;
        }

        /**
         * Gets the readable stock shortfall message.
         *
         * @return the stock shortfall message
         */
        private String getShortfallMessage() {
            return shortfallMessage;
        }
    }
}