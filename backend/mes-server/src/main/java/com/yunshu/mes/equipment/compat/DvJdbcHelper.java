package com.yunshu.mes.equipment.compat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class DvJdbcHelper {

    private DvJdbcHelper() {}

    public static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public static String strOr(Object v, String def) {
        String s = str(v);
        return (s == null || s.isBlank()) ? def : s;
    }

    public static long longVal(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    public static Long longObj(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        return longVal(v);
    }

    public static Integer intObj(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }

    public static Timestamp parseTs(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        String s = String.valueOf(v).trim().replace("T", " ");
        if (s.length() == 10) {
            s += " 00:00:00";
        }
        try {
            return Timestamp.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    public static void like(StringBuilder sql, List<Object> args, String col, String val) {
        if (StringUtils.hasText(val)) {
            sql.append(" AND ").append(col).append(" LIKE ?");
            args.add("%" + val.trim() + "%");
        }
    }

    public static void eq(StringBuilder sql, List<Object> args, String col, String val) {
        if (StringUtils.hasText(val)) {
            sql.append(" AND ").append(col).append(" = ?");
            args.add(val.trim());
        }
    }

    public static void eqLong(StringBuilder sql, List<Object> args, String col, Map<String, String> params, String key) {
        String val = params.get(key);
        if (StringUtils.hasText(val)) {
            sql.append(" AND ").append(col).append(" = ?");
            args.add(Long.parseLong(val.trim()));
        }
    }
}
