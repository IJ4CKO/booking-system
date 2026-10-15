package com.bookingapp.dao;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
 
import com.bookingapp.model.PurchaseOrder;
import com.bookingapp.model.PurchaseOrderLine;
 
/**
 * Data access object for the {purchase_order} and
 * {purchase_order_line} tables, which record stock ordered from
 * suppliers.
 */
public class PurchaseOrderDAO {
 
    /**
     * Retrieves every purchase order, newest first, opening and closing its
     * own connection.
     */
    public List<PurchaseOrder> getAllPurchaseOrders() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return getAllPurchaseOrders(conn);
        }
    }
 
    /**
     * Retrieves every purchase order, newest first, using a connection
     * supplied by the caller.
     */
    public List<PurchaseOrder> getAllPurchaseOrders(Connection conn) throws Exception {
        String sql = "SELECT purchase_order_id, supplier_id, order_date, expected_delivery_date, status, total_cost, notes FROM purchase_order ORDER BY order_date DESC, purchase_order_id DESC";
        List<PurchaseOrder> purchaseOrders = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                purchaseOrders.add(mapPurchaseOrder(results));
            }
        }
        return purchaseOrders;
    }
 
    /**
     * Retrieves a single purchase order header, opening and closing its own
     * connection.
     */
    public PurchaseOrder getPurchaseOrderById(int purchaseOrderId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return getPurchaseOrderById(conn, purchaseOrderId);
        }
    }
 
    /**
     * Retrieves a single purchase order header, using a connection supplied by
     * the caller.
     */
    public PurchaseOrder getPurchaseOrderById(Connection conn, int purchaseOrderId) throws Exception {
        String sql = "SELECT purchase_order_id, supplier_id, order_date, expected_delivery_date, status, total_cost, notes FROM purchase_order WHERE purchase_order_id = ? ";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, purchaseOrderId);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return mapPurchaseOrder(results);
                }
            }
        }
        return null;
    }
 
    /**
     * Creates a purchase order header, opening and closing its own connection.
     */
    public int addPurchaseOrder(PurchaseOrder purchaseOrder) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return addPurchaseOrder(conn, purchaseOrder);
        }
    }
 
    /**
     * Creates a purchase order header using an existing database transaction.
     */
    public int addPurchaseOrder(Connection conn, PurchaseOrder purchaseOrder) throws Exception {
        String sql = "INSERT INTO purchase_order ( supplier_id, order_date, expected_delivery_date, status, total_cost, notes ) VALUES (?, ?, ?, ?, ?, ?) ";
        try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, purchaseOrder.getSupplierId());
            statement.setString(2, purchaseOrder.getOrderDate());
            setNullableString(statement, 3, purchaseOrder.getExpectedDeliveryDate());
            statement.setString(4, purchaseOrder.getStatus());
            setNullableDouble(statement, 5, purchaseOrder.getTotalCost());
            setNullableString(statement, 6, purchaseOrder.getNotes());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("The purchase order could not be created.");
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int purchaseOrderId = generatedKeys.getInt(1);
                    purchaseOrder.setPurchaseOrderId(purchaseOrderId);
                    return purchaseOrderId;
                }
            }
        }
        throw new Exception("The purchase order ID could not be retrieved.");
    }
 
    /**
     * Overwrites every editable field of a purchase order header, opening and
     * closing its own connection.
     */
    public void updatePurchaseOrder(PurchaseOrder purchaseOrder) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            updatePurchaseOrder(conn, purchaseOrder);
        }
    }
 
    /**
     * Overwrites every editable field of a purchase order header using an
     * existing database transaction.
     */
    public void updatePurchaseOrder(Connection conn, PurchaseOrder purchaseOrder) throws Exception {
        String sql = "UPDATE purchase_order SET supplier_id = ?, order_date = ?, expected_delivery_date = ?, status = ?, total_cost = ?, notes = ? WHERE purchase_order_id = ? ";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, purchaseOrder.getSupplierId());
            statement.setString(2, purchaseOrder.getOrderDate());
            setNullableString(statement, 3, purchaseOrder.getExpectedDeliveryDate());
            statement.setString(4, purchaseOrder.getStatus());
            setNullableDouble(statement, 5, purchaseOrder.getTotalCost());
            setNullableString(statement, 6, purchaseOrder.getNotes());
            statement.setInt(7, purchaseOrder.getPurchaseOrderId());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("The purchase order was not found.");
            }
        }
    }
 
    /**
     * Sets the status of a purchase order, opening and closing its own
     * connection.
     */
    public void updatePurchaseOrderStatus(int purchaseOrderId, String status) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            updatePurchaseOrderStatus(conn, purchaseOrderId, status);
        }
    }
 
    /**
     * Sets the status of a purchase order using an existing database
     * transaction.
     */
    public void updatePurchaseOrderStatus(Connection conn, int purchaseOrderId, String status) throws Exception {
        String sql = "UPDATE purchase_order SET status = ? WHERE purchase_order_id = ? ";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, purchaseOrderId);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("The purchase order was not found.");
            }
        }
    }
 
    /**
     * Sets the total cost of a purchase order, opening and closing its own
     * connection.
     */
    public void updatePurchaseOrderTotalCost(int purchaseOrderId, double totalCost) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            updatePurchaseOrderTotalCost(conn, purchaseOrderId, totalCost);
        }
    }
 
    /**
     * Sets the total cost of a purchase order using an existing database
     * transaction.
     */
    public void updatePurchaseOrderTotalCost(Connection conn, int purchaseOrderId, double totalCost) throws Exception {
        String sql = "UPDATE purchase_order SET total_cost = ? WHERE purchase_order_id = ? ";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDouble(1, totalCost);
            statement.setInt(2, purchaseOrderId);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("The purchase order was not found.");
            }
        }
    }
 
    /**
     * Retrieves the lines of one purchase order, opening and closing its own
     * connection.
     */
    public List<PurchaseOrderLine> getPurchaseOrderLines(int purchaseOrderId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return getPurchaseOrderLines(conn, purchaseOrderId);
        }
    }
 
    /**
     * Retrieves the lines of one purchase order, using a connection supplied
     * by the caller.
     */
    public List<PurchaseOrderLine> getPurchaseOrderLines(Connection conn, int purchaseOrderId) throws Exception {
        String sql = "SELECT purchase_order_line_id, purchase_order_id, inventory_item_id, quantity_ordered, unit_cost FROM purchase_order_line WHERE purchase_order_id = ? ORDER BY purchase_order_line_id ASC ";
        List<PurchaseOrderLine> lines = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, purchaseOrderId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    lines.add(mapPurchaseOrderLine(results));
                }
            }
        }
        return lines;
    }
 
    /**
     * Adds a line to a purchase order, opening and closing its own connection.
     */
    public void addPurchaseOrderLine(PurchaseOrderLine line) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            addPurchaseOrderLine(conn, line);
        }
    }
 
    /**
     * Adds a line to a purchase order using an existing database transaction.
     */
    public int addPurchaseOrderLine(Connection conn, PurchaseOrderLine line) throws Exception {
        String sql = "INSERT INTO purchase_order_line ( purchase_order_id, inventory_item_id, quantity_ordered, unit_cost ) VALUES (?, ?, ?, ?) ";
        try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, line.getPurchaseOrderId());
            statement.setInt(2, line.getInventoryItemId());
            statement.setDouble(3, line.getQuantityOrdered());
            statement.setDouble(4, line.getUnitCost());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("The purchase order line could not be created.");
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int lineId = generatedKeys.getInt(1);
                    line.setPurchaseOrderLineId(lineId);
                    return lineId;
                }
            }
        }
        throw new Exception("The purchase order line ID could not be retrieved.");
    }
 
    /**
     * Removes a single line from a purchase order, opening and closing its own
     * connection.
     */
    public void deletePurchaseOrderLine(int purchaseOrderLineId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            deletePurchaseOrderLine(conn, purchaseOrderLineId);
        }
    }
 
    /**
     * Removes a single line from a purchase order using an existing database
     * transaction.
     */
    public void deletePurchaseOrderLine(Connection conn, int purchaseOrderLineId) throws Exception {
        String sql = "DELETE FROM purchase_order_line WHERE purchase_order_line_id = ? ";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, purchaseOrderLineId);
            statement.executeUpdate();
        }
    }
 
    /**
     * Removes every line from a purchase order, opening and closing its own
     * connection.
     */
    public void deleteAllPurchaseOrderLines(int purchaseOrderId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            deleteAllPurchaseOrderLines(conn, purchaseOrderId);
        }
    }
 
    /**
     * Removes every line from a purchase order using an existing database
     * transaction.
     */
    public void deleteAllPurchaseOrderLines(Connection conn, int purchaseOrderId) throws Exception {
        String sql = " DELETE FROM purchase_order_line WHERE purchase_order_id = ? ";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, purchaseOrderId);
            statement.executeUpdate();
        }
    }

    /**
     * Saves a purchase order and its lines together, as a single transaction.
     */
    public int savePurchaseOrderWithLines(PurchaseOrder purchaseOrder, List<PurchaseOrderLine> lines) throws Exception {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("A purchase order must contain at least one item.");
        }
        try (Connection conn = DBConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int purchaseOrderId;
                if (purchaseOrder.getPurchaseOrderId() <= 0) {
                    purchaseOrderId = addPurchaseOrder(conn, purchaseOrder);
                } else {
                    purchaseOrderId = purchaseOrder.getPurchaseOrderId();
                    updatePurchaseOrder(conn, purchaseOrder);
                    deleteAllPurchaseOrderLines(conn, purchaseOrderId);
                }
                for (PurchaseOrderLine line : lines) {
                    line.setPurchaseOrderId(purchaseOrderId);
                    addPurchaseOrderLine(conn, line);
                }
                double totalCost = calculateTotalCostFromLines(conn, purchaseOrderId);
                updatePurchaseOrderTotalCost(conn, purchaseOrderId, totalCost);
                purchaseOrder.setTotalCost(totalCost);
                conn.commit();
                return purchaseOrderId;
            } catch (Exception e) {
                rollbackTransaction(conn, e);
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }
 
 /**
 * Permanently removes a purchase order and its lines, as a single
 * transaction.
 */
    public void deletePurchaseOrder(int purchaseOrderId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                deletePurchaseOrder(conn, purchaseOrderId);
                conn.commit();
            } catch (Exception e) {
                rollbackTransaction(conn, e);
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }
 
    /**
     * Permanently removes a purchase order and its lines using an existing
     * database transaction.
     */
    public void deletePurchaseOrder(Connection conn, int purchaseOrderId) throws Exception {
        deleteAllPurchaseOrderLines(conn, purchaseOrderId);
        String sql = "DELETE FROM purchase_order WHERE purchase_order_id = ? ";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, purchaseOrderId);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("The purchase order was not found.");
            }
        }
    }
 
    /**
     * Works out what a purchase order comes to, from its lines, opening and
     * closing its own connection.
     */
    public double calculateTotalCostFromLines(int purchaseOrderId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return calculateTotalCostFromLines(conn, purchaseOrderId);
        }
    }
 
    /**
     * Works out what a purchase order comes to, from its lines, using a
     * connection supplied by the caller.
     */
    public double calculateTotalCostFromLines(Connection conn, int purchaseOrderId) throws Exception {
        String sql = " SELECT SUM(quantity_ordered * unit_cost) AS calculated_total FROM purchase_order_line WHERE purchase_order_id = ? ";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, purchaseOrderId);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    double total = results.getDouble("calculated_total");
                    return results.wasNull() ? 0.0 : total;
                }
            }
        }
        return 0.0;
    }
 
    /**
     * Returns stock that is already on ordered purchase orders and is expected
     * to arrive by the supplied booking date.
     *
     * This quantity is for planning information only. It is not treated as
     * physically available stock until the purchase order has been received.
     */
    public double getExpectedIncomingQuantity(
            int inventoryItemId,
            LocalDate requiredByDate
    ) throws Exception {
 
        if (inventoryItemId <= 0
                || requiredByDate == null) {
 
            return 0.0;
        }
 
        String sql = """
        SELECT COALESCE(
            SUM(pol.quantity_ordered),
            0
        ) AS expected_quantity
        FROM purchase_order_line pol
        JOIN purchase_order po
          ON po.purchase_order_id =
             pol.purchase_order_id
        WHERE pol.inventory_item_id = ?
          AND UPPER(po.status) = 'ORDERED'
          AND po.expected_delivery_date IS NOT NULL
          AND DATE(po.expected_delivery_date)
              >= DATE('now')
          AND DATE(po.expected_delivery_date)
              <= DATE(?)
        """;
 
        try (Connection conn
                = DBConnection.getConnection(); PreparedStatement statement
                = conn.prepareStatement(sql)) {
 
            statement.setInt(
                    1,
                    inventoryItemId
            );
 
            statement.setString(
                    2,
                    requiredByDate.toString()
            );
 
            try (ResultSet results
                    = statement.executeQuery()) {
 
                if (results.next()) {
                    return results.getDouble(
                            "expected_quantity"
                    );
                }
            }
        }
 
        return 0.0;
    }
 
    /**
     * Builds a {PurchaseOrder} from the current row of a result set.
     */
    private PurchaseOrder mapPurchaseOrder(ResultSet results) throws Exception {
        return new PurchaseOrder(results.getInt("purchase_order_id"), results.getInt("supplier_id"), results.getString("order_date"), results.getString("expected_delivery_date"), results.getString("status"), getNullableDouble(results, "total_cost"), results.getString("notes"));
    }
 
    /**
     * Builds a {PurchaseOrderLine} from the current row of a result set.

     */
    private PurchaseOrderLine mapPurchaseOrderLine(ResultSet results) throws Exception {
        return new PurchaseOrderLine(results.getInt("purchase_order_line_id"), results.getInt("purchase_order_id"), results.getInt("inventory_item_id"), results.getDouble("quantity_ordered"), results.getDouble("unit_cost"));
    }
 
    /**
     * Reads a decimal column that may be absent.
     */
    private Double getNullableDouble(ResultSet results, String columnName) throws Exception {
        double value = results.getDouble(columnName);
        if (results.wasNull()) {
            return null;
        }
        return value;
    }
 
    /**
     * Binds a decimal that may be absent, storing {NULL} where there is
     * no value.
     */
    private void setNullableDouble(PreparedStatement statement, int parameterIndex, Double value) throws Exception {
        if (value == null) {
            statement.setNull(parameterIndex, Types.REAL);
        } else {
            statement.setDouble(parameterIndex, value);
        }
    }
 
    /**
     * Binds a string that may be absent, storing {NULL} where there is
     * nothing to store.
     */
    private void setNullableString(PreparedStatement statement, int parameterIndex, String value) throws Exception {
        if (value == null || value.isBlank()) {
            statement.setNull(parameterIndex, Types.VARCHAR);
        } else {
            statement.setString(parameterIndex, value.trim());
        }
    }
 
    /**
     * Rolls a transaction back after a failure, without losing the original
     * cause.
     */
    private void rollbackTransaction(Connection conn, Exception originalException) {
        try {
            conn.rollback();
        } catch (Exception rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}