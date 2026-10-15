package com.bookingapp.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Represents one customer row in the data retention review list.
 *
 * A data retention customer is not used to edit customer details. It is created
 * from a database query result and shown to the business owner so they can see
 * which customers may need their personal data reviewed or anonymised.
 */
public class DataRetentionCustomer {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final int customerId;
    private final String fullName;
    private final String email;
    private final String phone;
    private final LocalDate lastOrderDate;
    private final int yearsSinceLastOrder;
    private final String recommendedAction;

    /**
     * Creates a data retention customer with all details needed for review.
     *
     * @param customerId          the database identifier for the customer
     * @param fullName            the customer's full name
     * @param email               the customer's email address
     * @param phone               the customer's hone number
     * @param lastOrderDate       the date of the customer's most recent order
     * @param yearsSinceLastOrder how many years have passed since the last order
     * @param recommendedAction   the action recommended for the owner to review
     */
    public DataRetentionCustomer(
            int customerId,
            String fullName,
            String email,
            String phone,
            LocalDate lastOrderDate,
            int yearsSinceLastOrder,
            String recommendedAction
    ) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.lastOrderDate = lastOrderDate;
        this.yearsSinceLastOrder = yearsSinceLastOrder;
        this.recommendedAction = recommendedAction;
    }

    /**
     * Gets the customer's database identifier.
     *
     * @return the customer ID
     */
    public int getCustomerId() {
        return customerId;
    }

    /**
     * Gets the customer's full name.
     *
     * @return the customer's full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Gets the customer's email address.
     *
     * @return the customer's email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the customer's phone number.
     *
     * @return the customer's phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Gets the customer's most recent order date.
     *
     * @return the last order date
     */
    public LocalDate getLastOrderDate() {
        return lastOrderDate;
    }

    /**
     * Gets the customer's most recent order date in British date format.
     *
     * This is useful when displaying the date in the Data Retention table or in
     * confirmation messages.
     *
     * @return the last order date formatted as dd/MM/yyyy
     */
    public String getFormattedLastOrderDate() {
        if (lastOrderDate == null) {
            return "";
        }

        return lastOrderDate.format(DISPLAY_DATE_FORMAT);
    }

    /**
     * Gets how many years have passed since the customer's last order.
     *
     * @return the number of years since the last order
     */
    public int getYearsSinceLastOrder() {
        return yearsSinceLastOrder;
    }

    /**
     * Gets the recommended data retention action.
     *
     * @return the recommended action
     */
    public String getRecommendedAction() {
        return recommendedAction;
    }
}