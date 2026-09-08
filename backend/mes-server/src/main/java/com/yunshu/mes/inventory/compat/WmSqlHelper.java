package com.yunshu.mes.inventory.compat;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

public final class WmSqlHelper {

    private WmSqlHelper() {}

    public static List<Map<String, Object>> list(JdbcTemplate jdbc, String table, String pk,
            Map<String, String> camelToSnake, Map<String, String> filters, int offset, int limit) {
        return list(jdbc, table, pk, camelToSnake, filters, offset, limit, true);
    }

    public static List<Map<String, Object>> list(JdbcTemplate jdbc, String table, String pk,
            Map<String, String> camelToSnake, Map<String, String> filters, int offset, int limit, boolean desc) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, filters, camelToSnake);
        sql.append(" ORDER BY ").append(pk).append(desc ? " DESC" : " ASC").append(" LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> WmRowMapper.map(rs, camelToSnake), args.toArray());
    }

    public static long count(JdbcTemplate jdbc, String table, Map<String, String> camelToSnake, Map<String, String> filters) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(table).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, filters, camelToSnake);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public static Map<String, Object> getById(JdbcTemplate jdbc, String table, String pk, String pkCamel,
            Map<String, String> camelToSnake, Long id) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT * FROM " + table + " WHERE " + pk + " = ?",
                (rs, n) -> WmRowMapper.map(rs, camelToSnake), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static Long insert(JdbcTemplate jdbc, String table, Map<String, String> camelToSnake, Map<String, Object> body) {
        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        for (Map.Entry<String, String> e : camelToSnake.entrySet()) {
            if (body.containsKey(e.getKey()) && body.get(e.getKey()) != null && !String.valueOf(body.get(e.getKey())).isBlank()) {
                cols.add(e.getValue());
                vals.add(body.get(e.getKey()));
            }
        }
        cols.add("create_time");
        vals.add(Timestamp.valueOf(LocalDateTime.now()));
        String colStr = String.join(", ", cols);
        String ph = String.join(", ", cols.stream().map(c -> "?").toList());
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO " + table + " (" + colStr + ") VALUES (" + ph + ")",
                    Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < vals.size(); i++) {
                ps.setObject(i + 1, vals.get(i));
            }
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public static int update(JdbcTemplate jdbc, String table, String pk, Map<String, String> camelToSnake,
            Map<String, Object> body, Long id) {
        List<String> sets = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        for (Map.Entry<String, String> e : camelToSnake.entrySet()) {
            if (body.containsKey(e.getKey())) {
                sets.add(e.getValue() + " = ?");
                vals.add(body.get(e.getKey()));
            }
        }
        sets.add("update_time = ?");
        vals.add(Timestamp.valueOf(LocalDateTime.now()));
        vals.add(id);
        return jdbc.update("UPDATE " + table + " SET " + String.join(", ", sets) + " WHERE " + pk + " = ?", vals.toArray());
    }

    public static int delete(JdbcTemplate jdbc, String table, String pk, Long id) {
        return jdbc.update("DELETE FROM " + table + " WHERE " + pk + " = ?", id);
    }

    private static void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> filters,
            Map<String, String> camelToSnake) {
        for (Map.Entry<String, String> f : filters.entrySet()) {
            if (!StringUtils.hasText(f.getValue())) {
                continue;
            }
            String col = camelToSnake.get(f.getKey());
            if (col == null) {
                continue;
            }
            if (f.getKey().endsWith("Name") || f.getKey().endsWith("Code")) {
                sql.append(" AND ").append(col).append(" LIKE ?");
                args.add("%" + f.getValue().trim() + "%");
            } else {
                sql.append(" AND ").append(col).append(" = ?");
                args.add(f.getValue().trim());
            }
        }
    }
}
