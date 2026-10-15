package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.InventoryBatch;
 
/**
 * Data access object for the {@code inventory_batch} table, which records
 * each separate delivery of a stock item.
 */
public class InventoryBatchDAO {
 
    /**
     * Retrieves every batch on record, oldest purchase first.
     */
    public List<InventoryBatch> getAllBatches() throws Exception {
        String sql = """
            SELECT
                batch_id,
                inventory_item_id,
                supplier_id,
                batch_code,
                purchase_date,
                expiry_date,
                quantity_received,
                quantity_remaining,
                unit_cost
            FROM inventory_batch
            ORDER BY purchase_date ASC, batch_id ASC
        """;
 
        List<InventoryBatch> batches = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()) {
 
            while (results.next()) {
                batches.add(mapBatch(results));
            }
        }
 
        return batches;
    }
 
    /**
     * Retrieves every batch of one stock item, oldest purchase first.
     */
    public List<InventoryBatch> getBatchesForInventoryItem(int inventoryItemId) throws Exception {
        String sql = """
            SELECT
                batch_id,
                inventory_item_id,
                supplier_id,
                batch_code,
                purchase_date,
                expiry_date,
                quantity_received,
                quantity_remaining,
                unit_cost
            FROM inventory_batch
            WHERE inventory_item_id = ?
            ORDER BY purchase_date ASC, batch_id ASC
        """;
 
        List<InventoryBatch> batches = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, inventoryItemId);
 
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    batches.add(mapBatch(results));
                }
            }
        }
 
        return batches;
    }
 
    /**
     * Returns usable batches ordered oldest first for FIFO stock usage.
     * Expired batches are excluded from booking deductions.
     */
    public List<InventoryBatch> getAvailableBatchesForInventoryItem(int inventoryItemId) throws Exception {
        String sql = "SELECT batch_id, inventory_item_id, supplier_id, batch_code, purchase_date, expiry_date, quantity_received, quantity_remaining, unit_cost FROM inventory_batch WHERE inventory_item_id = ? AND quantity_remaining > 0 AND ( expiry_date IS NULL OR expiry_date >= DATE('now') ) ORDER BY purchase_date ASC, batch_id ASC ";
          List<InventoryBatch> batches = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, inventoryItemId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    batches.add(mapBatch(results));
                }
            }
        }
        return batches;
    }
 
    /**
     * Retrieves a single batch by its identifier, opening and closing its own
     * connection.
     */
    public InventoryBatch getBatchById(int batchId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return getBatchById(conn, batchId);
        }
    }
 
    /**
     * Retrieves a single batch by its identifier, using a connection supplied
     * by the caller.
     */
    public InventoryBatch getBatchById(Connection conn, int batchId) throws Exception {
        String sql = "SELECT batch_id, inventory_item_id, supplier_id, batch_code, purchase_date, expiry_date, quantity_received, quantity_remaining, unit_cost FROM inventory_batch WHERE batch_id = ? ";
          try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, batchId);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return mapBatch(results);
                }
            }
        }
        return null;
    }
 
    /**
     * Records a new delivery of stock, opening and closing its own connection.
     */
    public void addBatch(InventoryBatch batch) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            addBatch(conn, batch);
        }
    }
 
    /**
     * Records a new delivery of stock using an existing database transaction.
     */
    public void addBatch(Connection conn, InventoryBatch batch) throws Exception {
        String sql = """
            INSERT INTO inventory_batch (
                inventory_item_id,
                supplier_id,
                batch_code,
                purchase_date,
                expiry_date,
                quantity_received,
                quantity_remaining,
                unit_cost
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
 
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, batch.getInventoryItemId());
 
            if (batch.getSupplierId() != null) {
                statement.setInt(2, batch.getSupplierId());
            } else {
                statement.setNull(2, java.sql.Types.INTEGER);
            }
 
            statement.setString(3, batch.getBatchCode());
            statement.setString(4, batch.getPurchaseDate());
            statement.setString(5, batch.getExpiryDate());
            statement.setDouble(6, batch.getQuantityReceived());
            statement.setDouble(7, batch.getQuantityRemaining());
            statement.setDouble(8, batch.getUnitCost());
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Overwrites every editable field of a batch, opening and closing its own
     * connection.
     */
    public void updateBatch(InventoryBatch batch) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            updateBatch(conn, batch);
        }
    }
 
    /**
     * Overwrites every editable field of a batch using an existing database
     * transaction.
     */
    public void updateBatch(Connection conn, InventoryBatch batch) throws Exception {
        String sql = """
            UPDATE inventory_batch
            SET inventory_item_id = ?,
                supplier_id = ?,
                batch_code = ?,
                purchase_date = ?,
                expiry_date = ?,
                quantity_received = ?,
                quantity_remaining = ?,
                unit_cost = ?
            WHERE batch_id = ?
        """;
 
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, batch.getInventoryItemId());
 
            if (batch.getSupplierId() != null) {
                statement.setInt(2, batch.getSupplierId());
            } else {
                statement.setNull(2, java.sql.Types.INTEGER);
            }
 
            statement.setString(3, batch.getBatchCode());
            statement.setString(4, batch.getPurchaseDate());
            statement.setString(5, batch.getExpiryDate());
            statement.setDouble(6, batch.getQuantityReceived());
            statement.setDouble(7, batch.getQuantityRemaining());
            statement.setDouble(8, batch.getUnitCost());
            statement.setInt(9, batch.getBatchId());
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Sets how much of a batch is left, opening and closing its own
     * connection.
     */
    public void updateBatchQuantityRemaining(int batchId, double quantityRemaining) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            updateBatchQuantityRemaining(conn, batchId, quantityRemaining);
        }
    }
 
    /**
     * Sets how much of a batch is left, using an existing database
     * transaction.
     */
    public void updateBatchQuantityRemaining(Connection conn, int batchId, double quantityRemaining) throws Exception {
        String sql = """
            UPDATE inventory_batch
            SET quantity_remaining = ?
            WHERE batch_id = ?
        """;
 
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDouble(1, quantityRemaining);
            statement.setInt(2, batchId);
            statement.executeUpdate();
        }
    }
 
    /**
     * Permanently removes a batch, opening and closing its own connection.
     */
    public void deleteBatch(int batchId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            deleteBatch(conn, batchId);
        }
    }
 
    /**
     * Permanently removes a batch using an existing database transaction.
     */
    public void deleteBatch(Connection conn, int batchId) throws Exception {
        String sql = "DELETE FROM inventory_batch WHERE batch_id = ?";
 
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, batchId);
            statement.executeUpdate();
        }
    }
 
    /**
     * Total stock value for one inventory item based on remaining batch
     * quantities.
     */
    public double getStockValueForInventoryItem(int inventoryItemId) throws Exception {
        String sql = """
            SELECT SUM(quantity_remaining * unit_cost) AS stock_value
            FROM inventory_batch
            WHERE inventory_item_id = ?
              AND quantity_remaining > 0
        """;
 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, inventoryItemId);
 
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    double stockValue = results.getDouble("stock_value");
                    return results.wasNull() ? 0.0 : stockValue;
                }
            }
        }
 
        return 0.0;
    }
 
    /**
     * Total stock value across all inventory items.
     */
    public double getTotalStockValue() throws Exception {
        String sql = """
            SELECT SUM(quantity_remaining * unit_cost) AS total_stock_value
            FROM inventory_batch
            WHERE quantity_remaining > 0
        """;
 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()) {
 
            if (results.next()) {
                double totalValue = results.getDouble("total_stock_value");
                return results.wasNull() ? 0.0 : totalValue;
            }
        }
 
        return 0.0;
    }
 
    /**
     * Average cost per unit for an inventory item based on remaining stock.
     */
    public double getAverageUnitCost(int inventoryItemId) throws Exception {
        String sql = """
            SELECT
                SUM(quantity_remaining * unit_cost) / SUM(quantity_remaining) AS average_unit_cost
            FROM inventory_batch
            WHERE inventory_item_id = ?
              AND quantity_remaining > 0
        """;
 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, inventoryItemId);
 
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    double averageCost = results.getDouble("average_unit_cost");
                    return results.wasNull() ? 0.0 : averageCost;
                }
            }
        }
 
        return 0.0;
    }
 
    /**
     * Retrieves batches that have gone out of date but still have stock in
     * them, soonest expiry first.
     */
    public List<InventoryBatch> getExpiredBatches() throws Exception {
        String sql = """
            SELECT
                batch_id,
                inventory_item_id,
                supplier_id,
                batch_code,
                purchase_date,
                expiry_date,
                quantity_received,
                quantity_remaining,
                unit_cost
            FROM inventory_batch
            WHERE expiry_date IS NOT NULL
              AND expiry_date < DATE('now')
              AND quantity_remaining > 0
            ORDER BY expiry_date ASC
        """;
 
        List<InventoryBatch> batches = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()) {
 
            while (results.next()) {
                batches.add(mapBatch(results));
            }
        }
 
        return batches;
    }
 
    /**
     * Retrieves batches still in date but due to expire within the next so
     * many days, soonest expiry first.
     */
    public List<InventoryBatch> getBatchesExpiringSoon(int daysAhead) throws Exception {
        String sql = """
            SELECT
                batch_id,
                inventory_item_id,
                supplier_id,
                batch_code,
                purchase_date,
                expiry_date,
                quantity_received,
                quantity_remaining,
                unit_cost
            FROM inventory_batch
            WHERE expiry_date IS NOT NULL
              AND expiry_date >= DATE('now')
              AND expiry_date <= DATE('now', ?)
              AND quantity_remaining > 0
            ORDER BY expiry_date ASC
        """;
 
        List<InventoryBatch> batches = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setString(1, "+" + daysAhead + " days");
 
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    batches.add(mapBatch(results));
                }
            }
        }
 
        return batches;
    }
 
    /**
     * Builds an {InventoryBatch} from the current row of a result set.
     */
    private InventoryBatch mapBatch(ResultSet results) throws Exception {
        return new InventoryBatch(
                results.getInt("batch_id"),
                results.getInt("inventory_item_id"),
                (Integer) results.getObject("supplier_id"),
                results.getString("batch_code"),
                results.getString("purchase_date"),
                results.getString("expiry_date"),
                results.getDouble("quantity_received"),
                results.getDouble("quantity_remaining"),
                results.getDouble("unit_cost")
        );
    }
}