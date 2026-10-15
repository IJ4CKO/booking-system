package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.InventoryWaste;
 
/**
 * Data access object for the {inventory_waste} table, which records
 * stock written off rather than used.
 */
public class InventoryWasteDAO {
 
    /**
     * Adds a waste record using its own database connection.
     */
    public int addWasteRecord(InventoryWaste wasteRecord) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return addWasteRecord(conn, wasteRecord);
        }
    }
 
    /**
     * Adds a waste record using an existing connection.
     */
    public int addWasteRecord(Connection conn, InventoryWaste wasteRecord) throws Exception {
        String sql = " INSERT INTO inventory_waste ( batch_id, inventory_item_id, waste_date, quantity_wasted, reason, unit_cost, waste_value, notes ) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "; 
        try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, wasteRecord.getBatchId());
            statement.setInt(2, wasteRecord.getInventoryItemId());
            statement.setString(3, wasteRecord.getWasteDate());
            statement.setDouble(4, wasteRecord.getQuantityWasted());
            statement.setString(5, wasteRecord.getReason());
            statement.setDouble(6, wasteRecord.getUnitCost());
            statement.setDouble(7, wasteRecord.getWasteValue());
            if (wasteRecord.getNotes() == null || wasteRecord.getNotes().isBlank()) {
                statement.setNull(8, java.sql.Types.VARCHAR);
            } else {
                statement.setString(8, wasteRecord.getNotes().trim());
            }
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("The waste record could not be created.");
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int wasteId = generatedKeys.getInt(1);
                    wasteRecord.setWasteId(wasteId);
                    return wasteId;
                }
            }
        }
        throw new Exception("The waste record ID could not be retrieved.");
    }
 
    /**
     * Returns every waste record, with the newest records first.
     */
    public List<InventoryWaste> getAllWasteRecords() throws Exception {
        String sql = " SELECT waste_id, batch_id, inventory_item_id, waste_date, quantity_wasted, reason, unit_cost, waste_value, notes FROM inventory_waste ORDER BY waste_date DESC, waste_id DESC "
        ; 
        List<InventoryWaste> wasteRecords = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                wasteRecords.add(mapWasteRecord(results));
            }
        }
        return wasteRecords;
    }
 
    /**
     * Returns all waste records linked to one inventory batch.
     */
    public List<InventoryWaste> getWasteRecordsForBatch(int batchId) throws Exception {
        String sql = " SELECT waste_id, batch_id, inventory_item_id, waste_date, quantity_wasted, reason, unit_cost, waste_value, notes FROM inventory_waste WHERE batch_id = ? ORDER BY waste_date DESC, waste_id DESC "; 
        List<InventoryWaste> wasteRecords = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, batchId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    wasteRecords.add(mapWasteRecord(results));
                }
            }
        }
        return wasteRecords;
    }
 
    /**
     * Returns all waste records linked to one inventory item.
     */
    public List<InventoryWaste> getWasteRecordsForInventoryItem(int inventoryItemId) throws Exception {
        String sql = " SELECT waste_id, batch_id, inventory_item_id, waste_date, quantity_wasted, reason, unit_cost, waste_value, notes FROM inventory_waste WHERE inventory_item_id = ? ORDER BY waste_date DESC, waste_id DESC "; 
        List<InventoryWaste> wasteRecords = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, inventoryItemId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    wasteRecords.add(mapWasteRecord(results));
                }
            }
        }
        return wasteRecords;
    }
 
    /**
     * Returns one waste record using its ID.
     */
    public InventoryWaste getWasteRecordById(int wasteId) throws Exception {
        String sql = " SELECT waste_id, batch_id, inventory_item_id, waste_date, quantity_wasted, reason, unit_cost, waste_value, notes FROM inventory_waste WHERE waste_id = ? ";
         try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, wasteId);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return mapWasteRecord(results);
                }
            }
        }
        return null;
    }
 
    /**
     * Calculates the total value of all recorded waste.
     */
    public double getTotalWasteValue() throws Exception {
        String sql = " SELECT SUM(waste_value) AS total_waste_value FROM inventory_waste "; 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                double totalValue = results.getDouble("total_waste_value");
                return results.wasNull() ? 0.0 : totalValue;
            }
        }
        return 0.0;
    }
 
    /**
     * Calculates the total waste value for one inventory item.
     */
    public double getTotalWasteValueForInventoryItem(int inventoryItemId) throws Exception {
        String sql = " SELECT SUM(waste_value) AS total_waste_value FROM inventory_waste WHERE inventory_item_id = ? ";
         try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, inventoryItemId);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    double totalValue = results.getDouble("total_waste_value");
                    return results.wasNull() ? 0.0 : totalValue;
                }
            }
        }
        return 0.0;
    }
 
    /**
     * Calculates the total value of waste recorded between two dates.
     */
    public double getTotalWasteValueBetweenDates(String startDate, String endDate) throws Exception {
        String sql =" SELECT SUM(waste_value) AS total_waste_value FROM inventory_waste WHERE waste_date >= ? AND waste_date <= ? "; 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, startDate);
            statement.setString(2, endDate);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    double totalValue = results.getDouble("total_waste_value");
                    return results.wasNull() ? 0.0 : totalValue;
                }
            }
        }
        return 0.0;
    }
 
    /**
     * Deletes a waste record.
     */
    public void deleteWasteRecord(int wasteId) throws Exception {
        String sql = " DELETE FROM inventory_waste WHERE waste_id = ? ";
          try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, wasteId);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("The waste record was not found.");
            }
        }
    }
 
    /**
     * Converts a database result into an InventoryWaste object.
     */
    private InventoryWaste mapWasteRecord(ResultSet results) throws Exception {
        return new InventoryWaste(results.getInt("waste_id"), results.getInt("batch_id"), results.getInt("inventory_item_id"), results.getString("waste_date"), results.getDouble("quantity_wasted"), results.getString("reason"), results.getDouble("unit_cost"), results.getDouble("waste_value"), results.getString("notes"));
    }
}