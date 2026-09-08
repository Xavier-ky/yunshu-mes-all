package com.yunshu.mes.planning.compat.repository;

import com.yunshu.mes.planning.compat.StatusMapper;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class CompatWorkorderRepository {

    private static final String BASE_SELECT = """
            SELECT wo.*, p.product_code, p.product_name, p.product_model,
                   co.order_no
            FROM work_order wo
            LEFT JOIN product p ON wo.product_id = p.product_id
            LEFT JOIN customer_order co ON wo.order_id = co.order_id
            WHERE wo.is_deleted = 0
            """;

    private final JdbcTemplate jdbc;

    public CompatWorkorderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> query, String apiStatusFilter) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, query, apiStatusFilter);
        sql.append(" ORDER BY wo.work_order_id DESC");
        return jdbc.query(sql.toString(), (rs, n) -> mapRow(rs), params.toArray());
    }

    public long count(Map<String, String> query, String apiStatusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM work_order wo LEFT JOIN product p ON wo.product_id = p.product_id WHERE wo.is_deleted = 0");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, query, apiStatusFilter);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, params.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> list = jdbc.query(BASE_SELECT + " AND wo.work_order_id = ?",
                (rs, n) -> mapRow(rs), id);
        return list.stream().findFirst();
    }

    public boolean existsCode(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        if (excludeId == null) {
            Long c = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM work_order WHERE work_order_no = ? AND is_deleted = 0", Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM work_order WHERE work_order_no = ? AND work_order_id <> ? AND is_deleted = 0",
                Long.class, code, excludeId);
        return c != null && c > 0;
    }

    public boolean orderMatchesProduct(Long orderId, Long productId) {
        if (orderId == null || productId == null) {
            return false;
        }
        Long cnt = jdbc.queryForObject("""
                SELECT COUNT(*) FROM customer_order_item WHERE order_id = ? AND product_id = ?
                """, Long.class, orderId, productId);
        return cnt != null && cnt > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO work_order
                    (work_order_no, work_order_name, parent_work_order_id, ancestors, work_order_type,
                     order_source, source_code, order_id, order_item_id, product_id, bom_id, route_id,
                     plan_qty, quantity_scheduled, quantity_changed, completed_qty,
                     client_id, client_code, client_name, request_date, status, lifecycle_status, remark)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, str(body, "workorderCode"));
            ps.setString(i++, str(body, "workorderName"));
            setLong(ps, i++, longVal(body, "parentId"));
            ps.setString(i++, strOr(body, "ancestors", "0"));
            ps.setString(i++, strOr(body, "workorderType", "SELF"));
            ps.setString(i++, strOr(body, "orderSource", "ORDER"));
            ps.setString(i++, str(body, "sourceCode"));
            setLong(ps, i++, longVal(body, "orderId"));
            setLong(ps, i++, longVal(body, "orderItemId"));
            ps.setLong(i++, requireLong(body, "productId"));
            setLong(ps, i++, longVal(body, "bomId"));
            setLong(ps, i++, longVal(body, "routeId"));
            ps.setBigDecimal(i++, decimal(body, "quantity", BigDecimal.ONE));
            ps.setBigDecimal(i++, decimal(body, "quantityScheduled", BigDecimal.ZERO));
            ps.setBigDecimal(i++, decimal(body, "quantityChanged", BigDecimal.ZERO));
            ps.setBigDecimal(i++, decimal(body, "quantityProduced", BigDecimal.ZERO));
            setLong(ps, i++, longVal(body, "clientId"));
            ps.setString(i++, str(body, "clientCode"));
            ps.setString(i++, str(body, "clientName"));
            setTimestamp(ps, i++, parseDateTime(body.get("requestDate")));
            ps.setString(i++, StatusMapper.toYunshuWorkorderStatus(strOr(body, "status", "PREPARE")));
            ps.setString(i++, strOr(body, "lifecycleStatus", "RELEASED"));
            ps.setString(i, str(body, "remark"));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Long id, Map<String, Object> body) {
        return jdbc.update("""
                UPDATE work_order SET
                  work_order_no = COALESCE(?, work_order_no),
                  work_order_name = COALESCE(?, work_order_name),
                  work_order_type = COALESCE(?, work_order_type),
                  order_source = COALESCE(?, order_source),
                  source_code = COALESCE(?, source_code),
                  product_id = COALESCE(?, product_id),
                  bom_id = COALESCE(?, bom_id),
                  route_id = COALESCE(?, route_id),
                  plan_qty = COALESCE(?, plan_qty),
                  quantity_changed = COALESCE(?, quantity_changed),
                  client_code = COALESCE(?, client_code),
                  client_name = COALESCE(?, client_name),
                  request_date = COALESCE(?, request_date),
                  status = COALESCE(?, status),
                  remark = COALESCE(?, remark)
                WHERE work_order_id = ?
                """,
                str(body, "workorderCode"), str(body, "workorderName"), str(body, "workorderType"),
                str(body, "orderSource"), str(body, "sourceCode"), longVal(body, "productId"),
                longVal(body, "bomId"), longVal(body, "routeId"), decimal(body, "quantity", null),
                decimal(body, "quantityChanged", null), str(body, "clientCode"), str(body, "clientName"),
                parseDateTime(body.get("requestDate")),
                body.get("status") != null ? StatusMapper.toYunshuWorkorderStatus(String.valueOf(body.get("status"))) : null,
                str(body, "remark"), id);
    }

    public int delete(Long id) {
        return jdbc.update("UPDATE work_order SET is_deleted = 1 WHERE work_order_id = ?", id);
    }

    public int finish(Long id) {
        return jdbc.update("UPDATE work_order SET status = 'COMPLETED', finish_date = NOW(3) WHERE work_order_id = ?", id);
    }

    public int cancel(Long id) {
        return jdbc.update("UPDATE work_order SET status = 'CANCELLED', cancel_date = NOW(3) WHERE work_order_id = ?", id);
    }

    public Optional<Long> findDefaultBomId(Long productId) {
        List<Long> ids = jdbc.query(
                "SELECT bom_id FROM bom WHERE product_id = ? ORDER BY bom_id DESC LIMIT 1",
                (rs, n) -> rs.getLong("bom_id"), productId);
        return ids.stream().findFirst();
    }

    public List<Map<String, Object>> findBomItems(Long bomId) {
        return jdbc.query("""
                SELECT bi.*, m.material_code, m.material_name, m.material_type
                FROM bom_item bi
                JOIN material m ON bi.material_id = m.material_id
                WHERE bi.bom_id = ?
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("materialId", rs.getLong("material_id"));
            row.put("materialCode", rs.getString("material_code"));
            row.put("materialName", rs.getString("material_name"));
            row.put("qtyPer", rs.getBigDecimal("qty_per"));
            row.put("materialType", rs.getString("material_type"));
            return row;
        }, bomId);
    }

    private void appendFilters(StringBuilder sql, List<Object> params, Map<String, String> query, String apiStatusFilter) {
        if (StringUtils.hasText(apiStatusFilter)) {
            sql.append(" AND wo.status IN (");
            List<String> yunshu = yunshuStatusesForApi(apiStatusFilter);
            sql.append(String.join(",", yunshu.stream().map(s -> "?").toList()));
            sql.append(")");
            params.addAll(yunshu);
        }
        like(sql, params, query, "workorderCode", "wo.work_order_no");
        like(sql, params, query, "workorderName", "wo.work_order_name");
        like(sql, params, query, "sourceCode", "wo.source_code");
        like(sql, params, query, "productCode", "p.product_code");
        like(sql, params, query, "productName", "p.product_name");
        like(sql, params, query, "clientCode", "wo.client_code");
        like(sql, params, query, "clientName", "wo.client_name");
        if (StringUtils.hasText(query.get("orderId"))) {
            sql.append(" AND wo.order_id = ?");
            params.add(Long.parseLong(query.get("orderId")));
        }
    }

    private static List<String> yunshuStatusesForApi(String apiStatus) {
        return switch (apiStatus) {
            case "PREPARE" -> List.of("CREATED");
            case "CONFIRMED" -> List.of("CONFIRMED", "DISPATCHED", "RUNNING");
            case "FINISHED" -> List.of("COMPLETED");
            case "CANCELED" -> List.of("CANCELLED");
            default -> List.of(apiStatus);
        };
    }

    private static void like(StringBuilder sql, List<Object> params, Map<String, String> query, String key, String col) {
        String v = query.get(key);
        if (StringUtils.hasText(v)) {
            sql.append(" AND ").append(col).append(" LIKE ?");
            params.add("%" + v.trim() + "%");
        }
    }

    private Map<String, Object> mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        long id = rs.getLong("work_order_id");
        m.put("workorderId", id);
        m.put("workorderCode", rs.getString("work_order_no"));
        m.put("workorderName", rs.getString("work_order_name"));
        m.put("parentId", rs.getObject("parent_work_order_id"));
        m.put("ancestors", rs.getString("ancestors"));
        m.put("workorderType", rs.getString("work_order_type"));
        m.put("orderSource", rs.getString("order_source"));
        m.put("sourceCode", rs.getString("source_code"));
        m.put("orderId", rs.getObject("order_id"));
        m.put("orderItemId", rs.getObject("order_item_id"));
        m.put("productId", rs.getLong("product_id"));
        m.put("productCode", rs.getString("product_code"));
        m.put("productName", rs.getString("product_name"));
        m.put("productSpc", rs.getString("product_model"));
        m.put("unitName", "件");
        m.put("unitOfMeasure", "PCS");
        m.put("quantity", rs.getBigDecimal("plan_qty"));
        m.put("quantityProduced", rs.getBigDecimal("completed_qty"));
        m.put("quantityScheduled", rs.getBigDecimal("quantity_scheduled"));
        m.put("quantityChanged", rs.getBigDecimal("quantity_changed"));
        m.put("clientId", rs.getObject("client_id"));
        m.put("clientCode", rs.getString("client_code"));
        m.put("clientName", rs.getString("client_name"));
        m.put("requestDate", rs.getTimestamp("request_date"));
        m.put("bomId", rs.getObject("bom_id"));
        m.put("routeId", rs.getObject("route_id"));
        m.put("status", StatusMapper.toApiWorkorderStatus(rs.getString("status")));
        try {
            m.put("lifecycleStatus", rs.getString("lifecycle_status"));
        } catch (java.sql.SQLException ignored) {
            m.put("lifecycleStatus", null);
        }
        m.put("remark", rs.getString("remark"));
        m.put("orderNo", rs.getString("order_no"));
        return m;
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String strOr(Map<String, Object> body, String key, String def) {
        String s = str(body, key);
        return StringUtils.hasText(s) ? s : def;
    }

    private static Long longVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || "".equals(v) || "0".equals(String.valueOf(v))) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    private static long requireLong(Map<String, Object> body, String key) {
        Long v = longVal(body, key);
        if (v == null) {
            throw new IllegalArgumentException(key + " required");
        }
        return v;
    }

    private static BigDecimal decimal(Map<String, Object> body, String key, BigDecimal def) {
        Object v = body.get(key);
        if (v == null || "".equals(v)) {
            return def;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(String.valueOf(v));
    }

    private static void setLong(PreparedStatement ps, int idx, Long v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(idx, Types.BIGINT);
        } else {
            ps.setLong(idx, v);
        }
    }

    private static void setTimestamp(PreparedStatement ps, int idx, Timestamp ts) throws java.sql.SQLException {
        if (ts == null) {
            ps.setNull(idx, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(idx, ts);
        }
    }

    private static Timestamp parseDateTime(Object v) {
        if (v == null || "".equals(v)) {
            return null;
        }
        String s = String.valueOf(v);
        try {
            if (s.length() <= 10) {
                return Timestamp.valueOf(LocalDate.parse(s).atStartOfDay());
            }
            return Timestamp.valueOf(LocalDateTime.parse(s.replace(" ", "T")));
        } catch (Exception e) {
            return Timestamp.valueOf(LocalDate.parse(s.substring(0, 10)).atStartOfDay());
        }
    }
}
