package com.bookingapp.model;

/**
 * Represents an inventory waste record in the booking and inventory system.
 *
 * An inventory waste record stores details about stock that has been wasted,
 * expired or removed from use. It links the waste record to an inventory batch
 * and inventory item, and records the quantity wasted, reason, cost and notes.
 */
public class InventoryWaste {

    private int wasteId;
    private int batchId;
    private int inventoryItemId;
    private String wasteDate;
    private double quantityWasted;
    private String reason;
    private double unitCost;
    private double wasteValue;
    private String notes;

    public InventoryWaste() {
    }

    /**
     * Creates an inventory waste record with all stored details.
     *
     * @param wasteId         the database identifier for the waste record
     * @param batchId         the batch linked to the wasted stock
     * @param inventoryItemId the inventory item linked to the wasted stock
     * @param wasteDate       the date the stock was recorded as wasted
     * @param quantityWasted  the quantity of stock wasted
     * @param reason          the reason the stock was wasted
     * @param unitCost        the cost per unit of the wasted stock
     * @param wasteValue      the total value of the wasted stock
     * @param notes           any additional notes about the waste record
     */
    public InventoryWaste(
            int wasteId,
            int batchId,
            int inventoryItemId,
            String wasteDate,
            double quantityWasted,
            String reason,
            double unitCost,
            double wasteValue,
            String notes
    ) {
        this.wasteId = wasteId;
        this.batchId = batchId;
        this.inventoryItemId = inventoryItemId;
        this.wasteDate = wasteDate;
        this.quantityWasted = quantityWasted;
        this.reason = reason;
        this.unitCost = unitCost;
        this.wasteValue = wasteValue;
        this.notes = notes;
    }

    /**
     * Gets the waste record database identifier.
     *
     * @return the waste ID
     */
    public int getWasteId() {
        return wasteId;
    }

    /**
     * Sets the waste record database identifier.
     *
     * @param wasteId the waste ID to store
     */
    public void setWasteId(int wasteId) {
        this.wasteId = wasteId;
    }

    /**
     * Gets the batch linked to the waste record.
     *
     * @return the batch ID
     */
    public int getBatchId() {
        return batchId;
    }

    /**
     * Sets the batch linked to the waste record.
     *
     * @param batchId the batch ID to store
     */
    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    /**
     * Gets the inventory item linked to the waste record.
     *
     * @return the inventory item ID
     */
    public int getInventoryItemId() {
        return inventoryItemId;
    }

    /**
     * Sets the inventory item linked to the waste record.
     *
     * @param inventoryItemId the inventory item ID to store
     */
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /**
     * Gets the date the stock was recorded as wasted.
     *
     * @return the waste date
     */
    public String getWasteDate() {
        return wasteDate;
    }

    /**
     * Sets the date the stock was recorded as wasted.
     *
     * @param wasteDate the waste date to store
     */
    public void setWasteDate(String wasteDate) {
        this.wasteDate = wasteDate;
    }

    /**
     * Gets the quantity of stock wasted.
     *
     * @return the quantity wasted
     */
    public double getQuantityWasted() {
        return quantityWasted;
    }

    /**
     * Sets the quantity of stock wasted.
     *
     * @param quantityWasted the quantity wasted to store
     */
    public void setQuantityWasted(double quantityWasted) {
        this.quantityWasted = quantityWasted;
    }

    /**
     * Gets the reason for the waste.
     *
     * @return the waste reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the reason for the waste.
     *
     * @param reason the waste reason to store
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Gets the cost per unit of the wasted stock.
     *
     * @return the unit cost
     */
    public double getUnitCost() {
        return unitCost;
    }

    /**
     * Sets the cost per unit of the wasted stock.
     *
     * @param unitCost the unit cost to store
     */
    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }

    /**
     * Gets the total value of the wasted stock.
     *
     * @return the waste value
     */
    public double getWasteValue() {
        return wasteValue;
    }

    /**
     * Sets the total value of the wasted stock.
     *
     * @param wasteValue the waste value to store
     */
    public void setWasteValue(double wasteValue) {
        this.wasteValue = wasteValue;
    }

    /**
     * Gets any additional notes about the waste record.
     *
     * @return the waste notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets any additional notes about the waste record.
     *
     * @param notes the notes to store
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returns a readable version of the waste record.
     *
     * This is useful when waste records are displayed in controls such as
     * lists, combo boxes or tables.
     *
     * @return the waste reason, quantity wasted and waste date
     */
    @Override
    public String toString() {
        return reason + " — " + quantityWasted + " on " + wasteDate;
    }
}