

package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.Booking;
 
/**
 * Data access object for the {bookings} table.
 */
public class BookingDAO {
 
    /**
     * Creates a new booking and returns its generated identifier.
     *
     * <p>The booking is validated and checked against existing
     * non-cancelled bookings before the insert is attempted, so a
     * customer cannot end up with two live bookings in the same slot.
     * The supplied connection is not closed, allowing the caller to
     * commit or roll back the surrounding transaction.</p>
     *
     * @param conn    an open connection, managed by the caller
     * @param booking the booking to persist
     * @return the newly generated booking identifier
     * @throws IllegalArgumentException if the booking is incomplete, or if
     *                                  the customer already holds an active
     *                                  booking in that slot
     * @throws Exception                if the insert fails or no key is returned
     */
    public int addBooking(
            Connection conn,
            Booking booking
    ) throws Exception {
 
        validateBooking(
                conn,
                booking
        );
 
        ensureNoDuplicateActiveBooking(
                conn,
                booking.getCustomerId(),
                booking.getCollectionDate(),
                booking.getCollectionTime()
        );
 
        String sql = """
            INSERT INTO bookings (
                customer_id,
                owner_user_id,
                booking_date,
                booking_time,
                dropoff_date,
                dropoff_time,
                dropoff_address,
                items_required_text,
                status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(
                             sql,
                             PreparedStatement.RETURN_GENERATED_KEYS
                     )) {
 
            statement.setInt(
                    1,
                    booking.getCustomerId()
            );
 
            statement.setInt(
                    2,
                    booking.getOwnerUserId()
            );
 
            statement.setString(
                    3,
                    booking.getCollectionDate()
            );
 
            statement.setString(
                    4,
                    booking.getCollectionTime()
            );
 
            statement.setString(
                    5,
                    booking.getDropoffDate()
            );
 
            statement.setString(
                    6,
                    booking.getDropoffTime()
            );
 
            statement.setString(
                    7,
                    booking.getDropoffAddress()
            );
 
            statement.setString(
                    8,
                    booking.getItemsRequired()
            );
 
            statement.setString(
                    9,
                    booking.getStatus()
            );
 
            statement.executeUpdate();
 
            try (ResultSet results =
                         statement.getGeneratedKeys()) {
 
                if (results.next()) {
                    return results.getInt(1);
                }
            }
        }
 
        throw new Exception(
                "The booking was unable to be created."
        );
    }
 

    public void updateBookingStatus(
            Connection conn,
            int bookingId,
            String status
    ) throws Exception {
 
        String sql = """
            UPDATE bookings
            SET status = ?
            WHERE booking_id = ?
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    status
            );
 
            statement.setInt(
                    2,
                    bookingId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Retrieves a single booking by its identifier.
     *
     * <p>This method opens and closes its own connection. The booking is
     * returned without the customer's name, as no join is performed here.</p>
     */
    public Booking getBookingById(
            int bookingId
    ) throws Exception {
 
        String sql = """
            SELECT
                booking_id,
                customer_id,
                owner_user_id,
                booking_date,
                booking_time,
                dropoff_date,
                dropoff_time,
                dropoff_address,
                items_required_text,
                status
            FROM bookings
            WHERE booking_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    bookingId
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (results.next()) {
                    return new Booking(
                            results.getInt("booking_id"),
                            results.getInt("customer_id"),
                            results.getInt("owner_user_id"),
                            results.getString("booking_date"),
                            results.getString("booking_time"),
                            results.getString("dropoff_date"),
                            results.getString("dropoff_time"),
                            results.getString("dropoff_address"),
                            results.getString("items_required_text"),
                            results.getString("status")
                    );
                }
            }
        }
 
        return null;
    }
 
    /**
     * Retrieves every booking, newest first, along with the name of the
     * customer each one belongs to.
     */
    public List<Booking> getAllBookings()
            throws Exception {
 
        List<Booking> bookings =
                new ArrayList<>();
 
        String sql = """
            SELECT
                b.booking_id,
                b.customer_id,
                b.owner_user_id,
                b.booking_date,
                b.booking_time,
                b.dropoff_date,
                b.dropoff_time,
                b.dropoff_address,
                b.items_required_text,
                b.status,
                c.full_name AS customer_name
            FROM bookings b
            JOIN customer c ON b.customer_id = c.customer_id
            ORDER BY b.booking_id DESC
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql);
             ResultSet results =
                     statement.executeQuery()) {
 
            while (results.next()) {
                bookings.add(
                        new Booking(
                                results.getInt("booking_id"),
                                results.getInt("customer_id"),
                                results.getInt("owner_user_id"),
                                results.getString("booking_date"),
                                results.getString("booking_time"),
                                results.getString("dropoff_date"),
                                results.getString("dropoff_time"),
                                results.getString("dropoff_address"),
                                results.getString("items_required_text"),
                                results.getString("status"),
                                results.getString("customer_name")
                        )
                );
            }
        }
 
        return bookings;
    }
 
    /**
     * Permanently removes a booking from the database.
     */
    public void deleteBooking(
            Connection conn,
            int bookingId
    ) throws Exception {
 
        String sql = """
            DELETE FROM bookings
            WHERE booking_id = ?
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    bookingId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Reports whether a customer already holds a non-cancelled booking at
     * the given date and time.
     */
    public boolean activeBookingAlreadyExists(
            int customerId,
            String bookingDate,
            String bookingTime
    ) throws Exception {
 
        try (Connection conn =
                     DBConnection.getConnection()) {
 
            return activeBookingAlreadyExists(
                    conn,
                    customerId,
                    bookingDate,
                    bookingTime
            );
        }
    }
 
    /**
     * Reports whether a customer already holds a non-cancelled booking at
     * the given date and time, using a connection supplied by the caller.
     */
    public boolean activeBookingAlreadyExists(
            Connection conn,
            int customerId,
            String bookingDate,
            String bookingTime
    ) throws Exception {
 
        String sql = """
            SELECT COUNT(*)
            FROM bookings
            WHERE customer_id = ?
              AND booking_date = ?
              AND booking_time = ?
              AND status <> 'CANCELLED'
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    customerId
            );
 
            statement.setString(
                    2,
                    bookingDate
            );
 
            statement.setString(
                    3,
                    bookingTime
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (results.next()) {
                    return results.getInt(1) > 0;
                }
            }
        }
 
        return false;
    }
 
    /**
     * Guard clause used before inserting a booking: throws if the customer
     * already has an active booking in the requested slot.
     */
    private void ensureNoDuplicateActiveBooking(
            Connection conn,
            int customerId,
            String bookingDate,
            String bookingTime
    ) throws Exception {
 
        if (activeBookingAlreadyExists(
                conn,
                customerId,
                bookingDate,
                bookingTime
        )) {
            throw new IllegalArgumentException(
                    "This customer already has a booking at this date and time."
            );
        }
    }

 /**
 * Gets active bookings for the dashboard calendar list.
 *
 * The dashboard uses this method to show bookings due on the selected date.
 * Cancelled bookings are excluded so the owner only sees work that still needs
 * attention.
 *
 * @param bookingDate selected date in dd/MM/yyyy format
 * @return list of booking summary strings
 */
public static List<String> getDashboardBookingsByDate(String bookingDate) {
    List<String> bookings = new ArrayList<>();

    String sql = """
            SELECT
                b.booking_id,
                c.full_name,
                b.booking_date,
                b.booking_time,
                b.dropoff_date,
                b.dropoff_time,
                b.status
            FROM bookings b
            JOIN customer c
              ON b.customer_id = c.customer_id
            WHERE b.booking_date = ?
              AND UPPER(TRIM(b.status)) <> 'CANCELLED'
            ORDER BY b.booking_time
            """;

    try (
            Connection connection = DBConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
    ) {
        statement.setString(
                1,
                bookingDate
        );

        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String bookingSummary =
                        "Booking #"
                        + resultSet.getInt("booking_id")
                        + " - "
                        + resultSet.getString("full_name")
                        + " at "
                        + resultSet.getString("booking_time")
                        + " ("
                        + resultSet.getString("status")
                        + ")";

                bookings.add(
                        bookingSummary
                );
            }
        }

    } catch (SQLException exception) {
        exception.printStackTrace();
    }

    return bookings;
}
 
    /**
     * Reports whether a customer already has a non-cancelled booking for a
     * particular product at the given date and time.
     */
    public boolean bookingAlreadyExists(
            int customerId,
            String bookingDate,
            String bookingTime,
            int productId
    ) throws Exception {
 
        String sql = """
            SELECT COUNT(*)
            FROM bookings b
            JOIN booking_line bl ON b.booking_id = bl.booking_id
            WHERE b.customer_id = ?
              AND b.booking_date = ?
              AND b.booking_time = ?
              AND bl.product_id = ?
              AND b.status <> 'CANCELLED'
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    customerId
            );
 
            statement.setString(
                    2,
                    bookingDate
            );
 
            statement.setString(
                    3,
                    bookingTime
            );
 
            statement.setInt(
                    4,
                    productId
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (results.next()) {
                    return results.getInt(1) > 0;
                }
            }
        }
 
        return false;
    }
 
    /**
     * Checks that the minimum details needed to save a booking are present.
     */
    private void validateBooking(
            Connection conn,
            Booking booking
    ) {
        if (conn == null) {
            throw new IllegalArgumentException(
                    "A database connection is required."
            );
        }
 
        if (booking == null) {
            throw new IllegalArgumentException(
                    "A booking is required."
            );
        }
 
        if (booking.getCustomerId() <= 0) {
            throw new IllegalArgumentException(
                    "A customer is required."
            );
        }
 
        if (safeText(
                booking.getCollectionDate()
        ).isBlank()) {
            throw new IllegalArgumentException(
                    "A booking date is required."
            );
        }
 
        if (safeText(
                booking.getCollectionTime()
        ).isBlank()) {
            throw new IllegalArgumentException(
                    "A booking time is required."
            );
        }
    }

    private String safeText(
            String value
    ) {
        return value == null ? "" : value.trim();
    }
}
 