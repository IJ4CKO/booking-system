package com.bookingapp.model;
 
/**
 * A confirmed booking: who it is for, when it is wanted, and what state it is
 * in.
 */
public class Booking {
 
    /** Database identifier; zero until the booking has been saved. */
    private int bookingId;
 
    /** The customer the booking is for. */
    private int customerId;
 
    /** The member of staff who took the booking. */
    private int ownerUserId;
 
    /** Date the order is to be collected, as {@code YYYY-MM-DD}. */
    private String collectionDate;
 
    /** Time the order is to be collected, as {@code HH:MM}. */
    private String collectionTime;
 
    /** Date of delivery, where the order is being delivered rather than collected. */
    private String dropoffDate;
 
    /** Time of delivery, where the order is being delivered rather than collected. */
    private String dropoffTime;
 
    /** Where the order is to be delivered; unset for a collection. */
    private String dropoffAddress;
 
    /** Free-text note of what the customer asked for. */
    private String itemsRequired;
 
    /** Where the booking has reached, for example whether it has been cancelled. */
    private String status;
 
    /** Display only: the customer's name, filled in by queries that join to the customer. */
    private String customerName;
 
    /** Display only: a readable summary of the products ordered and their quantities. */
    private String productQuantitiesSummary;
 
    public Booking() {
    }
 
    /**
     * Creates a booking from its stored columns.
     */
    public Booking(int bookingId, int customerId, int ownerUserId,
            String collectionDate, String collectionTime,
            String dropoffDate, String dropoffTime, String dropoffAddress,
            String itemsRequired, String status) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.ownerUserId = ownerUserId;
        this.collectionDate = collectionDate;
        this.collectionTime = collectionTime;
        this.dropoffDate = dropoffDate;
        this.dropoffTime = dropoffTime;
        this.dropoffAddress = dropoffAddress;
        this.itemsRequired = itemsRequired;
        this.status = status;
    }
 
    /**
     * Creates a booking from its stored columns, together with the customer's
     * name.
     */
    public Booking(int bookingId, int customerId, int ownerUserId,
            String collectionDate, String collectionTime,
            String dropoffDate, String dropoffTime, String dropoffAddress,
            String itemsRequired, String status, String customerName) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.ownerUserId = ownerUserId;
        this.collectionDate = collectionDate;
        this.collectionTime = collectionTime;
        this.dropoffDate = dropoffDate;
        this.dropoffTime = dropoffTime;
        this.dropoffAddress = dropoffAddress;
        this.itemsRequired = itemsRequired;
        this.status = status;
        this.customerName = customerName;
    }
 
    public int getBookingId() {
        return bookingId;
    }
 
    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getCustomerId() {
        return customerId;
    }
 
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }
 
    public int getOwnerUserId() {
        return ownerUserId;
    }
 
    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
 
    public String getCollectionDate() {
        return collectionDate;
    }
 
    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }
 
    public String getCollectionTime() {
        return collectionTime;
    }

    public void setCollectionTime(String collectionTime) {
        this.collectionTime = collectionTime;
    }
 
    public String getDropoffDate() {
        return dropoffDate;
    }

    public void setDropoffDate(String dropoffDate) {
        this.dropoffDate = dropoffDate;
    }

    public String getDropoffTime() {
        return dropoffTime;
    }
 
    public void setDropoffTime(String dropoffTime) {
        this.dropoffTime = dropoffTime;
    }
 
    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }
 
    public String getItemsRequired() {
        return itemsRequired;
    }
 
    public void setItemsRequired(String itemsRequired) {
        this.itemsRequired = itemsRequired;
    }

    public String getStatus() {
        return status;
    }
 
    public void setStatus(String status) {
        this.status = status;
    }
 
    /**
     * Returns the customer's name, for display.
     */
    public String getCustomerName() {
        return customerName;
    }
 
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
 
    /**
     * Returns a readable summary of the products ordered and their quantities.
     */
    public String getProductQuantitiesSummary() {
        return productQuantitiesSummary;
    }

    public void setProductQuantitiesSummary(
            String productQuantitiesSummary
    ) {
        this.productQuantitiesSummary
                = productQuantitiesSummary;
    }
 
    public String getBookingDate() {
        return collectionDate;
    }
 
    public void setBookingDate(String bookingDate) {
        this.collectionDate = bookingDate;
    }
 
    public String getBookingTime() {
        return collectionTime;
    }

    public void setBookingTime(String bookingTime) {
        this.collectionTime = bookingTime;
    }
}