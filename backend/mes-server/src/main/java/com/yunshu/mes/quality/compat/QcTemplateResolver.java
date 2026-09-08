package com.yunshu.mes.quality.compat;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 检验单保存时解析 qc_template.template_id（表字段 NOT NULL）。
 */
public final class QcTemplateResolver {

    private QcTemplateResolver() {
    }

    public static Long resolveTemplateId(JdbcTemplate jdbc, Object itemId, Object itemCode) {
        Long byItem = findByItemId(jdbc, itemId);
        if (byItem != null) {
            return byItem;
        }
        Long byCode = findByItemCode(jdbc, itemCode);
        if (byCode != null) {
            return byCode;
        }
        Long fallback = findDefaultTemplate(jdbc);
        return fallback != null ? fallback : 1L;
    }

    public static void ensureTemplateId(JdbcTemplate jdbc, java.util.Map<String, Object> body) {
        Object existing = body.get("templateId");
        if (existing != null && !"".equals(String.valueOf(existing).trim()) && !"null".equalsIgnoreCase(String.valueOf(existing).trim())) {
            return;
        }
        Long ipqcId = longOrNull(body.get("ipqcId"));
        if (ipqcId != null) {
            List<Long> fromRow = jdbc.query(
                    "SELECT template_id FROM qc_ipqc WHERE ipqc_id = ? LIMIT 1",
                    (rs, n) -> rs.getLong("template_id"),
                    ipqcId);
            if (!fromRow.isEmpty() && fromRow.get(0) != null && fromRow.get(0) > 0) {
                body.put("templateId", fromRow.get(0));
                return;
            }
        }
        body.put("templateId", resolveTemplateId(jdbc, body.get("itemId"), body.get("itemCode")));
    }

    private static Long longOrNull(Object v) {
        if (v == null || "".equals(String.valueOf(v).trim())) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long findByItemId(JdbcTemplate jdbc, Object itemId) {
        if (itemId == null || "".equals(String.valueOf(itemId).trim())) {
            return null;
        }
        List<Long> ids = jdbc.query(
                "SELECT template_id FROM qc_template_product WHERE item_id = ? ORDER BY record_id LIMIT 1",
                (rs, n) -> rs.getLong("template_id"),
                Long.parseLong(String.valueOf(itemId)));
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static Long findByItemCode(JdbcTemplate jdbc, Object itemCode) {
        if (itemCode == null || "".equals(String.valueOf(itemCode).trim())) {
            return null;
        }
        List<Long> ids = jdbc.query(
                "SELECT template_id FROM qc_template_product WHERE item_code = ? ORDER BY record_id LIMIT 1",
                (rs, n) -> rs.getLong("template_id"),
                String.valueOf(itemCode).trim());
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static Long findDefaultTemplate(JdbcTemplate jdbc) {
        List<Long> ids = jdbc.query("""
                SELECT template_id FROM qc_template
                WHERE enable_flag = 'Y'
                  AND (qc_types IS NULL OR qc_types = '' OR qc_types LIKE '%PQC%')
                ORDER BY template_id
                LIMIT 1
                """, (rs, n) -> rs.getLong("template_id"));
        return ids.isEmpty() ? null : ids.get(0);
    }
}
