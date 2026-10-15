package com.bookingapp.model;

/**
 * Represents an inventory item used within a product recipe.
 *
 * A recipe item links a product to an inventory item and records the quantity
 * required to make one unit of that product. This supports automatic stock
 * checks and stock deduction when bookings are confirmed.
 */
public class RecipeItem {

    private int recipeItemId;
    private int productId;
    private int inventoryItemId;
    private double quantityRequiredPerUnit;

    private String productName;
    private String inventoryItemName;

    public RecipeItem() {
    }

    /**
     * Creates a recipe item using the main stored details.
     *
     * @param recipeItemId           the database identifier for the recipe item
     * @param productId              the product linked to this recipe item
     * @param inventoryItemId        the inventory item required for the product
     * @param quantityRequiredPerUnit the quantity of the inventory item needed per product unit
     */
    public RecipeItem(int recipeItemId, int productId, int inventoryItemId, double quantityRequiredPerUnit) {
        this.recipeItemId = recipeItemId;
        this.productId = productId;
        this.inventoryItemId = inventoryItemId;
        this.quantityRequiredPerUnit = quantityRequiredPerUnit;
    }

    /**
     * Creates a recipe item with stored details and display names.
     *
     * The product name and inventory item name are used for displaying clearer
     * information in tables and screens.
     *
     * @param recipeItemId           the database identifier for the recipe item
     * @param productId              the product linked to this recipe item
     * @param inventoryItemId        the inventory item required for the product
     * @param quantityRequiredPerUnit the quantity of the inventory item needed per product unit
     * @param productName            the display name of the linked product
     * @param inventoryItemName      the display name of the linked inventory item
     */
    public RecipeItem(int recipeItemId, int productId, int inventoryItemId, double quantityRequiredPerUnit,
                      String productName, String inventoryItemName) {
        this.recipeItemId = recipeItemId;
        this.productId = productId;
        this.inventoryItemId = inventoryItemId;
        this.quantityRequiredPerUnit = quantityRequiredPerUnit;
        this.productName = productName;
        this.inventoryItemName = inventoryItemName;
    }

    /**
     * Gets the recipe item database identifier.
     *
     * @return the recipe item ID
     */
    public int getRecipeItemId() {
        return recipeItemId;
    }

    /**
     * Sets the recipe item database identifier.
     *
     * @param recipeItemId the recipe item ID to store
     */
    public void setRecipeItemId(int recipeItemId) {
        this.recipeItemId = recipeItemId;
    }

    /**
     * Gets the product linked to this recipe item.
     *
     * @return the product ID
     */
    public int getProductId() {
        return productId;
    }

    /**
     * Sets the product linked to this recipe item.
     *
     * @param productId the product ID to store
     */
    public void setProductId(int productId) {
        this.productId = productId;
    }

    /**
     * Gets the inventory item linked to this recipe item.
     *
     * @return the inventory item ID
     */
    public int getInventoryItemId() {
        return inventoryItemId;
    }

    /**
     * Sets the inventory item linked to this recipe item.
     *
     * @param inventoryItemId the inventory item ID to store
     */
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /**
     * Gets the quantity of the inventory item required per product unit.
     *
     * @return the quantity required per unit
     */
    public double getQuantityRequiredPerUnit() {
        return quantityRequiredPerUnit;
    }

    /**
     * Sets the quantity of the inventory item required per product unit.
     *
     * @param quantityRequiredPerUnit the quantity required per unit to store
     */
    public void setQuantityRequiredPerUnit(double quantityRequiredPerUnit) {
        this.quantityRequiredPerUnit = quantityRequiredPerUnit;
    }

    /**
     * Gets the product display name.
     *
     * @return the product name
     */
    public String getProductName() {
        return productName;
    }

    /**
     * Sets the product display name.
     *
     * @param productName the product name to store
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * Gets the inventory item display name.
     *
     * @return the inventory item name
     */
    public String getInventoryItemName() {
        return inventoryItemName;
    }

    /**
     * Sets the inventory item display name.
     *
     * @param inventoryItemName the inventory item name to store
     */
    public void setInventoryItemName(String inventoryItemName) {
        this.inventoryItemName = inventoryItemName;
    }
}