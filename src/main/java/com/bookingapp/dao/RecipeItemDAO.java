package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.RecipeItem;
 
/**
 * Data access object for the {recipe_items} table, which records how
 * much of each stock item one unit of a product consumes.
 */
public class RecipeItemDAO {
 
    /**
     * Adds a stock item to a product's recipe, with the quantity one unit of
     * the product consumes.
     */
    public void addRecipeItem(
            RecipeItem recipeItem
    ) throws Exception {
 
        validateRecipeItem(recipeItem);
 
        try (Connection conn =
                     DBConnection.getConnection()) {
 
            if (recipeItemAlreadyExists(
                    conn,
                    recipeItem.getProductId(),
                    recipeItem.getInventoryItemId()
            )) {
                throw new IllegalArgumentException(
                        "This inventory item is already linked to this product recipe."
                );
            }
 
            String sql = """
                INSERT INTO recipe_items (
                    product_id,
                    inventory_item_id,
                    quantity_required_per_unit
                )
                VALUES (?, ?, ?)
                """;
 
            try (PreparedStatement statement =
                         conn.prepareStatement(sql)) {
 
                statement.setInt(
                        1,
                        recipeItem.getProductId()
                );
 
                statement.setInt(
                        2,
                        recipeItem.getInventoryItemId()
                );
 
                statement.setDouble(
                        3,
                        recipeItem.getQuantityRequiredPerUnit()
                );
 
                statement.executeUpdate();
            }
        }
    }
 
    /**
     * Reports whether a stock item is already on a product's recipe, opening
     * and closing its own connection.
     */
    public boolean recipeItemAlreadyExists(
            int productId,
            int inventoryItemId
    ) throws Exception {
 
        try (Connection conn =
                     DBConnection.getConnection()) {
 
            return recipeItemAlreadyExists(
                    conn,
                    productId,
                    inventoryItemId
            );
        }
    }
 
    /**
     * Reports whether a stock item is already on a product's recipe, using a
     * connection supplied by the caller.
      */
    public boolean recipeItemAlreadyExists(
            Connection conn,
            int productId,
            int inventoryItemId
    ) throws Exception {
 
        String sql = """
            SELECT COUNT(*)
            FROM recipe_items
            WHERE product_id = ?
              AND inventory_item_id = ?
            """;
 
        try (PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    productId
            );
 
            statement.setInt(
                    2,
                    inventoryItemId
            );
 
            try (ResultSet results =
                         statement.executeQuery()) {
 
                if (results.next()) {
                    return results.getInt(1) > 0;
                }
            }
        }
 
        return false;
    }
 
    /**
     * Retrieves every recipe line across all products, in the order they were
     * added.
     */
    public List<RecipeItem> getAllRecipeItems()
            throws Exception {
 
        List<RecipeItem> recipeItems =
                new ArrayList<>();
 
        String sql = """
            SELECT
                r.recipe_item_id,
                r.product_id,
                r.inventory_item_id,
                r.quantity_required_per_unit,
                p.name AS product_name,
                i.name AS inventory_item_name
            FROM recipe_items r
            JOIN products p ON r.product_id = p.product_id
            JOIN inventory_items i
                ON r.inventory_item_id = i.inventory_item_id
            ORDER BY r.recipe_item_id
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql);
             ResultSet results =
                     statement.executeQuery()) {
 
            while (results.next()) {
                recipeItems.add(
                        new RecipeItem(
                                results.getInt("recipe_item_id"),
                                results.getInt("product_id"),
                                results.getInt("inventory_item_id"),
                                results.getDouble("quantity_required_per_unit"),
                                results.getString("product_name"),
                                results.getString("inventory_item_name")
                        )
                );
            }
        }
 
        return recipeItems;
    }
 
    /**
     * Removes a single line from a product's recipe.
     */
    public void deleteRecipeItem(
            int recipeItemId
    ) throws Exception {
 
        String sql = """
            DELETE FROM recipe_items
            WHERE recipe_item_id = ?
            """;
 
        try (Connection conn =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    recipeItemId
            );
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Checks that the details needed to save a recipe line are present and
     * sensible.
     */
    private void validateRecipeItem(
            RecipeItem recipeItem
    ) {
        if (recipeItem == null) {
            throw new IllegalArgumentException(
                    "A recipe item is required."
            );
        }
 
        if (recipeItem.getProductId() <= 0) {
            throw new IllegalArgumentException(
                    "A product is required."
            );
        }
 
        if (recipeItem.getInventoryItemId() <= 0) {
            throw new IllegalArgumentException(
                    "An inventory item is required."
            );
        }
 
        if (recipeItem.getQuantityRequiredPerUnit() <= 0) {
            throw new IllegalArgumentException(
                    "The quantity required per unit must be greater than zero."
            );
        }
    }
}