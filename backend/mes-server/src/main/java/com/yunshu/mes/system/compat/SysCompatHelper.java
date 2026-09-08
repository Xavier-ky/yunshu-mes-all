package com.yunshu.mes.system.compat;

import com.yunshu.mes.inventory.compat.PageUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 云枢系统管理 compat 通用工具。 */
public final class SysCompatHelper {

    private SysCompatHelper() {}

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

    public static int pageNum(Map<String, String> params) {
        return PageUtil.pageNum(params);
    }

    public static int pageSize(Map<String, String> params) {
        return PageUtil.pageSize(params);
    }

    public static int offset(int pageNum, int pageSize) {
        return PageUtil.offset(pageNum, pageSize);
    }

    /** Yunshu ENABLED/DISABLED -> API 状态码 0/1 */
    public static String toApiStatusCode(String yunshuStatus) {
        if (yunshuStatus == null) {
            return "0";
        }
        return "ENABLED".equalsIgnoreCase(yunshuStatus) ? "0" : "1";
    }

    /** API 状态码 0/1 -> Yunshu ENABLED/DISABLED */
    public static String fromApiStatusCode(Object apiStatus) {
        String s = str(apiStatus);
        if (s == null || "0".equals(s) || "ENABLED".equalsIgnoreCase(s)) {
            return "ENABLED";
        }
        return "DISABLED";
    }

    public static String delFlag(int isDeleted) {
        return isDeleted == 0 ? "0" : "2";
    }

    public static String delFlag(Boolean isDeleted) {
        return delFlag(Boolean.TRUE.equals(isDeleted) ? 1 : 0);
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

    public static void dateRange(StringBuilder sql, List<Object> args, String col,
                                 Map<String, String> params, String beginKey, String endKey) {
        String begin = params.get(beginKey);
        String end = params.get(endKey);
        if (StringUtils.hasText(begin)) {
            sql.append(" AND ").append(col).append(" >= ?");
            args.add(parseTs(begin));
        }
        if (StringUtils.hasText(end)) {
            sql.append(" AND ").append(col).append(" <= ?");
            args.add(parseTs(end + (end.length() == 10 ? " 23:59:59" : "")));
        }
    }

    public static String currentUsername() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "system";
        }
        HttpServletRequest req = attrs.getRequest();
        Object user = req.getAttribute("currentUser");
        return user == null ? "system" : String.valueOf(user);
    }

    public static Long currentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object uid = attrs.getRequest().getAttribute("currentUserId");
        if (uid instanceof Number n) {
            return n.longValue();
        }
        if (uid != null) {
            try {
                return Long.parseLong(String.valueOf(uid));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static List<Long> parseIds(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : csv.split(",")) {
            if (StringUtils.hasText(part)) {
                ids.add(Long.parseLong(part.trim()));
            }
        }
        return ids;
    }

    public static Long[] longArray(List<Long> ids) {
        return ids.toArray(Long[]::new);
    }

    public static Timestamp getTs(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return rs.wasNull() ? null : ts;
    }

    /** 构建 id/label/children 树选择结构。 */
    public static List<Map<String, Object>> buildTreeSelect(
            List<Map<String, Object>> flat, String idKey, String parentKey, String labelKey) {
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (Map<String, Object> row : flat) {
            Long id = longVal(row.get(idKey));
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", id);
            node.put("label", row.get(labelKey));
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(id, node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> row : flat) {
            Long id = longVal(row.get(idKey));
            Long parentId = longObj(row.get(parentKey));
            Map<String, Object> node = nodes.get(id);
            if (parentId == null || parentId == 0L || !nodes.containsKey(parentId)) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) nodes.get(parentId).get("children");
                children.add(node);
            }
        }
        pruneEmptyChildren(roots);
        return roots;
    }

    @SuppressWarnings("unchecked")
    private static void pruneEmptyChildren(List<Map<String, Object>> nodes) {
        for (Map<String, Object> node : nodes) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            if (children.isEmpty()) {
                node.remove("children");
            } else {
                pruneEmptyChildren(children);
            }
        }
    }

    public static String roleGroup(List<Map<String, Object>> roles) {
        return roles.stream()
                .map(r -> str(r.get("roleName")))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }

    public static String postGroup(List<Map<String, Object>> posts) {
        return posts.stream()
                .map(p -> str(p.get("postName")))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }
}
