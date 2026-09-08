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
public class WmAreaRepository {

    private static final String BASE = """
            SELECT sb.bin_id, sb.bin_code, sb.bin_name, sb.zone_id, sb.area, sb.max_loa,
                   sb.position_x, sb.position_y, sb.position_z, sb.enable_flag, sb.frozen_flag,
                   sb.product_mixing, sb.batch_mixing, sb.remark,
                   sb.attr1, sb.attr2, sb.attr3, sb.attr4, sb.create_by, sb.create_time, sb.update_by, sb.update_time
            FROM storage_bin sb
            """;

    private final JdbcTemplate jdbc;

    public WmAreaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> query, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query);
        sql.append(" ORDER BY sb.bin_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapRow(rs), args.toArray());
    }

    public long count(Map<String, String> query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM storage_bin sb WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE sb.bin_id = ?", (rs, n) -> mapRow(rs), id);
        return rows.stream().findFirst();
    }

    public boolean existsCode(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM storage_bin WHERE bin_code = ?", Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_bin WHERE bin_code = ? AND bin_id <> ?", Long.class, code, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO storage_bin (bin_code, bin_name, zone_id, area, max_loa,
                      position_x, position_y, position_z, enable_flag, frozen_flag,
                      product_mixing, batch_mixing, remark, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, str(body, "areaCode"));
            ps.setString(2, str(body, "areaName"));
            ps.setLong(3, Long.parseLong(String.valueOf(body.get("locationId"))));
            setDouble(ps, 4, body.get("area"));
            setDouble(ps, 5, body.get("maxLoa"));
            setInt(ps, 6, body.get("positionX"));
            setInt(ps, 7, body.get("positionY"));
            setInt(ps, 8, body.get("positionZ"));
            ps.setString(9, strOr(body, "enableFlag", "Y"));
            ps.setString(10, strOr(body, "frozenFlag", "N"));
            ps.setString(11, strOr(body, "productMixing", "Y"));
            ps.setString(12, strOr(body, "batchMixing", "Y"));
            ps.setString(13, str(body, "remark"));
            ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Long id, Map<String, Object> body) {
        if (body.containsKey("frozenFlag") && body.size() <= 3) {
            return jdbc.update("UPDATE storage_bin SET frozen_flag = ?, update_time = ? WHERE bin_id = ?",
                    str(body, "frozenFlag"), Timestamp.valueOf(LocalDateTime.now()), id);
        }
        return jdbc.update("""
                UPDATE storage_bin SET bin_code=?, bin_name=?, zone_id=?, area=?, max_loa=?,
                  position_x=?, position_y=?, position_z=?, enable_flag=?, product_mixing=?, batch_mixing=?, remark=?, update_time=?
                WHERE bin_id=?
                """,
                str(body, "areaCode"),
                str(body, "areaName"),
                Long.parseLong(String.valueOf(body.get("locationId"))),
                doubleVal(body.get("area")),
                doubleVal(body.get("maxLoa")),
                intVal(body.get("positionX")),
                intVal(body.get("positionY")),
                intVal(body.get("positionZ")),
                strOr(body, "enableFlag", "Y"),
                strOr(body, "productMixing", "Y"),
                strOr(body, "batchMixing", "Y"),
                str(body, "remark"),
                Timestamp.valueOf(LocalDateTime.now()),
                id);
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM storage_bin WHERE bin_id = ?", id);
    }

    public Optional<String> resolveLocationName(Long areaId) {
        List<String> names = jdbc.query("""
                SELECT CONCAT(w.warehouse_name, ' / ', sz.zone_name) AS loc
                FROM storage_bin sb
                JOIN storage_zone sz ON sb.zone_id = sz.zone_id
                JOIN warehouse w ON sz.warehouse_id = w.warehouse_id
                WHERE sb.bin_id = ?
                """, (rs, n) -> rs.getString("loc"), areaId);
        return names.stream().findFirst();
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> query) {
        if (StringUtils.hasText(query.get("locationId"))) {
            sql.append(" AND sb.zone_id = ?");
            args.add(Long.parseLong(query.get("locationId").trim()));
        }
        if (StringUtils.hasText(query.get("areaCode"))) {
            sql.append(" AND sb.bin_code LIKE ?");
            args.add("%" + query.get("areaCode").trim() + "%");
        }
        if (StringUtils.hasText(query.get("areaName"))) {
            sql.append(" AND sb.bin_name LIKE ?");
            args.add("%" + query.get("areaName").trim() + "%");
        }
    }

    static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("areaId", rs.getLong("bin_id"));
        m.put("areaCode", rs.getString("bin_code"));
        m.put("areaName", rs.getString("bin_name"));
        m.put("locationId", rs.getLong("zone_id"));
        m.put("area", rs.getObject("area"));
        m.put("maxLoa", rs.getObject("max_loa"));
        m.put("positionX", rs.getObject("position_x"));
        m.put("positionY", rs.getObject("position_y"));
        m.put("positionZ", rs.getObject("position_z"));
        m.put("enableFlag", rs.getString("enable_flag"));
        m.put("frozenFlag", rs.getString("frozen_flag"));
        m.put("productMixing", rs.getString("product_mixing"));
        m.put("batchMixing", rs.getString("batch_mixing"));
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

    private static Integer intVal(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        return Integer.valueOf(String.valueOf(v));
    }

    private static void setDouble(PreparedStatement ps, int idx, Object v) throws SQLException {
        Double d = doubleVal(v);
        if (d == null) {
            ps.setNull(idx, Types.DOUBLE);
        } else {
            ps.setDouble(idx, d);
        }
    }

    private static void setInt(PreparedStatement ps, int idx, Object v) throws SQLException {
        Integer i = intVal(v);
        if (i == null) {
            ps.setNull(idx, Types.INTEGER);
        } else {
            ps.setInt(idx, i);
        }
    }
}
