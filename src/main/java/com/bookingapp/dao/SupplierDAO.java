package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.Supplier;
 
/**
 * Data access object for the {supplier} table, which holds the
 * businesses stock is bought from.
 */
public class SupplierDAO {
 
    /**
     * Retrieves every supplier, in alphabetical order by name.
     */
    public List<Supplier> getAllSuppliers() throws Exception {
        String sql = """
            SELECT
                supplier_id,
                supplier_name,
                contact_name,
                phone,
                email,
                notes
            FROM supplier
            ORDER BY supplier_name
        """;
 
        List<Supplier> suppliers = new ArrayList<>();
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
 
            while (results.next()) {
                Supplier supplier = new Supplier(
                        results.getInt("supplier_id"),
                        results.getString("supplier_name"),
                        results.getString("contact_name"),
                        results.getString("phone"),
                        results.getString("email"),
                        results.getString("notes")
                );
 
                suppliers.add(supplier);
            }
        }
 
        return suppliers;
    }
 
    /**
     * Retrieves a single supplier by their identifier.
     */
    public Supplier getSupplierById(int supplierId) throws Exception {
        String sql = """
            SELECT
                supplier_id,
                supplier_name,
                contact_name,
                phone,
                email,
                notes
            FROM supplier
            WHERE supplier_id = ?
        """;
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, supplierId);
 
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return new Supplier(
                            results.getInt("supplier_id"),
                            results.getString("supplier_name"),
                            results.getString("contact_name"),
                            results.getString("phone"),
                            results.getString("email"),
                            results.getString("notes")
                    );
                }
            }
        }
 
        return null;
    }
 
    /**
     * Adds a new supplier.
     */
    public void addSupplier(Supplier supplier) throws Exception {
        String sql = """
            INSERT INTO supplier (
                supplier_name,
                contact_name,
                phone,
                email,
                notes
            ) VALUES (?, ?, ?, ?, ?)
        """;
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setString(1, supplier.getSupplierName());
            statement.setString(2, supplier.getContactName());
            statement.setString(3, supplier.getPhone());
            statement.setString(4, supplier.getEmail());
            statement.setString(5, supplier.getNotes());
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Overwrites every editable field of a supplier.
     */
    public void updateSupplier(Supplier supplier) throws Exception {
        String sql = """
            UPDATE supplier
            SET supplier_name = ?,
                contact_name = ?,
                phone = ?,
                email = ?,
                notes = ?
            WHERE supplier_id = ?
        """;
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setString(1, supplier.getSupplierName());
            statement.setString(2, supplier.getContactName());
            statement.setString(3, supplier.getPhone());
            statement.setString(4, supplier.getEmail());
            statement.setString(5, supplier.getNotes());
            statement.setInt(6, supplier.getSupplierId());
 
            statement.executeUpdate();
        }
    }
 
    /**
     * Permanently removes a supplier.
     */
    public void deleteSupplier(int supplierId) throws Exception {
        String sql = "DELETE FROM supplier WHERE supplier_id = ?";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
 
            statement.setInt(1, supplierId);
            statement.executeUpdate();
        }
    }
}