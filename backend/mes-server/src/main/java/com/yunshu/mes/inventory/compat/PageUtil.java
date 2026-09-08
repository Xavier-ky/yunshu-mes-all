package com.yunshu.mes.inventory.compat;

import java.util.Map;

public final class PageUtil {

    private PageUtil() {}

    public static int pageNum(Map<String, String> params) {
        return intVal(params.get("pageNum"), 1);
    }

    public static int pageSize(Map<String, String> params) {
        return intVal(params.get("pageSize"), 10);
    }

    public static int offset(int pageNum, int pageSize) {
        return Math.max(0, (pageNum - 1) * pageSize);
    }

    public static String limitSql(int pageNum, int pageSize) {
        return " LIMIT " + pageSize + " OFFSET " + offset(pageNum, pageSize);
    }

    private static int intVal(String raw, int defaultVal) {
        if (raw == null || raw.isBlank()) {
            return defaultVal;
        }
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
