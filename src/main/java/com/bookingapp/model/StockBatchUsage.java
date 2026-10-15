package com.bookingapp.model;

/**
 * Represents a stock batch usage record in the booking and inventory system.
 *
 * A stock batch usage record stores details about stock that has been used
 * from a specific inventory batch. It can be linked to a booking when stock
 * is used to fulfil an order, and it records the quantity used, unit cost and
 * date of use.
 */
public class StockBatchUsage {

    private int batchUsageId;
    private int batchId;
    private Integer bookingId;
    private int inventoryItemId;
    private double quantityUsed;
    private double unitCost;
    private String usageDate;

    public StockBatchUsage() {
    }

    /**
     * Creates a stock batch usage record with all stored details.
     *
     * @param batchUsageId   the database identifier for the stock batch usage record
     * @param batchId        the inventory batch linked to this usage record
     * @param bookingId      the booking linked to this usage record, or null if not linked to a booking
     * @param inventoryItemId the inventory item linked to this usage record
     * @param quantityUsed   the quantity of stock used from the batch
     * @param unitCost       the cost per unit at the time the stock was used
     * @param usageDate      the date the stock was used
     */
    public StockBatchUsage(int batchUsageId, int batchId, Integer bookingId, int inventoryItemId,
                           double quantityUsed, double unitCost, String usageDate) {
        this.batchUsageId = batchUsageId;
        this.batchId = batchId;
        this.bookingId = bookingId;
        this.inventoryItemId = inventoryItemId;
        this.quantityUsed = quantityUsed;
        this.unitCost = unitCost;
        this.usageDate = usageDate;
    }

    /**
     * Gets the stock batch usage database identifier.
     *
     * @return the stock batch usage ID
     */
    public int getBatchUsageId() {
        return batchUsageId;
    }

    /**
     * Sets the stock batch usage database identifier.
     *
     * @param batchUsageId the stock batch usage ID to store
     */
    public void setBatchUsageId(int batchUsageId) {
        this.batchUsageId = batchUsageId;
    }

    /**
     * Gets the inventory batch linked to this usage record.
     *
     * @return the batch ID
     */
    public int getBatchId() {
        return batchId;
    }

    /**
     * Sets the inventory batch linked to this usage record.
     *
     * @param batchId the batch ID to store
     */
    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    /**
     * Gets the booking linked to this usage record.
     *
     * @return the booking ID, or null if the usage record is not linked to a booking
     */
    public Integer getBookingId() {
        return bookingId;
    }

    /**
     * Sets the booking linked to this usage record.
     *
     * @param bookingId the booking ID to store, or null if the usage record is not linked to a booking
     */
    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }

    /**
     * Gets the inventory item linked to this usage record.
     *
     * @return the inventory item ID
     */
    public int getInventoryItemId() {
        return inventoryItemId;
    }

    /**
     * Sets the inventory item linked to this usage record.
     *
     * @param inventoryItemId the inventory item ID to store
     */
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /**
     * Gets the quantity of stock used from the batch.
     *
     * @return the quantity used
     */
    public double getQuantityUsed() {
        return quantityUsed;
    }

    /**
     * Sets the quantity of stock used from the batch.
     *
     * @param quantityUsed the quantity used to store
     */
    public void setQuantityUsed(double quantityUsed) {
        this.quantityUsed = quantityUsed;
    }

    /**
     * Gets the cost per unit at the time the stock was used.
     *
     * @return the unit cost
     */
    public double getUnitCost() {
        return unitCost;
    }

    /**
     * Sets the cost per unit at the time the stock was used.
     *
     * @param unitCost the unit cost to store
     */
    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }

    /**
     * Gets the date the stock was used.
     *
     * @return the usage date
     */
    public String getUsageDate() {
        return usageDate;
    }

    /**
     * Sets the date the stock was used.
     *
     * @param usageDate the usage date to store
     */
    public void setUsageDate(String usageDate) {
        this.usageDate = usageDate;
    }

    /**
     * Calculates the value of the stock used.
     *
     * This is calculated by multiplying the quantity used by the unit cost.
     *
     * @return the total value of the stock used
     */
    public double getUsageValue() {
        return quantityUsed * unitCost;
    }
}