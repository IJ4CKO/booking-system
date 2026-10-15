package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.Product;
 
/**
 * Data access object for the {products} table, the catalogue of things
 * a customer can order.
 */
public class ProductDAO {
 
    /**
     * Adds a new product to the catalogue.
     */
    public void addProduct(Product product) throws Exception {
        String sql = "INSERT INTO products (name, description, active) VALUES (?, ?, ?)";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
 
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setInt(3, product.isActive() ? 1 : 0);
            ps.executeUpdate();
        }
    }
 
    /**
     * Permanently removes a product from the catalogue.
     */
    public void deleteProduct(int productId) throws Exception {
        String sql = "DELETE FROM products WHERE product_id = ?";
 
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }
 
    /**
     * Retrieves a single product by its identifier.
     */
   public Product getProductById(int productId) throws Exception {
        String sql = "SELECT * FROM products WHERE product_id = ?";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
 
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getInt("product_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getInt("active") == 1
                    );
                }
            }
        }
 
        return null;
    }
 
    /**
     * Retrieves every product, in the order they were added.
     */
    public List<Product> getAllProducts() throws Exception {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY product_id";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
 
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("active") == 1
                ));
            }
        }
 
        return products;
    }
}