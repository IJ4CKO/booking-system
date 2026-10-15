package com.bookingapp.model;

/**
 * Represents a line item within a purchase order.
 *
 * A purchase order line stores the inventory item being ordered, the quantity
 * ordered and the unit cost. This allows a purchase order to contain one or
 * more inventory items.
 */
public class PurchaseOrderLine {

    private int purchaseOrderLineId;
    private int purchaseOrderId;
    private int inventoryItemId;
    private double quantityOrdered;
    private double unitCost;

    public PurchaseOrderLine() {
    }

    /**
     * Creates a purchase order line with all stored details.
     *
     * @param purchaseOrderLineId the database identifier for the purchase order line
     * @param purchaseOrderId     the purchase order linked to this line
     * @param inventoryItemId     the inventory item being ordered
     * @param quantityOrdered     the quantity of the inventory item ordered
     * @param unitCost            the cost per unit for the inventory item
     */
    public PurchaseOrderLine(int purchaseOrderLineId, int purchaseOrderId, int inventoryItemId,
                             double quantityOrdered, double unitCost) {
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.purchaseOrderId = purchaseOrderId;
        this.inventoryItemId = inventoryItemId;
        this.quantityOrdered = quantityOrdered;
        this.unitCost = unitCost;
    }

    /**
     * Gets the purchase order line database identifier.
     *
     * @return the purchase order line ID
     */
    public int getPurchaseOrderLineId() {
        return purchaseOrderLineId;
    }

    /**
     * Sets the purchase order line database identifier.
     *
     * @param purchaseOrderLineId the purchase order line ID to store
     */
    public void setPurchaseOrderLineId(int purchaseOrderLineId) {
        this.purchaseOrderLineId = purchaseOrderLineId;
    }

    /**
     * Gets the purchase order linked to this line.
     *
     * @return the purchase order ID
     */
    public int getPurchaseOrderId() {
        return purchaseOrderId;
    }

    /**
     * Sets the purchase order linked to this line.
     *
     * @param purchaseOrderId the purchase order ID to store
     */
    public void setPurchaseOrderId(int purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    /**
     * Gets the inventory item being ordered.
     *
     * @return the inventory item ID
     */
    public int getInventoryItemId() {
        return inventoryItemId;
    }

    /**
     * Sets the inventory item being ordered.
     *
     * @param inventoryItemId the inventory item ID to store
     */
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /**
     * Gets the quantity ordered.
     *
     * @return the quantity ordered
     */
    public double getQuantityOrdered() {
        return quantityOrdered;
    }

    /**
     * Sets the quantity ordered.
     *
     * @param quantityOrdered the quantity ordered to store
     */
    public void setQuantityOrdered(double quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }

    /**
     * Gets the cost per unit for the ordered item.
     *
     * @return the unit cost
     */
    public double getUnitCost() {
        return unitCost;
    }

    /**
     * Sets the cost per unit for the ordered item.
     *
     * @param unitCost the unit cost to store
     */
    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }

    /**
     * Calculates the total cost for this purchase order line.
     *
     * This is calculated by multiplying the quantity ordered by the unit cost.
     *
     * @return the total cost for this line
     */
    public double getLineTotal() {
        return quantityOrdered * unitCost;
    }
}