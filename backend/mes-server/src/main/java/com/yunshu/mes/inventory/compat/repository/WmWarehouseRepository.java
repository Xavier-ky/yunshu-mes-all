package com.yunshu.mes.inventory.compat.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class WmWarehouseRepository {

    private static final String BASE = """
            SELECT warehouse_id, warehouse_code, warehouse_name, warehouse_type, status,
                   location, area, user_id, user_name, charge, frozen_flag, enable_flag, remark,
                   attr1, attr2, attr3, attr4, create_by, create_time, update_by, update_time
            FROM warehouse
            """;

    private final JdbcTemplate jdbc;

    public WmWarehouseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> query, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query);
        sql.append(" ORDER BY warehouse_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapRow(rs), args.toArray());
    }

    public long count(Map<String, String> query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM warehouse WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE warehouse_id = ?",
                (rs, n) -> mapRow(rs), id);
        return rows.stream().findFirst();
    }

    public boolean existsCode(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM warehouse WHERE warehouse_code = ?", Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM warehouse WHERE warehouse_code = ? AND warehouse_id <> ?",
                Long.class, code, excludeId);
        return c != null && c > 0;
    }

    public boolean existsName(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM warehouse WHERE warehouse_name = ?", Long.class, name);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM warehouse WHERE warehouse_name = ? AND warehouse_id <> ?",
                Long.class, name, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO warehouse (warehouse_code, warehouse_name, warehouse_type, status,
                      location, area, user_id, user_name, charge, frozen_flag, enable_flag, remark,
                      create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, str(body, "warehouseCode"));
            ps.setString(2, str(body, "warehouseName"));
            ps.setString(3, strOr(body, "warehouseType", "RAW"));
            ps.setString(4, "ENABLED");
            ps.setString(5, str(body, "location"));
            setDouble(ps, 6, body.get("area"));
            setLong(ps, 7, body.get("userId"));
            ps.setString(8, str(body, "userName"));
            ps.setString(9, str(body, "charge"));
            ps.setString(10, strOr(body, "frozenFlag", "N"));
            ps.setString(11, strOr(body, "enableFlag", "Y"));
            ps.setString(12, str(body, "remark"));
            ps.setString(13, strOr(body, "createBy", "system"));
            ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Long id, Map<String, Object> body) {
        return jdbc.update("""
                UPDATE warehouse SET warehouse_code=?, warehouse_name=?, location=?, area=?,
                  user_id=?, user_name=?, charge=?, frozen_flag=?, remark=?, update_by=?, update_time=?
                WHERE warehouse_id=?
                """,
                str(body, "warehouseCode"),
                str(body, "warehouseName"),
                str(body, "location"),
                doubleVal(body.get("area")),
                longVal(body.get("userId")),
                str(body, "userName"),
                str(body, "charge"),
                strOr(body, "frozenFlag", "N"),
                str(body, "remark"),
                strOr(body, "updateBy", "system"),
                Timestamp.valueOf(LocalDateTime.now()),
                id);
    }

    public int updateFrozenOnly(Long id, String frozenFlag) {
        return jdbc.update("UPDATE warehouse SET frozen_flag = ?, update_time = ? WHERE warehouse_id = ?",
                frozenFlag, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public int delete(Long id) {
        Long zones = jdbc.queryForObject("SELECT COUNT(*) FROM storage_zone WHERE warehouse_id = ?", Long.class, id);
        if (zones != null && zones > 0) {
            throw new IllegalArgumentException("仓库下存在库区，不能删除！");
        }
        return jdbc.update("DELETE FROM warehouse WHERE warehouse_id = ?", id);
    }

    public List<Map<String, Object>> treeRows() {
        return jdbc.query("""
                SELECT w.warehouse_id, w.warehouse_code, w.warehouse_name, w.location, w.area, w.charge,
                       w.frozen_flag, w.remark, w.attr1, w.attr2, w.attr3, w.attr4,
                       w.create_by, w.create_time, w.update_by, w.update_time,
                       sz.zone_id AS location_id, sz.zone_code AS location_code, sz.zone_name AS location_name,
                       sz.area_flag,
                       sb.bin_id AS area_id, sb.bin_code AS area_code, sb.bin_name AS area_name,
                       sb.max_loa, sb.position_x, sb.position_y, sb.position_z
                FROM warehouse w
                LEFT JOIN storage_zone sz ON w.warehouse_id = sz.warehouse_id
                LEFT JOIN storage_bin sb ON sz.zone_id = sb.zone_id AND IFNULL(sb.enable_flag,'Y') = 'Y'
                ORDER BY w.warehouse_id, sz.zone_id, sb.bin_id
                """, (rs, n) -> mapTreeRow(rs));
    }

    public List<Map<String, Object>> buildTree(List<Map<String, Object>> flat) {
        Map<Long, Map<String, Object>> warehouses = new LinkedHashMap<>();
        Map<Long, Map<String, Object>> zones = new LinkedHashMap<>();
        for (Map<String, Object> row : flat) {
            Long whId = longObj(row.get("warehouseId"));
            if (whId == null) {
                continue;
            }
            Map<String, Object> wh = warehouses.computeIfAbsent(whId, id -> {
                Map<String, Object> w = new LinkedHashMap<>();
                w.put("warehouseId", row.get("warehouseId"));
                w.put("warehouseCode", row.get("warehouseCode"));
                w.put("warehouseName", row.get("warehouseName"));
                w.put("location", row.get("location"));
                w.put("area", row.get("area"));
                w.put("charge", row.get("charge"));
                w.put("frozenFlag", row.get("frozenFlag"));
                w.put("remark", row.get("remark"));
                w.put("children", new ArrayList<Map<String, Object>>());
                return w;
            });
            Long zoneId = longObj(row.get("locationId"));
            if (zoneId == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> whChildren = (List<Map<String, Object>>) wh.get("children");
            Map<String, Object> zone = zones.computeIfAbsent(zoneId, id -> {
                Map<String, Object> z = new LinkedHashMap<>();
                z.put("locationId", zoneId);
                z.put("locationCode", row.get("locationCode"));
                z.put("locationName", row.get("locationName"));
                z.put("warehouseId", whId);
                z.put("areaFlag", row.get("areaFlag"));
                z.put("children", new ArrayList<Map<String, Object>>());
                whChildren.add(z);
                return z;
            });
            Long binId = longObj(row.get("areaId"));
            if (binId == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> zoneChildren = (List<Map<String, Object>>) zone.get("children");
            Set<Long> seen = new LinkedHashSet<>();
            for (Map<String, Object> c : zoneChildren) {
                seen.add(longObj(c.get("areaId")));
            }
            if (!seen.contains(binId)) {
                Map<String, Object> bin = new LinkedHashMap<>();
                bin.put("areaId", binId);
                bin.put("areaCode", row.get("areaCode"));
                bin.put("areaName", row.get("areaName"));
                bin.put("locationId", zoneId);
                bin.put("maxLoa", row.get("maxLoa"));
                bin.put("positionX", row.get("positionX"));
                bin.put("positionY", row.get("positionY"));
                bin.put("positionZ", row.get("positionZ"));
                zoneChildren.add(bin);
            }
        }
        return new ArrayList<>(warehouses.values());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> query) {
        if (StringUtils.hasText(query.get("warehouseCode"))) {
            sql.append(" AND warehouse_code LIKE ?");
            args.add("%" + query.get("warehouseCode").trim() + "%");
        }
        if (StringUtils.hasText(query.get("warehouseName"))) {
            sql.append(" AND warehouse_name LIKE ?");
            args.add("%" + query.get("warehouseName").trim() + "%");
        }
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("warehouseId", rs.getLong("warehouse_id"));
        m.put("warehouseCode", rs.getString("warehouse_code"));
        m.put("warehouseName", rs.getString("warehouse_name"));
        m.put("location", rs.getString("location"));
        m.put("area", rs.getObject("area"));
        m.put("userId", rs.getObject("user_id"));
        m.put("userName", rs.getString("user_name"));
        m.put("charge", rs.getString("charge"));
        m.put("frozenFlag", rs.getString("frozen_flag"));
        m.put("enableFlag", rs.getString("enable_flag"));
        m.put("remark", rs.getString("remark"));
        m.put("attr1", rs.getString("attr1"));
        m.put("attr2", rs.getString("attr2"));
        m.put("attr3", rs.getObject("attr3"));
        m.put("attr4", rs.getObject("attr4"));
        m.put("createBy", rs.getString("create_by"));
        m.put("createTime", rs.getTimestamp("create_time"));
        m.put("updateBy", rs.getString("update_by"));
        m.put("updateTime", rs.getTimestamp("update_time"));
        return m;
    }

    private Map<String, Object> mapTreeRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("warehouseId", rs.getLong("warehouse_id"));
        m.put("warehouseCode", rs.getString("warehouse_code"));
        m.put("warehouseName", rs.getString("warehouse_name"));
        m.put("location", rs.getString("location"));
        m.put("area", rs.getObject("area"));
        m.put("charge", rs.getString("charge"));
        m.put("frozenFlag", rs.getString("frozen_flag"));
        m.put("remark", rs.getString("remark"));
        m.put("attr1", rs.getString("attr1"));
        m.put("attr2", rs.getString("attr2"));
        m.put("attr3", rs.getObject("attr3"));
        m.put("attr4", rs.getObject("attr4"));
        m.put("createBy", rs.getString("create_by"));
        m.put("createTime", rs.getTimestamp("create_time"));
        m.put("updateBy", rs.getString("update_by"));
        m.put("updateTime", rs.getTimestamp("update_time"));
        Object locId = rs.getObject("location_id");
        m.put("locationId", locId);
        m.put("locationCode", rs.getString("location_code"));
        m.put("locationName", rs.getString("location_name"));
        m.put("areaFlag", rs.getString("area_flag"));
        m.put("areaId", rs.getObject("area_id"));
        m.put("areaCode", rs.getString("area_code"));
        m.put("areaName", rs.getString("area_name"));
        m.put("maxLoa", rs.getObject("max_loa"));
        m.put("positionX", rs.getObject("position_x"));
        m.put("positionY", rs.getObject("position_y"));
        m.put("positionZ", rs.getObject("position_z"));
        return m;
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String strOr(Map<String, Object> body, String key, String def) {
        String s = str(body, key);
        return s == null || s.isBlank() ? def : s;
    }

    private static Double doubleVal(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        return Double.valueOf(String.valueOf(v));
    }

    private static Long longVal(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(v));
    }

    private static Long longObj(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(v));
    }

    private static void setDouble(PreparedStatement ps, int idx, Object v) throws SQLException {
        Double d = doubleVal(v);
        if (d == null) {
            ps.setNull(idx, Types.DOUBLE);
        } else {
            ps.setDouble(idx, d);
        }
    }

    private static void setLong(PreparedStatement ps, int idx, Object v) throws SQLException {
        Long l = longVal(v);
        if (l == null) {
            ps.setNull(idx, Types.BIGINT);
        } else {
            ps.setLong(idx, l);
        }
    }
}
