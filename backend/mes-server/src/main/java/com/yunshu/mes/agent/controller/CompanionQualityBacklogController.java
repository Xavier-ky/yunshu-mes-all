package com.yunshu.mes.agent.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A fixed, read-only live data contract for the companion quality-backlog preset. */
@RestController
@RequestMapping("/api/agent/companion")
public class CompanionQualityBacklogController {

    private static final String PENDING_STATUSES = "'PREPARE', 'APPROVING'";

    private static final String SUMMARY_SQL = """
            SELECT
              COUNT(*) AS pending_total,
              COALESCE(SUM(CASE WHEN status = 'PREPARE' THEN 1 ELSE 0 END), 0) AS created_count,
              COALESCE(SUM(CASE WHEN status = 'APPROVING' THEN 1 ELSE 0 END), 0) AS inspecting_count,
              0 AS in_progress_count,
              COALESCE(SUM(CASE WHEN create_time < DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN 1 ELSE 0 END), 0) AS aged_over_24h_count,
              COALESCE(SUM(CASE WHEN create_time < DATE_SUB(NOW(), INTERVAL 72 HOUR) THEN 1 ELSE 0 END), 0) AS aged_over_72h_count,
              COALESCE(SUM(CASE WHEN DATE(create_time) = CURDATE() THEN 1 ELSE 0 END), 0) AS created_today_count,
              MIN(create_time) AS oldest_pending_at
            FROM qc_ipqc
            WHERE status IN (%s)
            """.formatted(PENDING_STATUSES);

    private static final String OLDEST_TASKS_SQL = """
            SELECT q.ipqc_code, q.status, q.ipqc_type AS inspect_type, q.create_time,
                   COALESCE(wo.work_order_no, '') AS work_order_no,
                   COALESCE(p.product_name, q.item_name, '') AS product_name,
                   TIMESTAMPDIFF(HOUR, q.create_time, NOW()) AS pending_hours
            FROM qc_ipqc q
            LEFT JOIN work_order wo ON wo.work_order_id = q.workorder_id
            LEFT JOIN product p ON p.product_id = wo.product_id
            WHERE q.status IN (%s)
            ORDER BY q.create_time ASC, q.ipqc_id ASC
            LIMIT 5
            """.formatted(PENDING_STATUSES);

    private final JdbcTemplate jdbc;

    public CompanionQualityBacklogController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/quality-backlog")
    public ApiResponse<Map<String, Object>> qualityBacklog(HttpServletRequest request) {
        try {
            Map<String, Object> row = jdbc.queryForMap(SUMMARY_SQL);
            Map<String, Object> backlog = new LinkedHashMap<>();
            backlog.put("pendingTotal", number(row, "pending_total"));
            backlog.put("createdCount", number(row, "created_count"));
            backlog.put("inspectingCount", number(row, "inspecting_count"));
            backlog.put("inProgressCount", number(row, "in_progress_count"));
            backlog.put("agedOver24HoursCount", number(row, "aged_over_24h_count"));
            backlog.put("agedOver72HoursCount", number(row, "aged_over_72h_count"));
            backlog.put("createdTodayCount", number(row, "created_today_count"));
            backlog.put("oldestPendingAt", dateTime(row.get("oldest_pending_at")));
            backlog.put("agingDefinition", "A pending IPQC is counted as aged after 24 hours.");

            List<Map<String, Object>> oldestPendingTasks = jdbc.query(OLDEST_TASKS_SQL, (rs, index) -> {
                Map<String, Object> task = new LinkedHashMap<>();
                task.put("qualityTaskNo", rs.getString("ipqc_code"));
                task.put("status", rs.getString("status"));
                task.put("inspectType", rs.getString("inspect_type"));
                task.put("workOrderNo", rs.getString("work_order_no"));
                task.put("productName", rs.getString("product_name"));
                task.put("pendingHours", rs.getLong("pending_hours"));
                task.put("createdAt", dateTime(rs.getTimestamp("create_time")));
                return task;
            });

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("dataSource", "LIVE");
            payload.put("businessDate", LocalDate.now().toString());
            payload.put("queriedAt", LocalDateTime.now().toString());
            payload.put("backlog", backlog);
            payload.put("oldestPendingTasks", oldestPendingTasks);
            return ApiResponse.success(payload, request);
        } catch (DataAccessException ex) {
            return ApiResponse.fail("LIVE_DATA_UNAVAILABLE", "Live quality backlog data is unavailable", request);
        }
    }

    private static long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String dateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        return value == null ? null : value.toString();
    }
}
