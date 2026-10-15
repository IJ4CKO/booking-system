package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.InventoryItem;
 
/**
 * Data access object for the {inventory_items} table, which holds the
 * raw materials kept in stock.
 */
public class InventoryDAO {
 
    /**
     * Retrieves every stock item, in alphabetical order by name.
     */
    public List<InventoryItem> getAllInventoryItems() throws Exception {
        String sql = """
            SELECT
                inventory_item_id,
                name,
                unit,
                current_quantity,
                reorder_level,
                active,
                preferred_supplier_id,
                default_unit_cost,
                reorder_quantity,
                purchase_unit,
                recipe_unit,
                conversion_factor
            FROM inventory_items
            ORDER BY name
            """;
 
        List<InventoryItem> items =
                new ArrayList<>();
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql);
             ResultSet results =
                     statement.executeQuery()) {
 
            while (results.next()) {
                items.add(
                        mapInventoryItem(results)
                );
            }
        }
 
        return items;
    }
 
    /**
     * Retrieves the stock items still in use, in alphabetical order by name.
     */
    public List<InventoryItem> getActiveInventoryItems() throws Exception {
        String sql = """
            SELECT
                inventory_item_id,
                name,
                unit,
                current_quantity,
                reorder_level,
                active,
                preferred_supplier_id,
                default_unit_cost,
                reorder_quantity,
                purchase_unit,
                recipe_unit,
                conversion_factor
            FROM inventory_items
            WHERE active = 1
            ORDER BY name
            """;
 
        List<InventoryItem> items =
                new ArrayList<>();
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql);
             ResultSet results =
                     statement.executeQuery()) {
 
            while (results.next()) {
                items.add(
                        mapInventoryItem(results)
                );
            }
        }
 
        return items;
    }
 
    /**
     * Retrieves a single stock item by its identifier.
     */
    public InventoryItem getInventoryItemById(
            int inventoryItemId
    ) throws Exception {
 
        String sql = """
            SELECT
                inventory_item_id,
                name,
                unit,
                current_quantity,
                reorder_level,
                active,
                preferred_supplier_id,
                default_unit_cost,
                reorder_quantity,
                purchase_unit,
                recipe_unit,
                conversion_factor
            FROM inventory_items
            WHERE inventory_item_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    inventoryItemId
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (results.next()) {
                    return mapInventoryItem(results);
                }
            }
        }
 
        return null;
    }
 
    /**
     * Reads just the running quantity of one stock item.
     */
    public double getCurrentQuantity(
            int inventoryItemId
    ) throws Exception {
 
        String sql = """
            SELECT current_quantity
            FROM inventory_items
            WHERE inventory_item_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    inventoryItemId
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (results.next()) {
                    return results.getDouble("current_quantity");
                }
            }
        }
 
        throw new Exception(
                "Inventory item not found: " + inventoryItemId
        );
    }
 
    /**
     * Adds a new stock item to the catalogue.
     */
    public void addInventoryItem(
            InventoryItem item
    ) throws Exception {
 
        String sql = """
            INSERT INTO inventory_items (
                name,
                unit,
                current_quantity,
                reorder_level,
                active,
                preferred_supplier_id,
                default_unit_cost,
                reorder_quantity,
                purchase_unit,
                recipe_unit,
                conversion_factor
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    item.getName()
            );
 
            statement.setString(
                    2,
                    item.getUnit()
            );
 
            statement.setDouble(
                    3,
                    item.getCurrentQuantity()
            );
 
            statement.setDouble(
                    4,
                    item.getReorderLevel()
            );
 
            statement.setInt(
                    5,
                    item.isActive() ? 1 : 0
            );
 
            setNullableInteger(
                    statement,
                    6,
                    item.getPreferredSupplierId()
            );
 
            setNullableDouble(
                    statement,
                    7,
                    item.getDefaultUnitCost()
            );
 
            setNullableDouble(
                    statement,
                    8,
                    item.getReorderQuantity()
            );
 
            statement.setString(
                    9,
                    item.getPurchaseUnit()
            );
 
            statement.setString(
                    10,
                    item.getRecipeUnit()
            );
 
            statement.setDouble(
                    11,
                    item.getConversionFactor()
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Overwrites every editable field of a stock item.
     */
    public void updateInventoryItem(
            InventoryItem item
    ) throws Exception {
 
        String sql = """
            UPDATE inventory_items
            SET name = ?,
                unit = ?,
                current_quantity = ?,
                reorder_level = ?,
                active = ?,
                preferred_supplier_id = ?,
                default_unit_cost = ?,
                reorder_quantity = ?,
                purchase_unit = ?,
                recipe_unit = ?,
                conversion_factor = ?
            WHERE inventory_item_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setString(
                    1,
                    item.getName()
            );
 
            statement.setString(
                    2,
                    item.getUnit()
            );
 
            statement.setDouble(
                    3,
                    item.getCurrentQuantity()
            );
 
            statement.setDouble(
                    4,
                    item.getReorderLevel()
            );
 
            statement.setInt(
                    5,
                    item.isActive() ? 1 : 0
            );
 
            setNullableInteger(
                    statement,
                    6,
                    item.getPreferredSupplierId()
            );
 
            setNullableDouble(
                    statement,
                    7,
                    item.getDefaultUnitCost()
            );
 
            setNullableDouble(
                    statement,
                    8,
                    item.getReorderQuantity()
            );
 
            statement.setString(
                    9,
                    item.getPurchaseUnit()
            );
 
            statement.setString(
                    10,
                    item.getRecipeUnit()
            );
 
            statement.setDouble(
                    11,
                    item.getConversionFactor()
            );
 
            statement.setInt(
                    12,
                    item.getInventoryItemId()
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Sets the quantity of a stock item outright.
     */
    public void updateCurrentQuantity(
            int inventoryItemId,
            double newQuantity
    ) throws Exception {
 
        String sql = """
            UPDATE inventory_items
            SET current_quantity = ?
            WHERE inventory_item_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setDouble(
                    1,
                    newQuantity
            );
 
            statement.setInt(
                    2,
                    inventoryItemId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Adjusts the quantity of a stock item by a signed amount, using an
     * existing database transaction.
     */
    public void updateInventoryQuantity(
            Connection conn,
            int inventoryItemId,
            double quantityChange
    ) throws Exception {
 
        String sql = """
            UPDATE inventory_items
            SET current_quantity = current_quantity + ?
            WHERE inventory_item_id = ?
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setDouble(
                    1,
                    quantityChange
            );
 
            statement.setInt(
                    2,
                    inventoryItemId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Adds to the quantity of a stock item, opening and closing its own
     * connection.
     */
    public void increaseQuantity(
            int inventoryItemId,
            double amountToAdd
    ) throws Exception {
 
        String sql = """
            UPDATE inventory_items
            SET current_quantity = current_quantity + ?
            WHERE inventory_item_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setDouble(
                    1,
                    amountToAdd
            );
 
            statement.setInt(
                    2,
                    inventoryItemId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Takes away from the quantity of a stock item, opening and closing its
     * own connection.
     */
    public void decreaseQuantity(
            int inventoryItemId,
            double amountToSubtract
    ) throws Exception {
 
        String sql = """
            UPDATE inventory_items
            SET current_quantity = current_quantity - ?
            WHERE inventory_item_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setDouble(
                    1,
                    amountToSubtract
            );
 
            statement.setInt(
                    2,
                    inventoryItemId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Retrieves the active stock items that have fallen to or below their
     * reorder level, in alphabetical order by name.
     */
    public List<InventoryItem> getLowStockItems() throws Exception {
        String sql = """
            SELECT
                inventory_item_id,
                name,
                unit,
                current_quantity,
                reorder_level,
                active,
                preferred_supplier_id,
                default_unit_cost,
                reorder_quantity,
                purchase_unit,
                recipe_unit,
                conversion_factor
            FROM inventory_items
            WHERE active = 1
              AND current_quantity <= reorder_level
            ORDER BY name
            """;
 
        List<InventoryItem> items =
                new ArrayList<>();
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql);
             ResultSet results =
                     statement.executeQuery()) {
 
            while (results.next()) {
                items.add(
                        mapInventoryItem(results)
                );
            }
        }
 
        return items;
    }
 
    /**
     * Permanently removes a stock item.
     */
    public void deleteInventoryItem(
            int inventoryItemId
    ) throws Exception {
 
        String sql = """
            DELETE FROM inventory_items
            WHERE inventory_item_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    inventoryItemId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Builds an {InventoryItem} from the current row of a result set.
     */
    private InventoryItem mapInventoryItem(
            ResultSet results
    ) throws Exception {
 
        String unit =
                results.getString("unit");
 
        String purchaseUnit =
                results.getString("purchase_unit");
 
        String recipeUnit =
                results.getString("recipe_unit");
 
        double conversionFactor =
                results.getDouble("conversion_factor");
 
        if (conversionFactor <= 0) {
            conversionFactor = 1.0;
        }
 
        InventoryItem item =
                new InventoryItem(
                        results.getInt("inventory_item_id"),
                        results.getString("name"),
                        unit,
                        results.getDouble("current_quantity"),
                        results.getDouble("reorder_level"),
                        results.getInt("active") == 1,
                        getNullableInteger(
                                results,
                                "preferred_supplier_id"
                        ),
                        getNullableDouble(
                                results,
                                "default_unit_cost"
                        ),
                        getNullableDouble(
                                results,
                                "reorder_quantity"
                        ),
                        purchaseUnit,
                        recipeUnit,
                        conversionFactor
                );
 
        return item;
    }
 
    /**
     * Reads a whole-number column that may be absent.
     */
    private Integer getNullableInteger(
            ResultSet results,
            String columnName
    ) throws Exception {
 
        Object value =
                results.getObject(columnName);
 
        if (value == null) {
            return null;
        }
 
        if (value instanceof Number number) {
            return number.intValue();
        }
 
        return Integer.valueOf(value.toString());
    }
 
    /**
     * Reads a decimal column that may be absent.
     */
    private Double getNullableDouble(
            ResultSet results,
            String columnName
    ) throws Exception {
 
        Object value =
                results.getObject(columnName);
 
        if (value == null) {
            return null;
        }
 
        if (value instanceof Number number) {
            return number.doubleValue();
        }
 
        return Double.valueOf(value.toString());
    }
 
    /**
     * Binds a whole number that may be absent, storing {NULL} where
     * there is no value.
     */
    private void setNullableInteger(
            PreparedStatement statement,
            int parameterIndex,
            Integer value
    ) throws Exception {
 
        if (value == null) {
            statement.setNull(
                    parameterIndex,
                    Types.INTEGER
            );
        } else {
            statement.setInt(
                    parameterIndex,
                    value
            );
        }
    }
 
    /**
     * Binds a decimal that may be absent, storing {NULL} where there is
     * no value.
     */
    private void setNullableDouble(
            PreparedStatement statement,
            int parameterIndex,
            Double value
    ) throws Exception {
 
        if (value == null) {
            statement.setNull(
                    parameterIndex,
                    Types.REAL
            );
        } else {
            statement.setDouble(
                    parameterIndex,
                    value
            );
        }
    }
}