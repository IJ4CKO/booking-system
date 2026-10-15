package com.bookingapp.model;

/**
 * Represents a customer stored in the booking and inventory system.
 *
 * A customer record stores the customer's name, telephone number and email
 * address so that bookings and website requests can be linked to the correct
 * person.
 */
public class Customer {

    private int customerId;
    private String fullName;
    private String phone;
    private String email;

    public Customer() {
    }

    /**
     * Creates a customer with all stored details.
     *
     * @param customerId the database identifier for the customer
     * @param fullName   the customer's full name
     * @param phone      the customer's telephone number
     * @param email      the customer's email address
     */
    public Customer(int customerId, String fullName, String phone, String email) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
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
     * Sets the customer's database identifier.
     *
     * @param customerId the customer ID to store
     */
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
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
     * Sets the customer's full name.
     *
     * @param fullName the full name to store
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Gets the customer's telephone number.
     *
     * @return the customer's telephone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the customer's telephone number.
     *
     * @param phone the telephone number to store
     */
    public void setPhone(String phone) {
        this.phone = phone;
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
     * Sets the customer's email address.
     *
     * @param email the email address to store
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns a readable version of the customer.
     *
     * This is used when a customer is displayed in controls such as combo boxes,
     * so the user can identify the customer more easily.
     *
     * @return the customer ID and full name
     */
    @Override
    public String toString() {
        return customerId + " - " + fullName;
    }
}