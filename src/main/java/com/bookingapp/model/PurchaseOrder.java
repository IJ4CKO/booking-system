package com.bookingapp.model;

/**
 * Represents a purchase order in the booking and inventory system.
 *
 * A purchase order records stock ordered from a supplier. It stores the
 * supplier, order date, expected delivery date, status, total cost and any
 * additional notes.
 */
public class PurchaseOrder {

    private int purchaseOrderId;
    private int supplierId;
    private String orderDate;
    private String expectedDeliveryDate;
    private String status;
    private Double totalCost;
    private String notes;

    public PurchaseOrder() {
    }

    /**
     * Creates a purchase order with all stored details.
     *
     * @param purchaseOrderId     the database identifier for the purchase order
     * @param supplierId          the supplier linked to the purchase order
     * @param orderDate           the date the purchase order was created
     * @param expectedDeliveryDate the expected delivery date for the order
     * @param status              the current status of the purchase order
     * @param totalCost           the total cost of the purchase order, or null if not recorded
     * @param notes               any additional notes about the purchase order
     */
    public PurchaseOrder(int purchaseOrderId, int supplierId, String orderDate,
                         String expectedDeliveryDate, String status,
                         Double totalCost, String notes) {
        this.purchaseOrderId = purchaseOrderId;
        this.supplierId = supplierId;
        this.orderDate = orderDate;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.status = status;
        this.totalCost = totalCost;
        this.notes = notes;
    }

    /**
     * Gets the purchase order database identifier.
     *
     * @return the purchase order ID
     */
    public int getPurchaseOrderId() {
        return purchaseOrderId;
    }

    /**
     * Sets the purchase order database identifier.
     *
     * @param purchaseOrderId the purchase order ID to store
     */
    public void setPurchaseOrderId(int purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    /**
     * Gets the supplier linked to this purchase order.
     *
     * @return the supplier ID
     */
    public int getSupplierId() {
        return supplierId;
    }

    /**
     * Sets the supplier linked to this purchase order.
     *
     * @param supplierId the supplier ID to store
     */
    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    /**
     * Gets the date the purchase order was created.
     *
     * @return the order date
     */
    public String getOrderDate() {
        return orderDate;
    }

    /**
     * Sets the date the purchase order was created.
     *
     * @param orderDate the order date to store
     */
    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    /**
     * Gets the expected delivery date.
     *
     * @return the expected delivery date
     */
    public String getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    /**
     * Sets the expected delivery date.
     *
     * @param expectedDeliveryDate the expected delivery date to store
     */
    public void setExpectedDeliveryDate(String expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    /**
     * Gets the current purchase order status.
     *
     * @return the purchase order status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current purchase order status.
     *
     * @param status the status to store
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the total cost of the purchase order.
     *
     * @return the total cost, or null if not recorded
     */
    public Double getTotalCost() {
        return totalCost;
    }

    /**
     * Sets the total cost of the purchase order.
     *
     * @param totalCost the total cost to store, or null if not recorded
     */
    public void setTotalCost(Double totalCost) {
        this.totalCost = totalCost;
    }

    /**
     * Gets any additional notes about the purchase order.
     *
     * @return the purchase order notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets any additional notes about the purchase order.
     *
     * @param notes the notes to store
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }
}