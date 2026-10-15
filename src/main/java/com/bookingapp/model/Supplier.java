package com.bookingapp.model;

/**
 * Represents a supplier stored in the booking and inventory system.
 *
 * A supplier record stores the supplier's name, contact details and notes.
 * Suppliers can be linked to inventory batches, inventory items and purchase
 * orders to help manage stock ordering.
 */
public class Supplier {

    private int supplierId;
    private String supplierName;
    private String contactName;
    private String phone;
    private String email;
    private String notes;
    
    public Supplier() {
    }

    /**
     * Creates a supplier with all stored details.
     *
     * @param supplierId   the database identifier for the supplier
     * @param supplierName the name of the supplier
     * @param contactName  the name of the contact person at the supplier
     * @param phone        the supplier's telephone number
     * @param email        the supplier's email address
     * @param notes        any additional notes about the supplier
     */
    public Supplier(int supplierId, String supplierName, String contactName,
                    String phone, String email, String notes) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.contactName = contactName;
        this.phone = phone;
        this.email = email;
        this.notes = notes;
    }

    /**
     * Gets the supplier database identifier.
     *
     * @return the supplier ID
     */
    public int getSupplierId() {
        return supplierId;
    }

    /**
     * Sets the supplier database identifier.
     *
     * @param supplierId the supplier ID to store
     */
    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    /**
     * Gets the supplier name.
     *
     * @return the supplier name
     */
    public String getSupplierName() {
        return supplierName;
    }

    /**
     * Sets the supplier name.
     *
     * @param supplierName the supplier name to store
     */
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    /**
     * Gets the supplier contact name.
     *
     * @return the contact name
     */
    public String getContactName() {
        return contactName;
    }

    /**
     * Sets the supplier contact name.
     *
     * @param contactName the contact name to store
     */
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    /**
     * Gets the supplier telephone number.
     *
     * @return the supplier telephone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the supplier telephone number.
     *
     * @param phone the telephone number to store
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Gets the supplier email address.
     *
     * @return the supplier email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the supplier email address.
     *
     * @param email the email address to store
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets any additional notes about the supplier.
     *
     * @return the supplier notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets any additional notes about the supplier.
     *
     * @param notes the notes to store
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returns a readable version of the supplier.
     *
     * This is used when a supplier is displayed in controls such as combo boxes
     * or tables, so the user can identify the supplier more easily.
     *
     * @return the supplier name
     */
    @Override
    public String toString() {
        return supplierName;
    }
}