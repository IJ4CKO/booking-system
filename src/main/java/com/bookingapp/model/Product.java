package com.bookingapp.model;

/**
 * Represents a product offered by the business.
 *
 * A product can be linked to bookings, booking lines and recipe items.
 * The active field is used to show whether the product is currently available
 * for use in the system.
 */
public class Product {

    private int productId;
    private String name;
    private String description;
    private boolean active;

    public Product() {
    }

    /**
     * Creates a product with all stored details.
     *
     * @param productId   the database identifier for the product
     * @param name        the product name
     * @param description the product description
     * @param active      whether the product is currently active
     */
    public Product(int productId, String name, String description, boolean active) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    /**
     * Gets the product database identifier.
     *
     * @return the product ID
     */
    public int getProductId() {
        return productId;
    }

    /**
     * Sets the product database identifier.
     *
     * @param productId the product ID to store
     */
    public void setProductId(int productId) {
        this.productId = productId;
    }

    /**
     * Gets the product name.
     *
     * @return the product name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the product name.
     *
     * @param name the product name to store
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the product description.
     *
     * @return the product description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the product description.
     *
     * @param description the product description to store
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Checks whether the product is active.
     *
     * @return true if the product is active, otherwise false
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the product is active.
     *
     * @param active true if the product is active, otherwise false
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns a readable version of the product.
     *
     * This is used when a product is displayed in controls such as combo boxes
     * or tables, so the user can identify the product more easily.
     *
     * @return the product ID and name
     */
    @Override
    public String toString() {
        return productId + " - " + name;
    }
}