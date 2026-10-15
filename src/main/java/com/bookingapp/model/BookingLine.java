package com.bookingapp.model;
 
/**
 * A single product line on a booking: what was ordered, and how many.
 */
public class BookingLine {
 
    /** Database identifier; zero until the line has been saved. */
    private int bookingLineId;
 
    /** The booking this line belongs to; zero until it has been attached to one. */
    private int bookingId;
 
    /** The product ordered. */
    private int productId;
 
    /** How many of the product were ordered. */
    private int quantity;
 
    /** Display only: the product's name, filled in by queries that join to the catalogue. */
    private String productName;
 
    public BookingLine() {
    }
 
    /**
     * Creates a booking line from its stored columns.
     *
     * @param bookingLineId the database identifier, or zero if not yet saved
     * @param bookingId     the booking this line belongs to, or zero if not yet attached
     * @param productId     the product ordered
     * @param quantity      how many were ordered
     */
    public BookingLine(int bookingLineId, int bookingId, int productId, int quantity) {
        this.bookingLineId = bookingLineId;
        this.bookingId = bookingId;
        this.productId = productId;
        this.quantity = quantity;
    }
 
    /**
     * Creates a booking line together with the product's name.
     *
     * @param bookingLineId the database identifier
     * @param bookingId     the booking this line belongs to
     * @param productId     the product ordered
     * @param quantity      how many were ordered
     * @param productName   the product's name, for display
     */
    public BookingLine(int bookingLineId, int bookingId, int productId, int quantity, String productName) {
        this.bookingLineId = bookingLineId;
        this.bookingId = bookingId;
        this.productId = productId;
        this.quantity = quantity;
        this.productName = productName;
    }
 
    /**
     * @return the database identifier, or zero if the line has not been saved
     */
    public int getBookingLineId() {
        return bookingLineId;
    }
 
    /**
     * @param bookingLineId the database identifier
     */
    public void setBookingLineId(int bookingLineId) {
        this.bookingLineId = bookingLineId;
    }
 
    /**
     * @return the booking this line belongs to, or zero if it is not yet attached
     */
    public int getBookingId() {
        return bookingId;
    }
 
    /**
     * @param bookingId the booking this line belongs to
     */
    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
 
    /**
     * @return the product ordered
     */
    public int getProductId() {
        return productId;
    }
 
    /**
     * @param productId the product ordered
     */
    public void setProductId(int productId) {
        this.productId = productId;
    }
 
    /**
     * @return how many of the product were ordered
     */
    public int getQuantity() {
        return quantity;
    }
 
    /**
     * @param quantity how many of the product were ordered; must be greater than zero
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
 
    /**
     * Returns the product's name, for display.
     *
     * @return the product's name, or {@code null} if it was not fetched
     */
    public String getProductName() {
        return productName;
    }
 
    /**
     * @param productName the product's name, for display
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }
}