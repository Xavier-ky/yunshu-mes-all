package com.yunshu.mes.inventory.compat;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WmRowMapper {

    private WmRowMapper() {}

    public static Map<String, Object> map(ResultSet rs, Map<String, String> camelToSnake) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        ResultSetMetaData md = rs.getMetaData();
        Map<String, String> snakeToCamel = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : camelToSnake.entrySet()) {
            snakeToCamel.put(e.getValue(), e.getKey());
        }
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String col = md.getColumnLabel(i);
            String camel = snakeToCamel.getOrDefault(col, snakeToCamel.getOrDefault(col.toLowerCase(), toCamel(col)));
            row.put(camel, rs.getObject(i));
        }
        return row;
    }

    private static String toCamel(String snake) {
        if (snake == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                up = true;
            } else if (up) {
                sb.append(Character.toUpperCase(c));
                up = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
