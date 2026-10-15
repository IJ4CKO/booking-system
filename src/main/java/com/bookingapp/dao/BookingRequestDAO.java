package com.bookingapp.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.bookingapp.model.BookingLine;
import com.bookingapp.model.BookingRequest;

/**
 * Data access object for the {booking_request} table, which holds enquiries
 * submitted by customers before they become real bookings.
 */
public class BookingRequestDAO {

    /**
     * Retrieves every request still awaiting action, ordered so that the work
     * queue reads sensibly.
     */
    public List<BookingRequest> getPendingBookingRequests()
            throws Exception {

        String sql = """
                SELECT
                    request_id,
                    full_name,
                    email,
                    phone,
                    service_type,
                    fulfilment_type,
                    dropoff_address,
                    product_id,
                    quantity,
                    event_date,
                    event_time,
                    details,
                    source,
                    status,
                    reject_reason,
                    submitted_at,
                    order_total,
                    required_deposit,
                    deposit_status,
                    deposit_requested_date,
                    deposit_paid_date,
                    payment_method,
                    payment_reference,
                    booking_id
                FROM booking_request
                WHERE status IN (
                    'PENDING',
                    'PENDING_REVIEW',
                    'AWAITING_DEPOSIT',
                    'READY_TO_CONFIRM'
                )
                ORDER BY
                        CASE
                             WHEN status = 'PENDING'
                             THEN 0
                             WHEN status = 'PENDING_REVIEW'
                             THEN 0
                             WHEN status = 'AWAITING_DEPOSIT'
                             THEN 1
                             WHEN status = 'READY_TO_CONFIRM'
                             THEN 2
                             ELSE 3
                        END,
                        request_id DESC
                """;

        List<BookingRequest> requests
                = new ArrayList<>();

        try (Connection conn
                = DBConnection.getConnection(); PreparedStatement statement
                = conn.prepareStatement(sql); ResultSet results
                = statement.executeQuery()) {

            while (results.next()) {
                BookingRequest request
                        = mapBookingRequest(
                                conn,
                                results
                        );

                requests.add(
                        request
                );
            }
        }

        return requests;
    }

    /**
     * Sets the status of a request directly, without regard to the workflow.
     */
    public void updateStatus(
            int requestId,
            String status
    ) throws Exception {

        String sql = """
                UPDATE booking_request
                SET status = ?
                WHERE request_id = ?
                """;

        try (Connection conn
                = DBConnection.getConnection(); PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setString(
                    1,
                    status
            );

            statement.setInt(
                    2,
                    requestId
            );

            int rowsUpdated
                    = statement.executeUpdate();

            if (rowsUpdated != 1) {
                throw new IllegalStateException(
                        "The booking request status "
                        + "could not be updated."
                );
            }
        }
    }

   /**
 * Counts website booking requests that still need staff attention.
 *
 * This is used by the dashboard notification panel. It uses the same
 * booking_request table and active statuses as the Booking Requests screen.
 *
 * @return number of booking requests waiting for staff attention
 */
public static int countPendingRequests() {
    String sql = """
            SELECT COUNT(*)
            FROM booking_request
            WHERE status IN (
                'PENDING',
                'PENDING_REVIEW',
                'AWAITING_DEPOSIT',
                'READY_TO_CONFIRM'
            )
            """;

    try (
            Connection connection = DBConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
    ) {
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

    } catch (SQLException exception) {
        exception.printStackTrace();
    }

    return 0;
}

    /**
     * Moves a request to {AWAITING_DEPOSIT}, recording that a deposit has been
     * asked for.
     */
    public boolean markAwaitingDeposit(
            int requestId
    ) throws Exception {

        String sql = """
                UPDATE booking_request
                SET
                    status = 'AWAITING_DEPOSIT',
                    deposit_status = 'PENDING',
                    deposit_requested_date = CURRENT_TIMESTAMP,
                    deposit_paid_date = NULL,
                    payment_method = 'BANK_TRANSFER',
                    payment_reference = NULL,
                    reject_reason = NULL
                WHERE request_id = ?
                  AND status IN (
                      'PENDING',
                      'PENDING_REVIEW'
                  )
                  AND order_total IS NOT NULL
                  AND required_deposit IS NOT NULL
                  AND booking_id IS NULL
                """;

        try (Connection conn
                = DBConnection.getConnection(); PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    requestId
            );

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Prices a request by saving its order total and the deposit required
     * against it.
     */
    public void updateOrderTotalAndDeposit(
            int requestId,
            BigDecimal orderTotal,
            BigDecimal requiredDeposit
    ) throws Exception {

        if (orderTotal == null) {
            throw new IllegalArgumentException(
                    "The order total is required."
            );
        }

        if (requiredDeposit == null) {
            throw new IllegalArgumentException(
                    "The required deposit is required."
            );
        }

        BigDecimal normalisedOrderTotal
                = orderTotal.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal normalisedDeposit
                = requiredDeposit.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        if (normalisedOrderTotal.compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            throw new IllegalArgumentException(
                    "The order total must be greater than zero."
            );
        }

        if (normalisedDeposit.compareTo(
                BigDecimal.ZERO
        ) < 0) {
            throw new IllegalArgumentException(
                    "The deposit cannot be negative."
            );
        }

        String sql = """
                UPDATE booking_request
                SET
                    order_total = ?,
                    required_deposit = ?
                WHERE request_id = ?
                  AND status IN (
                      'PENDING',
                      'PENDING_REVIEW'
                  )
                  AND booking_id IS NULL
                """;

        try (Connection conn
                = DBConnection.getConnection(); PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setBigDecimal(
                    1,
                    normalisedOrderTotal
            );

            statement.setBigDecimal(
                    2,
                    normalisedDeposit
            );

            statement.setInt(
                    3,
                    requestId
            );

            int rowsUpdated
                    = statement.executeUpdate();

            if (rowsUpdated != 1) {
                throw new IllegalStateException(
                        "The order total and deposit could "
                        + "not be saved. The request "
                        + "may already have been reviewed."
                );
            }
        }
    }

    /**
     * Records that the deposit has been received and moves the request to
     * {READY_TO_CONFIRM}.
     */
    public boolean markDepositPaid(
            int requestId,
            String paymentReference
    ) throws Exception {

        String sql = """
        UPDATE booking_request
        SET
            status = 'READY_TO_CONFIRM',
            deposit_status = 'PAID',
            deposit_paid_date = CURRENT_TIMESTAMP,
            payment_method = 'BANK_TRANSFER',
            payment_reference = ?
        WHERE request_id = ?
          AND status = 'AWAITING_DEPOSIT'
          AND deposit_status = 'PENDING'
          AND booking_id IS NULL
        """;

        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {

            if (paymentReference == null
                    || paymentReference.isBlank()) {

                statement.setNull(
                        1,
                        Types.VARCHAR
                );

            } else {
                statement.setString(
                        1,
                        paymentReference.trim()
                );
            }

            statement.setInt(
                    2,
                    requestId
            );

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Claims a request for confirmation by moving it to {@code CONFIRMING}.
     */
    public boolean claimForConfirmation(
            Connection conn,
            int requestId
    ) throws Exception {

        String sql = """
                UPDATE booking_request
                SET status = 'CONFIRMING'
                WHERE request_id = ?
                  AND status = 'READY_TO_CONFIRM'
                  AND deposit_status = 'PAID'
                  AND booking_id IS NULL
                """;

        try (PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    requestId
            );

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Completes the confirmation of a claimed request by marking it {CONFIRMED}
     * and linking it to the booking that was created from it.
     */
    public void completeConfirmation(
            Connection conn,
            int requestId,
            int bookingId
    ) throws Exception {

        String sql = """
                UPDATE booking_request
                SET
                    status = 'CONFIRMED',
                    booking_id = ?
                WHERE request_id = ?
                  AND status = 'CONFIRMING'
                  AND deposit_status = 'PAID'
                  AND booking_id IS NULL
                """;

        try (PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    bookingId
            );

            statement.setInt(
                    2,
                    requestId
            );

            int rowsUpdated
                    = statement.executeUpdate();

            if (rowsUpdated != 1) {
                throw new IllegalStateException(
                        "The confirmed booking could not be "
                        + "linked to its original request."
                );
            }
        }
    }

    /**
     * Translates the products named on a request into booking lines ready to be
     * saved against a real booking.
     */
    public List<BookingLine> getBookingLinesForRequest(
            Connection conn,
            int requestId
    ) throws Exception {

        String sql = """
                SELECT
                    p.product_id,
                    COALESCE(brl.quantity, 1) AS quantity
                FROM booking_request_line brl
                JOIN products p
                  ON LOWER(TRIM(p.name)) =
                     LOWER(TRIM(brl.product_name))
                WHERE brl.request_id = ?
                  AND brl.product_name IS NOT NULL
                  AND TRIM(brl.product_name) <> ''
                ORDER BY brl.request_line_id
                """;

        List<BookingLine> bookingLines
                = new ArrayList<>();

        try (PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    requestId
            );

            try (ResultSet results
                    = statement.executeQuery()) {

                while (results.next()) {
                    int quantity
                            = results.getInt(
                                    "quantity"
                            );

                    if (quantity <= 0) {
                        throw new IllegalStateException(
                                "A requested product has an "
                                + "invalid quantity."
                        );
                    }

                    bookingLines.add(
                            new BookingLine(
                                    0,
                                    0,
                                    results.getInt(
                                            "product_id"
                                    ),
                                    quantity
                            )
                    );
                }
            }
        }

        if (bookingLines.isEmpty()) {
            addLegacyBookingLine(
                    conn,
                    requestId,
                    bookingLines
            );
        }

        return bookingLines;
    }

    /**
     * Lists the product names on a request that do not correspond to anything
     * in the catalogue.
     */
    public List<String> getUnmatchedProductNames(
            Connection conn,
            int requestId
    ) throws Exception {

        String sql = """
                SELECT DISTINCT brl.product_name
                FROM booking_request_line brl
                LEFT JOIN products p
                  ON LOWER(TRIM(p.name)) =
                     LOWER(TRIM(brl.product_name))
                WHERE brl.request_id = ?
                  AND brl.product_name IS NOT NULL
                  AND TRIM(brl.product_name) <> ''
                  AND p.product_id IS NULL
                ORDER BY brl.product_name
                """;

        List<String> unmatchedNames
                = new ArrayList<>();

        try (PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    requestId
            );

            try (ResultSet results
                    = statement.executeQuery()) {

                while (results.next()) {
                    unmatchedNames.add(
                            results.getString(
                                    "product_name"
                            )
                    );
                }
            }
        }

        return unmatchedNames;
    }

    /**
     * Retrieves the requests that went on to become bookings, newest first.
     */
    public List<BookingRequest> getConfirmedBookingRequestHistory()
            throws Exception {

        return getBookingRequestHistory(
                "CONFIRMED"
        );
    }

    /**
     * Retrieves the requests that were turned down, newest first, along with
     * the reason recorded against each.
     */
    public List<BookingRequest> getRejectedBookingRequestHistory()
            throws Exception {

        return getBookingRequestHistory(
                "REJECTED"
        );
    }

    /**
     * Turns a request down, recording why.
     */
    public void rejectRequest(
            int requestId,
            String rejectionReason
    ) throws Exception {

        if (rejectionReason == null
                || rejectionReason.isBlank()) {

            throw new IllegalArgumentException(
                    "A rejection reason is required."
            );
        }

        String sql = """
                UPDATE booking_request
                SET
                    status = 'REJECTED',
                    reject_reason = ?
                WHERE request_id = ?
                  AND status IN (
                      'PENDING',
                      'PENDING_REVIEW',
                      'AWAITING_DEPOSIT'
                  )
                  AND COALESCE(
                      deposit_status,
                      'NOT_REQUESTED'
                  ) <> 'PAID'
                  AND booking_id IS NULL
                """;

        try (Connection conn
                = DBConnection.getConnection(); PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setString(
                    1,
                    rejectionReason.trim()
            );

            statement.setInt(
                    2,
                    requestId
            );

            int rowsUpdated
                    = statement.executeUpdate();

            if (rowsUpdated != 1) {
                throw new IllegalStateException(
                        "The request could not be rejected. "
                        + "A paid deposit must be handled "
                        + "through the refund process."
                );
            }
        }
    }

    /**
     * Amends the customer and event details of a request that is still open to
     * change.
     */
    public boolean updateEditableRequest(
            int requestId,
            String fullName,
            String email,
            String phone,
            String fulfilmentType,
            String dropoffAddress,
            String eventDate,
            String eventTime,
            String details
    ) throws Exception {

        String sql = """
            UPDATE booking_request
            SET
                full_name = ?,
                email = ?,
                phone = ?,
                fulfilment_type = ?,
                dropoff_address = ?,
                event_date = ?,
                event_time = ?,
                details = ?
            WHERE request_id = ?
              AND status IN (
                  'PENDING',
                  'PENDING_REVIEW',
                  'AWAITING_DEPOSIT'
              )
              AND COALESCE(
                  deposit_status,
                  'NOT_REQUESTED'
              ) <> 'PAID'
              AND booking_id IS NULL
            """;

        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setString(4, fulfilmentType);

            if (dropoffAddress == null || dropoffAddress.isBlank()) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, dropoffAddress.trim());
            }

            statement.setString(6, eventDate);
            statement.setString(7, eventTime);

            if (details == null || details.isBlank()) {
                statement.setNull(8, Types.VARCHAR);
            } else {
                statement.setString(8, details.trim());
            }

            statement.setInt(9, requestId);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Permanently removes a request and its product lines, provided it is still
     * open to change.
     */
    public boolean deleteEditableRequest(
            int requestId
    ) throws Exception {

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                String deleteLinesSql = """
                    DELETE FROM booking_request_line
                    WHERE request_id = ?
                    """;

                try (PreparedStatement statement
                        = conn.prepareStatement(deleteLinesSql)) {

                    statement.setInt(1, requestId);
                    statement.executeUpdate();
                }

                String deleteRequestSql = """
                    DELETE FROM booking_request
                    WHERE request_id = ?
                      AND status IN (
                          'PENDING',
                          'PENDING_REVIEW',
                          'AWAITING_DEPOSIT'
                      )
                      AND COALESCE(
                          deposit_status,
                          'NOT_REQUESTED'
                      ) <> 'PAID'
                      AND booking_id IS NULL
                    """;

                boolean deleted;

                try (PreparedStatement statement
                        = conn.prepareStatement(deleteRequestSql)) {

                    statement.setInt(1, requestId);
                    deleted = statement.executeUpdate() == 1;
                }

                if (deleted) {
                    conn.commit();

                } else {
                    conn.rollback();
                }

                return deleted;

            } catch (Exception e) {
                conn.rollback();
                throw e;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Shared implementation behind the two history methods.
     */
    private List<BookingRequest> getBookingRequestHistory(
            String requiredStatus
    ) throws Exception {

        if (!"CONFIRMED".equals(requiredStatus)
                && !"REJECTED".equals(requiredStatus)) {

            throw new IllegalArgumentException(
                    "Unsupported booking request history status."
            );
        }

        String sql = """
                SELECT
                    request_id,
                    full_name,
                    email,
                    phone,
                    service_type,
                    fulfilment_type,
                    dropoff_address,
                    product_id,
                    quantity,
                    event_date,
                    event_time,
                    details,
                    source,
                    status,
                    reject_reason,
                    submitted_at,
                    order_total,
                    required_deposit,
                    deposit_status,
                    deposit_requested_date,
                    deposit_paid_date,
                    payment_method,
                    payment_reference,
                    booking_id
                FROM booking_request
                WHERE status = ?
                ORDER BY
                    submitted_at DESC,
                    request_id DESC
                """;

        List<BookingRequest> requests
                = new ArrayList<>();

        try (Connection conn
                = DBConnection.getConnection(); PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setString(
                    1,
                    requiredStatus
            );

            try (ResultSet results
                    = statement.executeQuery()) {

                while (results.next()) {
                    BookingRequest request
                            = mapBookingRequest(
                                    conn,
                                    results
                            );

                    requests.add(
                            request
                    );
                }
            }
        }

        return requests;
    }

    /**
     * Builds a {BookingRequest} from the current row of a result set.
     */
    private BookingRequest mapBookingRequest(
            Connection conn,
            ResultSet results
    ) throws Exception {

        int requestId
                = results.getInt(
                        "request_id"
                );

        BookingRequest request
                = new BookingRequest(
                        requestId,
                        results.getString(
                                "full_name"
                        ),
                        results.getString(
                                "email"
                        ),
                        results.getString(
                                "phone"
                        ),
                        results.getString(
                                "service_type"
                        ),
                        getRequestedServicesSummary(
                                conn,
                                requestId
                        ),
                        results.getString(
                                "fulfilment_type"
                        ),
                        results.getString(
                                "dropoff_address"
                        ),
                        (Integer) results.getObject(
                                "product_id"
                        ),
                        (Integer) results.getObject(
                                "quantity"
                        ),
                        results.getString(
                                "event_date"
                        ),
                        results.getString(
                                "event_time"
                        ),
                        results.getString(
                                "details"
                        ),
                        results.getString(
                                "source"
                        ),
                        results.getString(
                                "status"
                        ),
                        results.getString(
                                "reject_reason"
                        ),
                        results.getString(
                                "submitted_at"
                        ),
                        getNullableBigDecimal(
                                results,
                                "order_total"
                        ),
                        getNullableBigDecimal(
                                results,
                                "required_deposit"
                        ),
                        results.getString(
                                "deposit_status"
                        ),
                        results.getString(
                                "deposit_requested_date"
                        ),
                        results.getString(
                                "deposit_paid_date"
                        ),
                        results.getString(
                                "payment_method"
                        ),
                        results.getString(
                                "payment_reference"
                        ),
                        (Integer) results.getObject(
                                "booking_id"
                        )
                );

        request.setProductQuantitiesSummary(
                getProductQuantitiesSummary(
                        conn,
                        requestId
                )
        );

        return request;
    }

    /**
     * Fallback for requests submitted before the request lines table existed,
     * which carried a single product and quantity on the request row itself.
     */
    private void addLegacyBookingLine(
            Connection conn,
            int requestId,
            List<BookingLine> bookingLines
    ) throws Exception {

        String sql = """
                SELECT
                    product_id,
                    quantity
                FROM booking_request
                WHERE request_id = ?
                  AND product_id IS NOT NULL
                  AND quantity IS NOT NULL
                """;

        try (PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    requestId
            );

            try (ResultSet results
                    = statement.executeQuery()) {

                if (results.next()) {
                    int quantity
                            = results.getInt(
                                    "quantity"
                            );

                    if (quantity > 0) {
                        bookingLines.add(
                                new BookingLine(
                                        0,
                                        0,
                                        results.getInt(
                                                "product_id"
                                        ),
                                        quantity
                                )
                        );
                    }
                }
            }
        }
    }

    /**
     * Reads a monetary column that may be absent.
     *
     * <p>
     * The value is fetched as text first, since
     * {@link ResultSet#getBigDecimal(String)} cannot distinguish a missing
     * figure from zero without a follow-up check. A populated value is
     * normalised to two decimal places so that amounts compare and display
     * consistently.</p>
     *
     * @param results a result set positioned on the row to read
     * @param columnName the column to read
     * @return the amount to two decimal places, or {@code null} if not set
     * @throws Exception if the column cannot be read
     */
    private BigDecimal getNullableBigDecimal(
            ResultSet results,
            String columnName
    ) throws Exception {

        String value
                = results.getString(
                        columnName
                );

        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    /**
     * Builds the "what was ordered" summary shown against a request, in the
     * form {Product: 2; Other product: 1}.
     */
    private String getProductQuantitiesSummary(
            Connection conn,
            int requestId
    ) throws Exception {

        String sql = """
                SELECT
                    product_name,
                    COALESCE(quantity, 1) AS quantity
                FROM booking_request_line
                WHERE request_id = ?
                  AND product_name IS NOT NULL
                  AND TRIM(product_name) <> ''
                ORDER BY request_line_id
                """;

        List<String> productQuantities
                = new ArrayList<>();

        try (PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    requestId
            );

            try (ResultSet results
                    = statement.executeQuery()) {

                while (results.next()) {
                    String productName
                            = results.getString(
                                    "product_name"
                            );

                    int quantity
                            = results.getInt(
                                    "quantity"
                            );

                    if (quantity <= 0) {
                        quantity = 1;
                    }

                    productQuantities.add(
                            productName
                            + ": "
                            + quantity
                    );
                }
            }
        }

        if (!productQuantities.isEmpty()) {
            return String.join(
                    "; ",
                    productQuantities
            );
        }

        return getLegacyProductQuantitiesSummary(
                conn,
                requestId
        );
    }

    /**
     * Summarises the single product held on an older request row, joining to
     * the catalogue for its name.
     */
    private String getLegacyProductQuantitiesSummary(
            Connection conn,
            int requestId
    ) throws Exception {

        String sql = """
                SELECT
                    p.name AS product_name,
                    COALESCE(br.quantity, 1) AS quantity
                FROM booking_request br
                JOIN products p
                  ON p.product_id = br.product_id
                WHERE br.request_id = ?
                  AND br.product_id IS NOT NULL
                """;

        try (PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    requestId
            );

            try (ResultSet results
                    = statement.executeQuery()) {

                if (results.next()) {
                    String productName
                            = results.getString(
                                    "product_name"
                            );

                    int quantity
                            = results.getInt(
                                    "quantity"
                            );

                    if (quantity <= 0) {
                        quantity = 1;
                    }

                    return productName
                            + ": "
                            + quantity;
                }
            }
        }

        return "";
    }

    /**
     * Builds the "what was asked for" summary shown against a request, as a
     * comma-separated list such as {@code Hire - Product, Delivery}.
     */
    private String getRequestedServicesSummary(
            Connection conn,
            int requestId
    ) throws Exception {

        String sql = """
                SELECT
                    service_type,
                    product_name
                FROM booking_request_line
                WHERE request_id = ?
                ORDER BY request_line_id
                """;

        List<String> requestedItems
                = new ArrayList<>();

        try (PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    requestId
            );

            try (ResultSet results
                    = statement.executeQuery()) {

                while (results.next()) {
                    String serviceType
                            = results.getString(
                                    "service_type"
                            );

                    String productName
                            = results.getString(
                                    "product_name"
                            );

                    String summaryText;

                    if (productName != null
                            && !productName.isBlank()) {

                        if (serviceType != null
                                && !serviceType.isBlank()) {

                            summaryText
                                    = serviceType
                                    + " - "
                                    + productName;

                        } else {
                            summaryText
                                    = productName;
                        }

                    } else {
                        summaryText
                                = serviceType;
                    }

                    if (summaryText != null
                            && !summaryText.isBlank()
                            && !requestedItems.contains(
                                    summaryText
                            )) {

                        requestedItems.add(
                                summaryText
                        );
                    }
                }
            }
        }

        return String.join(
                ", ",
                requestedItems
        );
    }
}
