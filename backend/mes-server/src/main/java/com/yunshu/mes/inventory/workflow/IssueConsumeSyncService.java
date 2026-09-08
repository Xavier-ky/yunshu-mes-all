package com.yunshu.mes.inventory.workflow;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

/**
 * 领料 execute 后同步 wm_item_consume（attr1=issue_id 幂等）。
 */
@Service
public class IssueConsumeSyncService {

    private static final Logger log = LoggerFactory.getLogger(IssueConsumeSyncService.class);

    private final JdbcTemplate jdbc;

    public IssueConsumeSyncService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void syncFromIssue(Long issueId, Map<String, Object> header) {
        try {
            if (issueId == null || header == null) {
                return;
            }
            if (existsForIssue(issueId)) {
                return;
            }
            Long workOrderId = longOrNull(header.get("workorderId"));
            String workOrderCode = header.get("workorderCode") != null
                    ? String.valueOf(header.get("workorderCode")) : null;
            String issueCode = header.get("issueCode") != null
                    ? String.valueOf(header.get("issueCode")) : null;

            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                var ps = con.prepareStatement("""
                        INSERT INTO wm_item_consume
                        (workorder_id, workorder_code, consume_date, status, remark, attr1, create_time)
                        VALUES (?,?,?,?,?,?,?)
                        """, new String[] { "record_id" });
                int i = 1;
                ps.setObject(i++, workOrderId);
                ps.setString(i++, workOrderCode);
                ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(i++, "FINISHED");
                ps.setString(i++, "synced from wm_issue:" + issueId);
                ps.setString(i++, String.valueOf(issueId));
                ps.setTimestamp(i, Timestamp.valueOf(LocalDateTime.now()));
                return ps;
            }, kh);
            Number key = kh.getKey();
            if (key == null) {
                return;
            }
            long consumeId = key.longValue();

            List<Map<String, Object>> lines = jdbc.queryForList("""
                    SELECT line_id, item_id, item_code, item_name, specification, unit_name, quantity_issued
                    FROM wm_issue_line WHERE issue_id = ?
                    """, issueId);
            for (Map<String, Object> line : lines) {
                Long lineId = longOrNull(line.get("line_id"));
                double qty = toDouble(line.get("quantity_issued"));
                KeyHolder lineKh = new GeneratedKeyHolder();
                jdbc.update(con -> {
                    var ps = con.prepareStatement("""
                            INSERT INTO wm_item_consume_line
                            (record_id, item_id, item_code, item_name, specification, unit_name,
                             quantity_consume, create_time)
                            VALUES (?,?,?,?,?,?,?,?)
                            """, new String[] { "line_id" });
                    int j = 1;
                    ps.setLong(j++, consumeId);
                    ps.setLong(j++, longOrNull(line.get("item_id")));
                    ps.setString(j++, str(line.get("item_code")));
                    ps.setString(j++, str(line.get("item_name")));
                    ps.setString(j++, str(line.get("specification")));
                    ps.setString(j++, str(line.get("unit_name")));
                    ps.setDouble(j++, qty);
                    ps.setTimestamp(j, Timestamp.valueOf(LocalDateTime.now()));
                    return ps;
                }, lineKh);
                Number consumeLineId = lineKh.getKey();
                if (consumeLineId == null || lineId == null) {
                    continue;
                }
                List<Map<String, Object>> details = jdbc.queryForList("""
                        SELECT material_stock_id, item_id, item_code, item_name, specification,
                               unit_name, quantity, batch_id, batch_code
                        FROM wm_issue_detail WHERE line_id = ?
                        """, lineId);
                for (Map<String, Object> d : details) {
                    jdbc.update("""
                            INSERT INTO wm_item_consume_detail
                            (line_id, record_id, material_stock_id, item_id, item_code, item_name,
                             specification, unit_name, quantity, batch_id, batch_code, create_time)
                            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                            """,
                            consumeLineId.longValue(), consumeId,
                            longOrNull(d.get("material_stock_id")),
                            longOrNull(d.get("item_id")),
                            str(d.get("item_code")), str(d.get("item_name")),
                            str(d.get("specification")), str(d.get("unit_name")),
                            toDouble(d.get("quantity")),
                            longOrNull(d.get("batch_id")), str(d.get("batch_code")),
                            Timestamp.valueOf(LocalDateTime.now()));
                }
            }
            log.info("wm_item_consume synced for issue {} ({})", issueId, issueCode);
        } catch (Exception e) {
            log.warn("wm_item_consume sync failed for issue {}: {}", issueId, e.getMessage());
        }
    }

    private boolean existsForIssue(Long issueId) {
        Long cnt = jdbc.queryForObject("""
                SELECT COUNT(*) FROM wm_item_consume WHERE attr1 = ? AND status = 'FINISHED'
                """, Long.class, String.valueOf(issueId));
        return cnt != null && cnt > 0;
    }

    private static Long longOrNull(Object v) {
        if (v == null || "".equals(String.valueOf(v))) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double toDouble(Object v) {
        if (v == null) {
            return 0;
        }
        return Double.parseDouble(String.valueOf(v));
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
