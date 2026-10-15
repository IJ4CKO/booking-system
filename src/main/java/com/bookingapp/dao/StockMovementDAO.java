package com.bookingapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.StockMovement;
 
/**
 * Data access object for the {stock_movement} table, the running audit
 * trail of every change to stock levels.
 */
public class StockMovementDAO {
 
    /**
     * Records a change in stock level, using an existing database transaction.
     */
    public void addStockMovement(Connection conn, int inventoryItemId, double quantityChange,
                                 Integer bookingId, String movementType, String notes) throws Exception {
        String sql = """
            INSERT INTO stock_movement (
                inventory_item_id,
                movement_time,
                quantity_change,
                booking_id,
                movement_type,
                notes
            )
            VALUES (?, datetime('now'), ?, ?, ?, ?)
        """;
 
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, inventoryItemId);
            statement.setDouble(2, quantityChange);
 
            if (bookingId != null) {
                statement.setInt(3, bookingId);
            } else {
                statement.setNull(3, java.sql.Types.INTEGER);
            }
 
            statement.setString(4, movementType);
            statement.setString(5, notes);
            statement.executeUpdate();
        }
    }
 
    /**
     * Convenience overload for older code paths that do not need notes.
     */
    public void addStockMovement(Connection conn, int inventoryItemId, double quantityChange,
                                 Integer bookingId, String movementType) throws Exception {
        addStockMovement(conn, inventoryItemId, quantityChange, bookingId, movementType, null);
    }
 
    /**
     * Retrieves every stock movement, newest first.
     */
    public List<StockMovement> getAllStockMovements() throws Exception {
        String sql = """
            SELECT
                stock_movement_id,
                inventory_item_id,
                movement_time,
                quantity_change,
                booking_id,
                movement_type,
                notes
            FROM stock_movement
            ORDER BY movement_time DESC, stock_movement_id DESC
        """;
 
        List<StockMovement> movements = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
 
            while (results.next()) {
                StockMovement movement = new StockMovement(
                        results.getInt("stock_movement_id"),
                        results.getInt("inventory_item_id"),
                        results.getString("movement_time"),
                        results.getDouble("quantity_change"),
                        (Integer) results.getObject("booking_id"),
                        results.getString("movement_type"),
                        results.getString("notes")
                );
 
                movements.add(movement);
            }
        }
 
        return movements;
    }
 
    /**
     * Retrieves the movements affecting one stock item, newest first.
     */
    public List<StockMovement> getStockMovementsForInventoryItem(int inventoryItemId) throws Exception {
        String sql = """
            SELECT
                stock_movement_id,
                inventory_item_id,
                movement_time,
                quantity_change,
                booking_id,
                movement_type,
                notes
            FROM stock_movement
            WHERE inventory_item_id = ?
            ORDER BY movement_time DESC, stock_movement_id DESC
        """;
 
        List<StockMovement> movements = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, inventoryItemId);
 
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    StockMovement movement = new StockMovement(
                            results.getInt("stock_movement_id"),
                            results.getInt("inventory_item_id"),
                            results.getString("movement_time"),
                            results.getDouble("quantity_change"),
                            (Integer) results.getObject("booking_id"),
                            results.getString("movement_type"),
                            results.getString("notes")
                    );
 
                    movements.add(movement);
                }
            }
        }
 
        return movements;
    }
 
    /**
     * Retrieves the movements recorded against one booking, newest first.
     */
    public List<StockMovement> getStockMovementsForBooking(int bookingId) throws Exception {
        String sql = """
            SELECT
                stock_movement_id,
                inventory_item_id,
                movement_time,
                quantity_change,
                booking_id,
                movement_type,
                notes
            FROM stock_movement
            WHERE booking_id = ?
            ORDER BY movement_time DESC, stock_movement_id DESC
        """;
 
        List<StockMovement> movements = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, bookingId);
 
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    StockMovement movement = new StockMovement(
                            results.getInt("stock_movement_id"),
                            results.getInt("inventory_item_id"),
                            results.getString("movement_time"),
                            results.getDouble("quantity_change"),
                            (Integer) results.getObject("booking_id"),
                            results.getString("movement_type"),
                            results.getString("notes")
                    );
 
                    movements.add(movement);
                }
            }
        }
 
        return movements;
    }
}
