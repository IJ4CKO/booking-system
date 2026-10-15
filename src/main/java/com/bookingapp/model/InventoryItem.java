package com.bookingapp.model;

/**
 * Represents an inventory item stored in the booking and inventory system.
 *
 * An inventory item records the stock used by the business, such as ingredients
 * or materials. It stores the current quantity, reorder level, supplier details,
 * unit costs and unit conversion information.
 */
public class InventoryItem {

    private int inventoryItemId;
    private String name;
    private String unit;
    private double currentQuantity;
    private double reorderLevel;
    private boolean active;

    private Integer preferredSupplierId;
    private Double defaultUnitCost;
    private Double reorderQuantity;

    private String purchaseUnit;
    private String recipeUnit;
    private double conversionFactor = 1.0;

    public InventoryItem() {
    }

    /**
     * Creates an inventory item using the core stock details.
     *
     * The purchase unit and recipe unit are set to the same value as the main
     * unit, and the conversion factor is set to 1.0 by default.
     *
     * @param inventoryItemId the database identifier for the inventory item
     * @param name            the name of the inventory item
     * @param unit            the main unit used for the item
     * @param currentQuantity the current stock quantity
     * @param reorderLevel    the quantity level where stock should be reordered
     * @param active          whether the inventory item is active
     */
    public InventoryItem(
            int inventoryItemId,
            String name,
            String unit,
            double currentQuantity,
            double reorderLevel,
            boolean active
    ) {
        this.inventoryItemId = inventoryItemId;
        this.name = name;
        this.unit = unit;
        this.currentQuantity = currentQuantity;
        this.reorderLevel = reorderLevel;
        this.active = active;
        this.purchaseUnit = unit;
        this.recipeUnit = unit;
        this.conversionFactor = 1.0;
    }

    /**
     * Creates an inventory item with all stored details.
     *
     * This constructor includes supplier, cost, reorder and unit conversion
     * information.
     *
     * @param inventoryItemId     the database identifier for the inventory item
     * @param name                the name of the inventory item
     * @param unit                the main unit used for the item
     * @param currentQuantity     the current stock quantity
     * @param reorderLevel        the quantity level where stock should be reordered
     * @param active              whether the inventory item is active
     * @param preferredSupplierId the preferred supplier for this item, or null if none is recorded
     * @param defaultUnitCost     the default cost per unit, or null if not recorded
     * @param reorderQuantity     the quantity normally reordered, or null if not recorded
     * @param purchaseUnit        the unit used when purchasing the item
     * @param recipeUnit          the unit used when the item is used in recipes
     * @param conversionFactor    the conversion factor between purchase and recipe units
     */
    public InventoryItem(
            int inventoryItemId,
            String name,
            String unit,
            double currentQuantity,
            double reorderLevel,
            boolean active,
            Integer preferredSupplierId,
            Double defaultUnitCost,
            Double reorderQuantity,
            String purchaseUnit,
            String recipeUnit,
            double conversionFactor
    ) {
        this.inventoryItemId = inventoryItemId;
        this.name = name;
        this.unit = unit;
        this.currentQuantity = currentQuantity;
        this.reorderLevel = reorderLevel;
        this.active = active;
        this.preferredSupplierId = preferredSupplierId;
        this.defaultUnitCost = defaultUnitCost;
        this.reorderQuantity = reorderQuantity;
        this.purchaseUnit = purchaseUnit;
        this.recipeUnit = recipeUnit;
        setConversionFactor(conversionFactor);
    }

    /**
     * Gets the inventory item database identifier.
     *
     * @return the inventory item ID
     */
    public int getInventoryItemId() {
        return inventoryItemId;
    }

    /**
     * Sets the inventory item database identifier.
     *
     * @param inventoryItemId the inventory item ID to store
     */
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /**
     * Gets the inventory item name.
     *
     * @return the inventory item name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the inventory item name.
     *
     * @param name the inventory item name to store
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the main unit used for the inventory item.
     *
     * @return the main unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the main unit used for the inventory item.
     *
     * If the purchase unit or recipe unit have not already been set, they are
     * also set to this unit.
     *
     * @param unit the unit to store
     */
    public void setUnit(String unit) {
        this.unit = unit;

        if (purchaseUnit == null || purchaseUnit.isBlank()) {
            purchaseUnit = unit;
        }

        if (recipeUnit == null || recipeUnit.isBlank()) {
            recipeUnit = unit;
        }
    }

    /**
     * Gets the current stock quantity.
     *
     * @return the current stock quantity
     */
    public double getCurrentQuantity() {
        return currentQuantity;
    }

    /**
     * Sets the current stock quantity.
     *
     * @param currentQuantity the current stock quantity to store
     */
    public void setCurrentQuantity(double currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    /**
     * Gets the reorder level.
     *
     * @return the reorder level
     */
    public double getReorderLevel() {
        return reorderLevel;
    }

    /**
     * Sets the reorder level.
     *
     * @param reorderLevel the reorder level to store
     */
    public void setReorderLevel(double reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    /**
     * Checks whether the inventory item is active.
     *
     * @return true if the item is active, otherwise false
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the inventory item is active.
     *
     * @param active true if the item is active, otherwise false
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Gets the preferred supplier linked to this item.
     *
     * @return the preferred supplier ID, or null if no supplier is recorded
     */
    public Integer getPreferredSupplierId() {
        return preferredSupplierId;
    }

    /**
     * Sets the preferred supplier linked to this item.
     *
     * @param preferredSupplierId the preferred supplier ID, or null if no supplier is recorded
     */
    public void setPreferredSupplierId(Integer preferredSupplierId) {
        this.preferredSupplierId = preferredSupplierId;
    }

    /**
     * Gets the default unit cost.
     *
     * @return the default unit cost, or null if no cost is recorded
     */
    public Double getDefaultUnitCost() {
        return defaultUnitCost;
    }

    /**
     * Sets the default unit cost.
     *
     * @param defaultUnitCost the default unit cost to store, or null if not recorded
     */
    public void setDefaultUnitCost(Double defaultUnitCost) {
        this.defaultUnitCost = defaultUnitCost;
    }

    /**
     * Gets the normal reorder quantity.
     *
     * @return the reorder quantity, or null if not recorded
     */
    public Double getReorderQuantity() {
        return reorderQuantity;
    }

    /**
     * Sets the normal reorder quantity.
     *
     * @param reorderQuantity the reorder quantity to store, or null if not recorded
     */
    public void setReorderQuantity(Double reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }

    /**
     * Gets the unit used when purchasing this item.
     *
     * If no purchase unit has been set, the main unit is returned instead.
     *
     * @return the purchase unit
     */
    public String getPurchaseUnit() {
        if (purchaseUnit == null || purchaseUnit.isBlank()) {
            return unit;
        }

        return purchaseUnit;
    }

    /**
     * Sets the unit used when purchasing this item.
     *
     * @param purchaseUnit the purchase unit to store
     */
    public void setPurchaseUnit(String purchaseUnit) {
        this.purchaseUnit = purchaseUnit;
    }

    /**
     * Gets the unit used when this item is used in recipes.
     *
     * If no recipe unit has been set, the main unit is returned instead.
     *
     * @return the recipe unit
     */
    public String getRecipeUnit() {
        if (recipeUnit == null || recipeUnit.isBlank()) {
            return unit;
        }

        return recipeUnit;
    }

    /**
     * Sets the unit used when this item is used in recipes.
     *
     * @param recipeUnit the recipe unit to store
     */
    public void setRecipeUnit(String recipeUnit) {
        this.recipeUnit = recipeUnit;
    }

    /**
     * Gets the conversion factor used to convert purchase quantities into
     * recipe quantities.
     *
     * If the stored conversion factor is invalid, 1.0 is returned.
     *
     * @return the conversion factor
     */
    public double getConversionFactor() {
        if (conversionFactor <= 0) {
            return 1.0;
        }

        return conversionFactor;
    }

    /**
     * Sets the conversion factor used for unit conversion.
     *
     * If the value is zero or below, the conversion factor is set to 1.0
     * to avoid invalid stock calculations.
     *
     * @param conversionFactor the conversion factor to store
     */
    public void setConversionFactor(double conversionFactor) {
        if (conversionFactor <= 0) {
            this.conversionFactor = 1.0;
        } else {
            this.conversionFactor = conversionFactor;
        }
    }

    /**
     * Converts a purchase quantity into the recipe quantity.
     *
     * This supports cases where stock is bought in one unit but used in recipes
     * in another unit.
     *
     * @param purchaseQuantity the quantity bought or received
     * @return the converted quantity in the recipe unit
     */
    public double convertPurchaseQuantityToRecipeQuantity(
            double purchaseQuantity
    ) {
        return purchaseQuantity * getConversionFactor();
    }

    /**
     * Checks whether the item is at or below its reorder level.
     *
     * @return true if the current quantity is at or below the reorder level
     */
    public boolean isLowStock() {
        return currentQuantity <= reorderLevel;
    }

    /**
     * Calculates an estimated value for the current stock.
     *
     * If no default unit cost has been recorded, the value is returned as 0.0.
     *
     * @return the estimated stock value
     */
    public double getEstimatedStockValue() {
        if (defaultUnitCost == null) {
            return 0.0;
        }

        return currentQuantity * defaultUnitCost;
    }

    /**
     * Returns a readable version of the inventory item.
     *
     * This is used when an inventory item is displayed in controls such as
     * combo boxes or tables.
     *
     * @return the inventory item ID and name
     */
    @Override
    public String toString() {
        return inventoryItemId + " - " + name;
    }
}