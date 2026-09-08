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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class WmLocationRepository {

    private static final String BASE = """
            SELECT zone_id, zone_code, zone_name, warehouse_id, area, area_flag, frozen_flag, remark,
                   attr1, attr2, attr3, attr4, create_by, create_time, update_by, update_time
            FROM storage_zone
            """;

    private final JdbcTemplate jdbc;

    public WmLocationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> query, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query);
        sql.append(" ORDER BY zone_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapRow(rs), args.toArray());
    }

    public long count(Map<String, String> query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM storage_zone WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE zone_id = ?", (rs, n) -> mapRow(rs), id);
        return rows.stream().findFirst();
    }

    public boolean existsCode(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM storage_zone WHERE zone_code = ?", Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_zone WHERE zone_code = ? AND zone_id <> ?", Long.class, code, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO storage_zone (zone_code, zone_name, warehouse_id, area, area_flag, frozen_flag, remark, create_time)
                    VALUES (?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, str(body, "locationCode"));
            ps.setString(2, str(body, "locationName"));
            ps.setLong(3, Long.parseLong(String.valueOf(body.get("warehouseId"))));
            setDouble(ps, 4, body.get("area"));
            ps.setString(5, strOr(body, "areaFlag", "Y"));
            ps.setString(6, strOr(body, "frozenFlag", "N"));
            ps.setString(7, str(body, "remark"));
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Long id, Map<String, Object> body) {
        if (body.containsKey("frozenFlag") && body.size() <= 3) {
            return jdbc.update("UPDATE storage_zone SET frozen_flag = ?, update_time = ? WHERE zone_id = ?",
                    str(body, "frozenFlag"), Timestamp.valueOf(LocalDateTime.now()), id);
        }
        return jdbc.update("""
                UPDATE storage_zone SET zone_code=?, zone_name=?, warehouse_id=?, area=?, area_flag=?, remark=?, update_time=?
                WHERE zone_id=?
                """,
                str(body, "locationCode"),
                str(body, "locationName"),
                Long.parseLong(String.valueOf(body.get("warehouseId"))),
                doubleVal(body.get("area")),
                strOr(body, "areaFlag", "Y"),
                str(body, "remark"),
                Timestamp.valueOf(LocalDateTime.now()),
                id);
    }

    public int delete(Long id) {
        Long bins = jdbc.queryForObject("SELECT COUNT(*) FROM storage_bin WHERE zone_id = ?", Long.class, id);
        if (bins != null && bins > 0) {
            throw new IllegalArgumentException("库区下存在库位，不能删除！");
        }
        return jdbc.update("DELETE FROM storage_zone WHERE zone_id = ?", id);
    }

    public int setProductMixing(Long zoneId, String flag) {
        return jdbc.update("UPDATE storage_bin SET product_mixing = ?, update_time = ? WHERE zone_id = ?",
                flag, Timestamp.valueOf(LocalDateTime.now()), zoneId);
    }

    public int setBatchMixing(Long zoneId, String flag) {
        return jdbc.update("UPDATE storage_bin SET batch_mixing = ?, update_time = ? WHERE zone_id = ?",
                flag, Timestamp.valueOf(LocalDateTime.now()), zoneId);
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> query) {
        if (StringUtils.hasText(query.get("warehouseId"))) {
            sql.append(" AND warehouse_id = ?");
            args.add(Long.parseLong(query.get("warehouseId").trim()));
        }
        if (StringUtils.hasText(query.get("locationCode"))) {
            sql.append(" AND zone_code LIKE ?");
            args.add("%" + query.get("locationCode").trim() + "%");
        }
        if (StringUtils.hasText(query.get("locationName"))) {
            sql.append(" AND zone_name LIKE ?");
            args.add("%" + query.get("locationName").trim() + "%");
        }
    }

    static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("locationId", rs.getLong("zone_id"));
        m.put("locationCode", rs.getString("zone_code"));
        m.put("locationName", rs.getString("zone_name"));
        m.put("warehouseId", rs.getLong("warehouse_id"));
        m.put("area", rs.getObject("area"));
        m.put("areaFlag", rs.getString("area_flag"));
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

    private static void setDouble(PreparedStatement ps, int idx, Object v) throws SQLException {
        Double d = doubleVal(v);
        if (d == null) {
            ps.setNull(idx, Types.DOUBLE);
        } else {
            ps.setDouble(idx, d);
        }
    }
}
