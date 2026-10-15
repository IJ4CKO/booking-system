
package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.Customer;
 
/**
 * Data access object for the {customer} table.
 */
public class CustomerDAO {
 
    /**
     * Creates a customer, opening and closing its own connection.
     */
    public void addCustomer(
            Customer customer
    ) throws Exception {
 
        try (Connection conn =
                     DBConnection.getConnection()) {
 
            addCustomer(conn, customer);
        }
    }
 
    /**
     * Creates a customer using an existing database transaction.
     */
    public void addCustomer(
            Connection conn,
            Customer customer
    ) throws Exception {
 
        validateConnectionAndCustomer(
                conn,
                customer
        );
 
        ensureNoDuplicateCustomer(
                conn,
                customer.getEmail(),
                customer.getPhone()
        );
 
        String sql = """
            INSERT INTO customer (
                full_name,
                phone,
                email
            )
            VALUES (?, ?, ?)
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    safeText(customer.getFullName())
            );
 
            statement.setString(
                    2,
                    blankToNull(customer.getPhone())
            );
 
            statement.setString(
                    3,
                    blankToNull(customer.getEmail())
            );
 
            int rowsInserted =
                    statement.executeUpdate();
 
            if (rowsInserted != 1) {
                throw new IllegalStateException(
                        "Unable to create the customer."
                );
            }
        }
    }
 
    /**
     * Creates a customer and returns their generated identifier, opening and
     * closing its own connection.
     */
    public int addCustomerAndReturnId(
            Customer customer
    ) throws Exception {
 
        try (Connection conn =
                     DBConnection.getConnection()) {
 
            return addCustomerAndReturnId(
                    conn,
                    customer
            );
        }
    }
 
    /**
     * Creates a customer using an existing database transaction
     * and returns the generated customer ID.
     */
    public int addCustomerAndReturnId(
            Connection conn,
            Customer customer
    ) throws Exception {
 
        validateConnectionAndCustomer(
                conn,
                customer
        );
 
        ensureNoDuplicateCustomer(
                conn,
                customer.getEmail(),
                customer.getPhone()
        );
 
        String sql = """
            INSERT INTO customer (
                full_name,
                phone,
                email
            )
            VALUES (?, ?, ?)
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(
                             sql,
                             PreparedStatement
                                     .RETURN_GENERATED_KEYS
                     )) {
 
            statement.setString(
                    1,
                    safeText(customer.getFullName())
            );
 
            statement.setString(
                    2,
                    blankToNull(customer.getPhone())
            );
 
            statement.setString(
                    3,
                    blankToNull(customer.getEmail())
            );
 
            int rowsInserted =
                    statement.executeUpdate();
 
            if (rowsInserted != 1) {
                throw new IllegalStateException(
                        "Unable to create the customer."
                );
            }
 
            try (ResultSet results =
                         statement.getGeneratedKeys()) {
 
                if (results.next()) {
                    return results.getInt(1);
                }
            }
        }
 
        throw new IllegalStateException(
                "The new customer ID could not be obtained."
        );
    }
 
    /**
     * Reports whether anyone is already on file under the given email address
     * or telephone number.
     */
    public boolean customerExistsByEmailOrPhone(
            String email,
            String phone
    ) throws Exception {
 
        try (Connection conn =
                     DBConnection.getConnection()) {
 
            return findCustomerByEmailOrPhone(
                    conn,
                    email,
                    phone
            ) != null;
        }
    }
 
    /**
     * Finds a customer by email address or telephone number, opening and
     * closing its own connection.
     */
    public Customer findCustomerByEmailOrPhone(
            String email,
            String phone
    ) throws Exception {
 
        try (Connection conn =
                     DBConnection.getConnection()) {
 
            return findCustomerByEmailOrPhone(
                    conn,
                    email,
                    phone
            );
        }
    }
 
    /**
     * Finds a customer by email address or telephone number, using a
     * connection supplied by the caller.
     */
    public Customer findCustomerByEmailOrPhone(
            Connection conn,
            String email,
            String phone
    ) throws Exception {
 
        if (conn == null) {
            throw new IllegalArgumentException(
                    "A database connection is required."
            );
        }
 
        String cleanedEmail =
                blankToNull(email);
 
        String cleanedPhone =
                blankToNull(phone);
 
        if (cleanedEmail == null && cleanedPhone == null) {
            return null;
        }
 
        String sql = """
            SELECT
                customer_id,
                full_name,
                phone,
                email
            FROM customer
            WHERE (
                ? IS NOT NULL
                AND LOWER(TRIM(email)) = LOWER(TRIM(?))
            )
            OR (
                ? IS NOT NULL
                AND TRIM(phone) = TRIM(?)
            )
            LIMIT 1
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    cleanedEmail
            );
 
            statement.setString(
                    2,
                    cleanedEmail
            );
 
            statement.setString(
                    3,
                    cleanedPhone
            );
 
            statement.setString(
                    4,
                    cleanedPhone
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (results.next()) {
                    return mapCustomer(results);
                }
            }
        }
 
        return null;
    }
 
    /**
     * Guard clause used before creating a customer: throws if either contact
     * detail already belongs to somebody.
     */
    private void ensureNoDuplicateCustomer(
            Connection conn,
            String email,
            String phone
    ) throws Exception {
 
        Customer existingCustomer =
                findCustomerByEmailOrPhone(
                        conn,
                        email,
                        phone
                );
 
        if (existingCustomer == null) {
            return;
        }
 
        throw new IllegalArgumentException(
                "A customer with this email address or phone number "
                        + "already exists: "
                        + existingCustomer.getFullName()
        );
    }
 
    /**
     * Finds a customer whose name, telephone number and email all match,
     * opening and closing its own connection.
     */
    public Customer getCustomerByDetails(
            String fullName,
            String phone,
            String email
    ) throws Exception {
 
        try (Connection conn =
                     DBConnection.getConnection()) {
 
            return getCustomerByDetails(
                    conn,
                    fullName,
                    phone,
                    email
            );
        }
    }
 
    /**
     * Searches for a matching customer using an existing
     * database transaction.
     */
    public Customer getCustomerByDetails(
            Connection conn,
            String fullName,
            String phone,
            String email
    ) throws Exception {
 
        if (conn == null) {
            throw new IllegalArgumentException(
                    "A database connection is required."
            );
        }
 
        String sql = """
            SELECT
                customer_id,
                full_name,
                phone,
                email
            FROM customer
            WHERE LOWER(TRIM(full_name)) =
                  LOWER(TRIM(?))
              AND TRIM(COALESCE(phone, '')) =
                  TRIM(COALESCE(?, ''))
              AND LOWER(
                    TRIM(COALESCE(email, ''))
                  ) =
                  LOWER(
                    TRIM(COALESCE(?, ''))
                  )
            LIMIT 1
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    safeText(fullName)
            );
 
            statement.setString(
                    2,
                    safeText(phone)
            );
 
            statement.setString(
                    3,
                    safeText(email)
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (results.next()) {
                    return mapCustomer(results);
                }
            }
        }
 
        return null;
    }
 
    /**
     * Retrieves every customer, oldest first.
     */
    public List<Customer> getAllCustomers()
            throws Exception {
 
        List<Customer> customers =
                new ArrayList<>();
 
        String sql = """
            SELECT
                customer_id,
                full_name,
                phone,
                email
            FROM customer
            ORDER BY customer_id
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql);
             ResultSet results =
                     statement.executeQuery()) {
 
            while (results.next()) {
                customers.add(
                        mapCustomer(results)
                );
            }
        }
 
        return customers;
    }
 
    /**
     * Retrieves a single customer by their identifier.
     */
    public Customer getCustomerById(
            int customerId
    ) throws Exception {
 
        String sql = """
            SELECT
                customer_id,
                full_name,
                phone,
                email
            FROM customer
            WHERE customer_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    customerId
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (results.next()) {
                    return mapCustomer(results);
                }
            }
        }
 
        return null;
    }
 
    /**
     * Permanently removes a customer.
     */
    public void deleteCustomer(
            int customerId
    ) throws Exception {
 
        String sql = """
            DELETE FROM customer
            WHERE customer_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    customerId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Checks that the minimum details needed to save a customer are present.
     */
    private void validateConnectionAndCustomer(
            Connection conn,
            Customer customer
    ) {
        if (conn == null) {
            throw new IllegalArgumentException(
                    "A database connection is required."
            );
        }
 
        if (customer == null) {
            throw new IllegalArgumentException(
                    "A customer is required."
            );
        }
 
        if (safeText(
                customer.getFullName()
        ).isBlank()) {
 
            throw new IllegalArgumentException(
                    "The customer name is required."
            );
        }
    }
 
    /**
     * Builds a {Customer} from the current row of a result set.
     */
    private Customer mapCustomer(
            ResultSet results
    ) throws Exception {
 
        return new Customer(
                results.getInt("customer_id"),
                results.getString("full_name"),
                results.getString("phone"),
                results.getString("email")
        );
    }
 
    /**
     * Normalises a possibly {null} value into a trimmed string, so that
     * blank checks and comparisons can be made without a null test at every
     * call site.
     */
    private String safeText(
            String value
    ) {
        return value == null ? "" : value.trim();
    }
 
    /**
     * Normalises a possibly blank value the other way, to {null}.
     */
    private String blankToNull(
            String value
    ) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}