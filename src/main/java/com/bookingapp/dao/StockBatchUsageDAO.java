package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.StockBatchUsage;
 
/**
 * Data access object for the {stock_batch_usage} table, which records
 * which batch each withdrawal of stock was taken from.
 */
public class StockBatchUsageDAO {
 
    /**
     * Retrieves every record of stock drawn from a batch, newest first.
     */
    public List<StockBatchUsage> getAllBatchUsage() throws Exception {
        String sql = """
            SELECT
                batch_usage_id,
                batch_id,
                booking_id,
                inventory_item_id,
                quantity_used,
                unit_cost,
                usage_date
            FROM stock_batch_usage
            ORDER BY usage_date DESC, batch_usage_id DESC
        """;
 
        List<StockBatchUsage> usages = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
 
            while (results.next()) {
                usages.add(mapUsage(results));
            }
        }
 
        return usages;
    }
 
    /**
     * Retrieves the stock drawn for one booking, oldest first.
     */
    public List<StockBatchUsage> getBatchUsageForBooking(int bookingId) throws Exception {
        String sql = """
            SELECT
                batch_usage_id,
                batch_id,
                booking_id,
                inventory_item_id,
                quantity_used,
                unit_cost,
                usage_date
            FROM stock_batch_usage
            WHERE booking_id = ?
            ORDER BY usage_date ASC, batch_usage_id ASC
        """;
 
        List<StockBatchUsage> usages = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, bookingId);
 
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    usages.add(mapUsage(results));
                }
            }
        }
 
        return usages;
    }
 
    /**
     * Retrieves every withdrawal of one stock item, newest first.
     */
    public List<StockBatchUsage> getBatchUsageForInventoryItem(int inventoryItemId) throws Exception {
        String sql = """
            SELECT
                batch_usage_id,
                batch_id,
                booking_id,
                inventory_item_id,
                quantity_used,
                unit_cost,
                usage_date
            FROM stock_batch_usage
            WHERE inventory_item_id = ?
            ORDER BY usage_date DESC, batch_usage_id DESC
        """;
 
        List<StockBatchUsage> usages = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, inventoryItemId);
 
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    usages.add(mapUsage(results));
                }
            }
        }
 
        return usages;
    }
 
    /**
     * Retrieves every withdrawal from one batch, newest first.
     */
    public List<StockBatchUsage> getBatchUsageForBatch(int batchId) throws Exception {
        String sql = """
            SELECT
                batch_usage_id,
                batch_id,
                booking_id,
                inventory_item_id,
                quantity_used,
                unit_cost,
                usage_date
            FROM stock_batch_usage
            WHERE batch_id = ?
            ORDER BY usage_date DESC, batch_usage_id DESC
        """;
 
        List<StockBatchUsage> usages = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, batchId);
 
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    usages.add(mapUsage(results));
                }
            }
        }
 
        return usages;
    }
 
    /**
     * Records a withdrawal from a batch, opening and closing its own
     * connection.
     */
    public void addBatchUsage(StockBatchUsage usage) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            addBatchUsage(conn, usage);
        }
    }
 
    /**
     * Records a withdrawal from a batch using an existing database
     * transaction.
     */
    public void addBatchUsage(Connection conn, StockBatchUsage usage) throws Exception {
        String sql = """
            INSERT INTO stock_batch_usage (
                batch_id,
                booking_id,
                inventory_item_id,
                quantity_used,
                unit_cost,
                usage_date
            ) VALUES (?, ?, ?, ?, ?, ?)
        """;
 
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, usage.getBatchId());
 
            if (usage.getBookingId() != null) {
                statement.setInt(2, usage.getBookingId());
            } else {
                statement.setNull(2, java.sql.Types.INTEGER);
            }
 
            statement.setInt(3, usage.getInventoryItemId());
            statement.setDouble(4, usage.getQuantityUsed());
            statement.setDouble(5, usage.getUnitCost());
            statement.setString(6, usage.getUsageDate());
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Works out what the stock consumed by one booking cost.
     */
    public double getTotalUsageValueForBooking(int bookingId) throws Exception {
        String sql = """
            SELECT SUM(quantity_used * unit_cost) AS total_usage_value
            FROM stock_batch_usage
            WHERE booking_id = ?
        """;
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, bookingId);
 
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    double totalValue = results.getDouble("total_usage_value");
                    return results.wasNull() ? 0.0 : totalValue;
                }
            }
        }
 
        return 0.0;
    }
 
    /**
     * Works out what has been spent on one stock item over its whole history.
     */
    public double getTotalUsageValueForInventoryItem(int inventoryItemId) throws Exception {
        String sql = """
            SELECT SUM(quantity_used * unit_cost) AS total_usage_value
            FROM stock_batch_usage
            WHERE inventory_item_id = ?
        """;
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, inventoryItemId);
 
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    double totalValue = results.getDouble("total_usage_value");
                    return results.wasNull() ? 0.0 : totalValue;
                }
            }
        }
 
        return 0.0;
    }
 
    /**
     * Builds a {StockBatchUsage} from the current row of a result set.
     */
    private StockBatchUsage mapUsage(ResultSet results) throws Exception {
        return new StockBatchUsage(
                results.getInt("batch_usage_id"),
                results.getInt("batch_id"),
                (Integer) results.getObject("booking_id"),
                results.getInt("inventory_item_id"),
                results.getDouble("quantity_used"),
                results.getDouble("unit_cost"),
                results.getString("usage_date")
        );
    }
}