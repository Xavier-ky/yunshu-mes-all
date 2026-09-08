package com.yunshu.mes.planning.repository;

import com.yunshu.mes.planning.vo.CustomerOrderVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 客户订单数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class CustomerOrderRepository {

    private static final String SELECT_LIST = """
            SELECT o.order_id, o.order_no, o.customer_name,
                   (SELECT oi.product_id FROM customer_order_item oi WHERE oi.order_id = o.order_id ORDER BY oi.order_item_id LIMIT 1) AS product_id,
                   (SELECT p.product_name FROM customer_order_item oi JOIN product p ON p.product_id = oi.product_id WHERE oi.order_id = o.order_id ORDER BY oi.order_item_id LIMIT 1) AS product_name,
                   (SELECT COALESCE(SUM(oi.order_qty), 0) FROM customer_order_item oi WHERE oi.order_id = o.order_id) AS order_qty,
                   o.delivery_date, o.status
            FROM customer_order o
            WHERE o.is_deleted = 0
            ORDER BY o.created_at DESC, o.order_id DESC
            """;
    private static final String SELECT_BY_ID = """
            SELECT o.order_id, o.order_no, o.customer_name,
                   (SELECT oi.product_id FROM customer_order_item oi WHERE oi.order_id = o.order_id ORDER BY oi.order_item_id LIMIT 1) AS product_id,
                   (SELECT p.product_name FROM customer_order_item oi JOIN product p ON p.product_id = oi.product_id WHERE oi.order_id = o.order_id ORDER BY oi.order_item_id LIMIT 1) AS product_name,
                   (SELECT COALESCE(SUM(oi.order_qty), 0) FROM customer_order_item oi WHERE oi.order_id = o.order_id) AS order_qty,
                   o.delivery_date, o.status
            FROM customer_order o
            WHERE o.order_id = ? AND o.is_deleted = 0
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public CustomerOrderRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<CustomerOrderVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new CustomerOrderVO(
                rs.getLong("order_id"),
                rs.getString("order_no"),
                rs.getString("customer_name"),
                rs.getObject("product_id") != null ? rs.getLong("product_id") : null,
                rs.getString("product_name"),
                rs.getLong("order_qty"),
                rs.getString("delivery_date"),
                rs.getString("status")));
    }

    public Optional<CustomerOrderVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<CustomerOrderVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new CustomerOrderVO(
                rs.getLong("order_id"),
                rs.getString("order_no"),
                rs.getString("customer_name"),
                rs.getObject("product_id") != null ? rs.getLong("product_id") : null,
                rs.getString("product_name"),
                rs.getLong("order_qty"),
                rs.getString("delivery_date"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public record OrderLineInsert(Long productId, Long orderQty) {
    }

    public Long insertWithItems(
            String orderNo,
            String customerName,
            String deliveryDate,
            String status,
            String remark,
            List<OrderLineInsert> items) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO customer_order (order_no, customer_name, order_date, delivery_date, status, remark) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, orderNo);
            ps.setString(2, customerName);
            ps.setDate(3, new java.sql.Date(System.currentTimeMillis()));
            ps.setString(4, deliveryDate);
            ps.setString(5, status);
            ps.setString(6, remark);
            return ps;
        }, keyHolder);
        Number orderId = keyHolder.getKey();
        if (orderId == null || items == null || items.isEmpty()) {
            return orderId == null ? null : orderId.longValue();
        }
        long oid = orderId.longValue();
        for (OrderLineInsert line : items) {
            jdbc.update("INSERT INTO customer_order_item (order_id, product_id, order_qty) VALUES (?, ?, ?)",
                    oid, line.productId(), line.orderQty());
        }
        return oid;
    }

    public List<CustomerOrderVO> findByCustomerName(String customerName) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query("""
                SELECT o.order_id, o.order_no, o.customer_name,
                       (SELECT oi.product_id FROM customer_order_item oi WHERE oi.order_id = o.order_id ORDER BY oi.order_item_id LIMIT 1) AS product_id,
                       (SELECT p.product_name FROM customer_order_item oi JOIN product p ON p.product_id = oi.product_id WHERE oi.order_id = o.order_id ORDER BY oi.order_item_id LIMIT 1) AS product_name,
                       (SELECT COALESCE(SUM(oi.order_qty), 0) FROM customer_order_item oi WHERE oi.order_id = o.order_id) AS order_qty,
                       o.delivery_date, o.status
                FROM customer_order o
                WHERE o.customer_name = ? AND o.is_deleted = 0
                ORDER BY o.created_at DESC, o.order_id DESC
                """, (rs, n) -> new CustomerOrderVO(
                rs.getLong("order_id"),
                rs.getString("order_no"),
                rs.getString("customer_name"),
                rs.getObject("product_id") != null ? rs.getLong("product_id") : null,
                rs.getString("product_name"),
                rs.getLong("order_qty"),
                rs.getString("delivery_date"),
                rs.getString("status")), customerName);
    }

    public Long insert(String orderNo, String customerName, Long productId, Long orderQty, String deliveryDate, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO customer_order (order_no, customer_name, order_date, delivery_date, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, orderNo);
            ps.setString(2, customerName);
            ps.setDate(3, new java.sql.Date(System.currentTimeMillis()));
            ps.setString(4, deliveryDate);
            ps.setString(5, status);
            return ps;
        }, keyHolder);
        Number orderId = keyHolder.getKey();
        if (orderId != null && productId != null) {
            jdbc.update("INSERT INTO customer_order_item (order_id, product_id, order_qty) VALUES (?, ?, ?)",
                    orderId.longValue(), productId, orderQty);
        }
        return orderId == null ? null : orderId.longValue();
    }

    public void update(Long id, String orderNo, String customerName, Long productId, Long orderQty, String deliveryDate, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE customer_order SET order_no = ?, customer_name = ?, delivery_date = ?, status = ? WHERE order_id = ?",
                orderNo, customerName, deliveryDate, status, id);
        if (productId != null) {
            jdbc.update("UPDATE customer_order_item SET product_id = ?, order_qty = ? WHERE order_id = ?",
                    productId, orderQty, id);
        }
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM customer_order WHERE order_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
