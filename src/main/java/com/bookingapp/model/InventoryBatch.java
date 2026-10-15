package com.bookingapp.model;

/**
 * Represents an inventory batch stored in the booking and inventory system.
 *
 * An inventory batch records stock that has been purchased or added to the
 * system. It stores the inventory item, optional supplier, purchase date,
 * expiry date, quantities and unit cost. This supports stock tracking,
 * batch value calculation and FIFO-style stock management.
 */
public class InventoryBatch {

    private int batchId;
    private int inventoryItemId;
    private Integer supplierId;
    private String batchCode;
    private String purchaseDate;
    private String expiryDate;
    private double quantityReceived;
    private double quantityRemaining;
    private double unitCost;

    public InventoryBatch() {
    }

    /**
     * Creates an inventory batch with all stored details.
     *
     * @param batchId           the database identifier for the batch
     * @param inventoryItemId   the inventory item linked to this batch
     * @param supplierId        the supplier linked to this batch, or null if no supplier is recorded
     * @param batchCode         the batch code used to identify the stock batch
     * @param purchaseDate      the date the batch was purchased or received
     * @param expiryDate        the expiry date of the batch, or null if it does not expire
     * @param quantityReceived  the original quantity received for the batch
     * @param quantityRemaining the quantity still remaining in the batch
     * @param unitCost          the cost per unit for this batch
     */
    public InventoryBatch(int batchId, int inventoryItemId, Integer supplierId, String batchCode,
                          String purchaseDate, String expiryDate,
                          double quantityReceived, double quantityRemaining, double unitCost) {
        this.batchId = batchId;
        this.inventoryItemId = inventoryItemId;
        this.supplierId = supplierId;
        this.batchCode = batchCode;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.quantityReceived = quantityReceived;
        this.quantityRemaining = quantityRemaining;
        this.unitCost = unitCost;
    }

    /**
     * Gets the batch database identifier.
     *
     * @return the batch ID
     */
    public int getBatchId() {
        return batchId;
    }

    /**
     * Sets the batch database identifier.
     *
     * @param batchId the batch ID to store
     */
    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    /**
     * Gets the inventory item linked to this batch.
     *
     * @return the inventory item ID
     */
    public int getInventoryItemId() {
        return inventoryItemId;
    }

    /**
     * Sets the inventory item linked to this batch.
     *
     * @param inventoryItemId the inventory item ID to store
     */
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /**
     * Gets the supplier linked to this batch.
     *
     * @return the supplier ID, or null if no supplier is recorded
     */
    public Integer getSupplierId() {
        return supplierId;
    }

    /**
     * Sets the supplier linked to this batch.
     *
     * @param supplierId the supplier ID to store, or null if no supplier is recorded
     */
    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    /**
     * Gets the batch code.
     *
     * @return the batch code
     */
    public String getBatchCode() {
        return batchCode;
    }

    /**
     * Sets the batch code.
     *
     * @param batchCode the batch code to store
     */
    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    /**
     * Gets the purchase date.
     *
     * @return the purchase date
     */
    public String getPurchaseDate() {
        return purchaseDate;
    }

    /**
     * Sets the purchase date.
     *
     * @param purchaseDate the purchase date to store
     */
    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    /**
     * Gets the expiry date.
     *
     * @return the expiry date, or null if the batch does not expire
     */
    public String getExpiryDate() {
        return expiryDate;
    }

    /**
     * Sets the expiry date.
     *
     * @param expiryDate the expiry date to store, or null if the batch does not expire
     */
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    /**
     * Gets the original quantity received for this batch.
     *
     * @return the quantity received
     */
    public double getQuantityReceived() {
        return quantityReceived;
    }

    /**
     * Sets the original quantity received for this batch.
     *
     * @param quantityReceived the quantity received to store
     */
    public void setQuantityReceived(double quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    /**
     * Gets the quantity remaining in this batch.
     *
     * @return the quantity remaining
     */
    public double getQuantityRemaining() {
        return quantityRemaining;
    }

    /**
     * Sets the quantity remaining in this batch.
     *
     * @param quantityRemaining the quantity remaining to store
     */
    public void setQuantityRemaining(double quantityRemaining) {
        this.quantityRemaining = quantityRemaining;
    }

    /**
     * Gets the cost per unit for this batch.
     *
     * @return the unit cost
     */
    public double getUnitCost() {
        return unitCost;
    }

    /**
     * Sets the cost per unit for this batch.
     *
     * @param unitCost the unit cost to store
     */
    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }

    /**
     * Calculates the remaining value of the batch.
     *
     * This is calculated by multiplying the quantity remaining by the unit cost.
     *
     * @return the current value of the remaining stock in this batch
     */
    public double getBatchValue() {
        return quantityRemaining * unitCost;
    }

    /**
     * Returns a readable version of the batch.
     *
     * This is used when a batch is displayed in controls such as combo boxes
     * or tables. If a batch code exists, it is shown. Otherwise, the batch ID
     * is shown.
     *
     * @return the batch code, or the batch ID if no code is available
     */
    @Override
    public String toString() {
        return batchCode != null && !batchCode.isBlank()
                ? batchCode
                : "Batch " + batchId;
    }
}