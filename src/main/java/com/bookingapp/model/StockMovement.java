package com.bookingapp.model;

/**
 * Represents a stock movement record in the booking and inventory system.
 *
 * A stock movement records a change in the quantity of an inventory item.
 * This can be linked to a booking when stock is used or restored, and it helps
 * provide a history of stock increases, decreases and manual adjustments.
 */
public class StockMovement {

    private int stockMovementId;
    private int inventoryItemId;
    private String movementTime;
    private double quantityChange;
    private Integer bookingId;
    private String movementType;
    private String notes;

    public StockMovement() {
    }

    /**
     * Creates a stock movement record with all stored details.
     *
     * @param stockMovementId the database identifier for the stock movement record
     * @param inventoryItemId the inventory item linked to this stock movement
     * @param movementTime    the date and time the stock movement was recorded
     * @param quantityChange  the quantity change, where positive values increase stock and negative values decrease stock
     * @param bookingId       the booking linked to this stock movement, or null if not linked to a booking
     * @param movementType    the type of stock movement, such as booking use, cancellation, purchase or adjustment
     * @param notes           any additional notes about the stock movement
     */
    public StockMovement(int stockMovementId, int inventoryItemId, String movementTime,
                         double quantityChange, Integer bookingId, String movementType, String notes) {
        this.stockMovementId = stockMovementId;
        this.inventoryItemId = inventoryItemId;
        this.movementTime = movementTime;
        this.quantityChange = quantityChange;
        this.bookingId = bookingId;
        this.movementType = movementType;
        this.notes = notes;
    }

    /**
     * Gets the stock movement database identifier.
     *
     * @return the stock movement ID
     */
    public int getStockMovementId() {
        return stockMovementId;
    }

    /**
     * Sets the stock movement database identifier.
     *
     * @param stockMovementId the stock movement ID to store
     */
    public void setStockMovementId(int stockMovementId) {
        this.stockMovementId = stockMovementId;
    }

    /**
     * Gets the inventory item linked to this stock movement.
     *
     * @return the inventory item ID
     */
    public int getInventoryItemId() {
        return inventoryItemId;
    }

    /**
     * Sets the inventory item linked to this stock movement.
     *
     * @param inventoryItemId the inventory item ID to store
     */
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /**
     * Gets the date and time the stock movement was recorded.
     *
     * @return the movement time
     */
    public String getMovementTime() {
        return movementTime;
    }

    /**
     * Sets the date and time the stock movement was recorded.
     *
     * @param movementTime the movement time to store
     */
    public void setMovementTime(String movementTime) {
        this.movementTime = movementTime;
    }

    /**
     * Gets the quantity change for the stock movement.
     *
     * A positive value means stock was increased, while a negative value means
     * stock was decreased.
     *
     * @return the quantity change
     */
    public double getQuantityChange() {
        return quantityChange;
    }

    /**
     * Sets the quantity change for the stock movement.
     *
     * @param quantityChange the quantity change to store
     */
    public void setQuantityChange(double quantityChange) {
        this.quantityChange = quantityChange;
    }

    /**
     * Gets the booking linked to this stock movement.
     *
     * @return the booking ID, or null if the movement is not linked to a booking
     */
    public Integer getBookingId() {
        return bookingId;
    }

    /**
     * Sets the booking linked to this stock movement.
     *
     * @param bookingId the booking ID to store, or null if the movement is not linked to a booking
     */
    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }

    /**
     * Gets the type of stock movement.
     *
     * @return the stock movement type
     */
    public String getMovementType() {
        return movementType;
    }

    /**
     * Sets the type of stock movement.
     *
     * @param movementType the movement type to store
     */
    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    /**
     * Gets any additional notes about the stock movement.
     *
     * @return the stock movement notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets any additional notes about the stock movement.
     *
     * @param notes the notes to store
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Checks whether the stock movement increases stock.
     *
     * @return true if the quantity change is greater than zero
     */
    public boolean isIncrease() {
        return quantityChange > 0;
    }

    /**
     * Checks whether the stock movement decreases stock.
     *
     * @return true if the quantity change is less than zero
     */
    public boolean isDecrease() {
        return quantityChange < 0;
    }
}