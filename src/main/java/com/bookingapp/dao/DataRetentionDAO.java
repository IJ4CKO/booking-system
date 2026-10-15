package com.bookingapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bookingapp.model.DataRetentionCustomer;

/**
 * Handles database operations for the data retention review feature.
 *
 * The data retention feature helps the business owner identify customers whose
 * last confirmed or completed order was more than seven years ago. The owner
 * can then decide whether the customer's personal data should be anonymised.
 */
public class DataRetentionDAO {

    private static final int CUSTOMER_DATA_RETENTION_YEARS = 7;

    private static final DateTimeFormatter BRITISH_DATE_FORMAT
            = DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DATABASE_DATE_FORMAT
            = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Gets the customers who are due for data retention review.
     *
     * This method calls loadLatestValidOrderRecordsByCustomer() to get each
     * customer's latest confirmed or completed order date. It then checks
     * whether the last valid order date is older than the retention period,
     * currently set to seven years. For each customer who meets this condition,
     * the number of years since their last order is calculated using
     * Period.between, based on today's date.
     *
     * Customers with no valid parsed order date are ignored because the system
     * cannot safely decide whether they are due for review.
     *
     * @return a list of DataRetentionCustomer objects for customers whose last
     * valid order was outside the retention period
     */
    public List<DataRetentionCustomer> getCustomersDueForRetentionReview() {
        Map<Integer, CustomerRetentionRecord> customerRecords
                = loadLatestValidOrderRecordsByCustomer();

        LocalDate today = LocalDate.now();

        LocalDate retentionCutOffDate
                = today.minusYears(CUSTOMER_DATA_RETENTION_YEARS);

        List<DataRetentionCustomer> customersDueForReview
                = new ArrayList<>();

        for (CustomerRetentionRecord record : customerRecords.values()) {
            LocalDate lastOrderDate = record.getLastOrderDate();

            if (lastOrderDate == null) {
                continue;
            }

            if (lastOrderDate.isAfter(retentionCutOffDate)) {
                continue;
            }

            int yearsSinceLastOrder = Period.between(
                    lastOrderDate,
                    today
            ).getYears();

            DataRetentionCustomer dataRetentionCustomer
                    = new DataRetentionCustomer(
                            record.getCustomerId(),
                            record.getFullName(),
                            record.getEmail(),
                            record.getPhone(),
                            lastOrderDate,
                            yearsSinceLastOrder,
                            "Review for anonymisation"
                    );

            customersDueForReview.add(dataRetentionCustomer);
        }

        return customersDueForReview;
    }

    /**
     * Anonymises the selected customer as part of the seven-year data retention
     * process.
     *
     * This method coordinates the full anonymisation process inside one
     * database transaction. It removes the customer's personal details from the
     * customer table, removes copied contact details from linked website
     * booking requests where a booking_id relationship exists, and records the
     * anonymisation action in the data retention log.
     *
     * If any part of the process fails, the transaction is rolled back so the
     * database is not left partly updated.
     *
     * @param customerId the database identifier of the customer to anonymise
     */
    public void anonymiseCustomer(int customerId) {
        if (customerId <= 0) {
            throw new IllegalArgumentException(
                    "A valid customer ID is required."
            );
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                anonymiseCustomerRecord(connection, customerId);
                anonymiseLinkedBookingRequestData(connection, customerId);
                insertDataRetentionLog(connection, customerId);

                connection.commit();

            } catch (SQLException exception) {
                connection.rollback();

                throw new RuntimeException(
                        "Unable to anonymise the selected customer.",
                        exception
                );

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Unable to complete the customer anonymisation process.",
                    exception
            );
        }
    }

    /**
     * Loads each customer's latest valid order record from the database.
     *
     * A valid order is a booking with a status of CONFIRMED or COMPLETED.
     * Cancelled bookings are ignored because they do not count as completed
     * customer orders for data retention purposes.
     *
     * The query may return more than one booking row for the same customer. A
     * map is used so each customer is only tracked once, and the stored last
     * order date is updated only when a newer valid booking date is found.
     *
     * Date comparison is carried out in Java rather than SQLite because the
     * project may store booking dates in British format, such as dd/MM/yyyy.
     *
     * @return each customer ID mapped to a temporary retention record
     * containing customer details and the latest valid order date found
     */
    private Map<Integer, CustomerRetentionRecord>
            loadLatestValidOrderRecordsByCustomer() {

        Map<Integer, CustomerRetentionRecord> customerRecords
                = new HashMap<>();

        String sql = """
        SELECT
            c.customer_id,
            c.full_name,
            c.email,
            c.phone,
            b.booking_date
        FROM customer c
        JOIN bookings b
            ON c.customer_id = b.customer_id
        WHERE UPPER(TRIM(b.status)) IN ('CONFIRMED', 'COMPLETED')
          AND NOT EXISTS (
              SELECT 1
              FROM data_retention_log drl
              WHERE drl.customer_id = c.customer_id
                AND UPPER(TRIM(drl.action_taken)) = 'ANONYMISED'
          )
        """;

        try (
                Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int customerId = resultSet.getInt("customer_id");

                String fullName = getStringOrBlank(resultSet, "full_name");
                String email = getStringOrBlank(resultSet, "email");
                String phone = getStringOrBlank(resultSet, "phone");

                CustomerRetentionRecord record
                        = customerRecords.get(customerId);

                if (record == null) {
                    record = new CustomerRetentionRecord(
                            customerId,
                            fullName,
                            email,
                            phone
                    );

                    customerRecords.put(customerId, record);
                }

                String bookingDateText = getStringOrBlank(
                        resultSet,
                        "booking_date"
                );

                LocalDate bookingDate = parseDateSafely(bookingDateText);

                record.updateLastOrderDate(bookingDate);
            }

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Unable to load latest valid customer order records.",
                    exception
            );
        }

        return customerRecords;
    }

    /**
     * Removes the customer's personal details from the customer table.
     *
     * The customer record is updated rather than deleted because existing
     * bookings may still reference the customer through customer_id. This keeps
     * the booking history intact while removing personal contact information.
     *
     * The customer's full name is replaced with a non-identifying value that
     * still includes the customer ID, such as "Anonymised Customer 42". This
     * allows anonymised records to remain distinguishable in the system without
     * storing the customer's real name. The email address and phone number are
     * set to null because the system no longer holds those personal details
     * after anonymisation.
     *
     * @param connection the active database connection used for the current
     * transaction
     * @param customerId the database identifier of the customer to anonymise
     * @throws SQLException if the customer record cannot be updated
     */
    private void anonymiseCustomerRecord(
            Connection connection,
            int customerId
    ) throws SQLException {

        String sql = """
                UPDATE customer
                SET
                    full_name = ?,
                    email = NULL,
                    phone = NULL
                WHERE customer_id = ?
                """;

        String anonymisedName = "Anonymised Customer " + customerId;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, anonymisedName);
            statement.setInt(2, customerId);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                throw new SQLException(
                        "No customer record was found for ID "
                        + customerId + "."
                );
            }
        }
    }

    /**
     * Removes copied customer personal details from linked website booking
     * requests.
     *
     * Website booking requests store their own copy of customer information,
     * such as full name, email address, phone number and delivery address. This
     * method only updates booking requests that have already been linked to a
     * booking, because the booking_id can be matched to the bookings table and
     * then to the selected customer_id.
     *
     * Unconfirmed booking requests that do not have a booking_id are not
     * updated by this method because there is no reliable customer_id
     * relationship to prove that they belong to the selected customer.
     *
     * @param connection the active database connection used for the current
     * transaction
     * @param customerId the database identifier of the customer being
     * anonymised
     * @throws SQLException if linked booking request data cannot be updated
     */
    private void anonymiseLinkedBookingRequestData(
            Connection connection,
            int customerId
    ) throws SQLException {

        String sql = """
                UPDATE booking_request
                SET
                    full_name = ?,
                    email = NULL,
                    phone = NULL,
                    dropoff_address = NULL
                WHERE booking_id IN (
                    SELECT booking_id
                    FROM bookings
                    WHERE customer_id = ?
                )
                """;

        String anonymisedName = "Anonymised Customer " + customerId;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, anonymisedName);
            statement.setInt(2, customerId);
            statement.executeUpdate();
        }
    }

    /**
     * Records that a customer's personal data has been anonymised.
     *
     * The audit log supports accountability by recording which customer ID was
     * anonymised, what action was taken and when it happened. The action is
     * always recorded as ANONYMISED because this method is only used for the
     * customer data retention anonymisation process.
     *
     * The log deliberately avoids storing the customer's original name, email
     * address or phone number, because keeping those details would undermine
     * the purpose of anonymisation.
     *
     * @param connection the active database connection used for the current
     * transaction
     * @param customerId the database identifier of the anonymised customer
     * @throws SQLException if the retention log record cannot be inserted
     */
    private void insertDataRetentionLog(
            Connection connection,
            int customerId
    ) throws SQLException {

        String sql = """
                INSERT INTO data_retention_log (
                    customer_id,
                    action_taken,
                    action_date,
                    notes
                )
                VALUES (?, ?, ?, ?)
                """;

        String actionDate = LocalDate.now().toString();

        String notes = "Customer personal data anonymised under the 7-year "
                + "data retention rule.";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            statement.setString(2, "ANONYMISED");
            statement.setString(3, actionDate);
            statement.setString(4, notes);

            statement.executeUpdate();
        }
    }

    /**
     * Gets a string value from a result set and returns blank text if the
     * database value is null.
     *
     * This is useful when displaying optional customer details such as email or
     * phone number, because the table can show blank text instead of null.
     *
     * @param resultSet the result set being read
     * @param columnName the column name to read
     * @return the column value, or blank text if the value is null
     * @throws SQLException if the column cannot be read
     */
    private String getStringOrBlank(ResultSet resultSet, String columnName)
            throws SQLException {

        String value = resultSet.getString(columnName);

        if (value == null) {
            return "";
        }

        return value;
    }

    /**
     * Parses a booking date from the database.
     *
     * The project may store dates in British format, such as dd/MM/yyyy, or in
     * ISO database format, such as yyyy-MM-dd. This method tries both formats
     * and returns null if the date is blank or cannot be parsed.
     *
     * Returning null is deliberate because one invalid legacy date should not
     * stop the whole data retention review screen from loading.
     *
     * @param dateText the date text from the database
     * @return the parsed date, or null if the date is missing or invalid
     */
    private LocalDate parseDateSafely(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return null;
        }

        String cleanedDateText = dateText.trim();

        try {
            return LocalDate.parse(cleanedDateText, BRITISH_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            // Try ISO database format below.
        }

        try {
            return LocalDate.parse(cleanedDateText, DATABASE_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * Stores temporary customer information while the DAO works out the
     * customer's latest confirmed or completed order date.
     *
     * This helper class is only used inside DataRetentionDAO while processing
     * query results. It is not used directly by the JavaFX interface.
     *
     * The customer details are final because they are set once when the
     * customer is first found. The last order date is not final because it may
     * need to be updated if another booking row contains a newer valid order
     * date.
     */
    private static class CustomerRetentionRecord {

        private final int customerId;
        private final String fullName;
        private final String email;
        private final String phone;

        private LocalDate lastOrderDate;

        /**
         * Creates a temporary customer retention record.
         *
         * @param customerId the database identifier for the customer
         * @param fullName the customer's full name
         * @param email the customer's email address
         * @param phone the customer's telephone number
         */
        private CustomerRetentionRecord(
                int customerId,
                String fullName,
                String email,
                String phone
        ) {
            this.customerId = customerId;
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
        }

        /**
         * Updates the stored last order date if the supplied booking date is
         * newer.
         *
         * This is needed because the SQL query may return several confirmed or
         * completed bookings for the same customer. The DAO only needs to keep
         * the most recent valid order date.
         *
         * @param bookingDate the booking date to compare with the stored date
         */
        private void updateLastOrderDate(LocalDate bookingDate) {
            if (bookingDate == null) {
                return;
            }

            if (lastOrderDate == null
                    || bookingDate.isAfter(lastOrderDate)) {

                lastOrderDate = bookingDate;
            }
        }

        /**
         * Gets the customer's database identifier.
         *
         * @return the customer ID
         */
        private int getCustomerId() {
            return customerId;
        }

        /**
         * Gets the customer's full name.
         *
         * @return the customer's full name
         */
        private String getFullName() {
            return fullName;
        }

        /**
         * Gets the customer's email address.
         *
         * @return the customer's email address
         */
        private String getEmail() {
            return email;
        }

        /**
         * Gets the customer's telephone number.
         *
         * @return the customer's telephone number
         */
        private String getPhone() {
            return phone;
        }

        /**
         * Gets the latest valid order date found for this customer.
         *
         * @return the latest confirmed or completed order date
         */
        private LocalDate getLastOrderDate() {
            return lastOrderDate;
        }
    }
}
