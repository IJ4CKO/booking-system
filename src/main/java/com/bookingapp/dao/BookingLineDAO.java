
package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.BookingLine;
 
/**
 * Data access object for the {booking_line} table, which holds the
 * individual product lines belonging to a booking.

 */
public class BookingLineDAO {
 
    /**
     * Adds a product line to a booking.
     *
     * <p>This behaves as an upsert rather than a plain insert: if the booking
     * already has a line for the same product, the existing line's quantity is
     * increased by the amount given instead of a second row being created. A
     * booking therefore holds at most one line per product.</p>
     */
    public void addBookingLine(
            Connection conn,
            BookingLine bookingLine
    ) throws Exception {
 
        validateBookingLine(
                conn,
                bookingLine
        );
 
        if (bookingLineAlreadyExists(
                conn,
                bookingLine.getBookingId(),
                bookingLine.getProductId()
        )) {
            increaseExistingBookingLineQuantity(
                    conn,
                    bookingLine.getBookingId(),
                    bookingLine.getProductId(),
                    bookingLine.getQuantity()
            );
 
            return;
        }
 
        String sql = """
            INSERT INTO booking_line (
                booking_id,
                product_id,
                quantity
            )
            VALUES (?, ?, ?)
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    bookingLine.getBookingId()
            );
 
            statement.setInt(
                    2,
                    bookingLine.getProductId()
            );
 
            statement.setInt(
                    3,
                    bookingLine.getQuantity()
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Reports whether a booking already holds a line for the given product.
     */
    public boolean bookingLineAlreadyExists(
            Connection conn,
            int bookingId,
            int productId
    ) throws Exception {
 
        String sql = """
            SELECT COUNT(*)
            FROM booking_line
            WHERE booking_id = ?
              AND product_id = ?
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    bookingId
            );
 
            statement.setInt(
                    2,
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
     * Adds to the quantity of a line that is already on the booking.
     */
    private void increaseExistingBookingLineQuantity(
            Connection conn,
            int bookingId,
            int productId,
            int quantityToAdd
    ) throws Exception {
 
        String sql = """
            UPDATE booking_line
            SET quantity = quantity + ?
            WHERE booking_id = ?
              AND product_id = ?
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    quantityToAdd
            );
 
            statement.setInt(
                    2,
                    bookingId
            );
 
            statement.setInt(
                    3,
                    productId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Retrieves all lines belonging to a booking, in the order they were
     * added, together with the name of each product.
     */
    public List<BookingLine> getBookingLinesByBookingId(
            int bookingId
    ) throws Exception {
 
        List<BookingLine> lines =
                new ArrayList<>();
 
        String sql = """
            SELECT
                bl.booking_line_id,
                bl.booking_id,
                bl.product_id,
                bl.quantity,
                p.name AS product_name
            FROM booking_line bl
            JOIN products p ON bl.product_id = p.product_id
            WHERE bl.booking_id = ?
            ORDER BY bl.booking_line_id
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
 
                while (results.next()) {
                    lines.add(
                            new BookingLine(
                                    results.getInt("booking_line_id"),
                                    results.getInt("booking_id"),
                                    results.getInt("product_id"),
                                    results.getInt("quantity"),
                                    results.getString("product_name")
                            )
                    );
                }
            }
        }
 
        return lines;
    }
 
    /**
     * Removes every line belonging to a booking.
     */
    public void deleteBookingLinesByBookingId(
            Connection conn,
            int bookingId
    ) throws Exception {
 
        String sql = """
            DELETE FROM booking_line
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
     * Checks that the minimum details needed to save a booking line are
     * present.
     */
    private void validateBookingLine(
            Connection conn,
            BookingLine bookingLine
    ) {
        if (conn == null) {
            throw new IllegalArgumentException(
                    "A database connection is required."
            );
        }
 
        if (bookingLine == null) {
            throw new IllegalArgumentException(
                    "A booking line is required."
            );
        }
 
        if (bookingLine.getBookingId() <= 0) {
            throw new IllegalArgumentException(
                    "A booking is required."
            );
        }
 
        if (bookingLine.getProductId() <= 0) {
            throw new IllegalArgumentException(
                    "A product is required."
            );
        }
 
        if (bookingLine.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "The product quantity must be greater than zero."
            );
        }
    }
}
 