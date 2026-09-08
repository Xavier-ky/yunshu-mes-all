package com.yunshu.mes.andon.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/andon/board")
public class AndonBoardController {

    private static final String SQL_STATIONS =
            "SELECT w.station_id, w.station_code, w.station_name, w.station_type, "
                    + "l.line_id, l.line_code, l.line_name, "
                    + "CASE WHEN ar.record_id IS NOT NULL THEN 'ALERT' ELSE 'OK' END AS andon_status, "
                    + "ar.record_id AS active_record_id, ar.andon_reason, ar.elapsed_minutes "
                    + "FROM workstation w "
                    + "JOIN production_line l ON w.line_id = l.line_id "
                    + "LEFT JOIN ( "
                    + "  SELECT r.workstation_id, r.record_id, r.andon_reason, "
                    + "         TIMESTAMPDIFF(MINUTE, r.create_time, NOW()) AS elapsed_minutes "
                    + "  FROM pro_andon_record r "
                    + "  INNER JOIN ( "
                    + "    SELECT workstation_id, MIN(record_id) AS record_id "
                    + "    FROM pro_andon_record WHERE status = 'ACTIVE' GROUP BY workstation_id "
                    + "  ) active_latest ON r.record_id = active_latest.record_id "
                    + ") ar ON ar.workstation_id = w.station_id "
                    + "ORDER BY l.line_name, w.station_code "
                    + "LIMIT 120";

    private static final String SQL_LINES =
            "SELECT l.line_id, l.line_code, l.line_name, "
                    + "COUNT(w.station_id) AS station_count, "
                    + "SUM(CASE WHEN ar.cnt > 0 THEN 1 ELSE 0 END) AS alert_count, "
                    + "MAX(COALESCE(ar.elapsed, 0)) AS worst_elapsed "
                    + "FROM production_line l "
                    + "JOIN workstation w ON w.line_id = l.line_id "
                    + "LEFT JOIN ( "
                    + "  SELECT workstation_id, COUNT(*) AS cnt, "
                    + "         MAX(TIMESTAMPDIFF(MINUTE, create_time, NOW())) AS elapsed "
                    + "  FROM pro_andon_record WHERE status = 'ACTIVE' GROUP BY workstation_id "
                    + ") ar ON ar.workstation_id = w.station_id "
                    + "GROUP BY l.line_id, l.line_name "
                    + "ORDER BY alert_count DESC, l.line_name "
                    + "LIMIT 40";

    private final JdbcTemplate jdbc;

    public AndonBoardController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(HttpServletRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activeRecords", count("SELECT COUNT(*) FROM pro_andon_record WHERE status='ACTIVE'"));
        m.put("openEvents", count("SELECT COUNT(*) FROM andon_event WHERE status='OPEN'"));
        m.put("shiftClosed", count(
                "SELECT COUNT(*) FROM pro_andon_record WHERE status='HANDLED' AND handle_time >= CURDATE()"));
        m.put("slaOverdue", count(
                "SELECT COUNT(*) FROM pro_andon_record WHERE status='ACTIVE' "
                        + "AND create_time < DATE_SUB(NOW(), INTERVAL 30 MINUTE)"));
        Double avg = jdbc.queryForObject(
                "SELECT AVG(TIMESTAMPDIFF(MINUTE, create_time, handle_time)) FROM pro_andon_record "
                        + "WHERE status='HANDLED' AND handle_time IS NOT NULL AND handle_time >= CURDATE()",
                Double.class);
        m.put("avgResponseMinutes", avg == null ? 0 : Math.round(avg * 10) / 10.0);
        m.put("totalRecords", count("SELECT COUNT(*) FROM pro_andon_record"));
        m.put("handledRecords", count("SELECT COUNT(*) FROM pro_andon_record WHERE status='HANDLED'"));
        Long userId = currentUserId(req);
        if (userId != null) {
            m.put("mineCount", count(
                    "SELECT COUNT(*) FROM pro_andon_record WHERE status='ACTIVE' AND handler_user_id = ?",
                    userId));
        } else {
            m.put("mineCount", 0L);
        }
        return ApiResponse.success(m, req);
    }

    @GetMapping("/active")
    public ApiResponse<List<Map<String, Object>>> active(HttpServletRequest req) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT record_id, workstation_code, workstation_name, workorder_code, process_name, "
                        + "nick_name, andon_reason, andon_level, create_time, "
                        + "TIMESTAMPDIFF(MINUTE, create_time, NOW()) AS elapsed_minutes "
                        + "FROM pro_andon_record WHERE status = 'ACTIVE' ORDER BY create_time ASC LIMIT 50",
                (rs, n) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("recordId", rs.getLong("record_id"));
                    row.put("workstationCode", rs.getString("workstation_code"));
                    row.put("workstationName", rs.getString("workstation_name"));
                    row.put("workorderCode", rs.getString("workorder_code"));
                    row.put("processName", rs.getString("process_name"));
                    row.put("nickName", rs.getString("nick_name"));
                    row.put("andonReason", rs.getString("andon_reason"));
                    row.put("andonLevel", rs.getString("andon_level"));
                    row.put("createTime", rs.getString("create_time"));
                    row.put("elapsedMinutes", rs.getLong("elapsed_minutes"));
                    return row;
                });
        return ApiResponse.success(rows, req);
    }

    @GetMapping("/stations")
    public ApiResponse<List<Map<String, Object>>> stations(HttpServletRequest req) {
        try {
            List<Map<String, Object>> rows = jdbc.query(SQL_STATIONS, (rs, n) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("stationId", rs.getLong("station_id"));
                row.put("stationCode", rs.getString("station_code"));
                row.put("stationName", rs.getString("station_name"));
                row.put("stationType", rs.getString("station_type"));
                row.put("lineId", rs.getLong("line_id"));
                row.put("lineCode", rs.getString("line_code"));
                row.put("lineName", rs.getString("line_name"));
                row.put("andonStatus", rs.getString("andon_status"));
                Object rid = rs.getObject("active_record_id");
                row.put("activeRecordId", rid == null ? null : rs.getLong("active_record_id"));
                row.put("andonReason", rs.getString("andon_reason"));
                Object elapsed = rs.getObject("elapsed_minutes");
                row.put("elapsedMinutes", elapsed == null ? null : rs.getLong("elapsed_minutes"));
                return row;
            });
            return ApiResponse.success(rows, req);
        } catch (Exception e) {
            return ApiResponse.success(List.of(), req);
        }
    }

    @GetMapping("/lines")
    public ApiResponse<List<Map<String, Object>>> lines(HttpServletRequest req) {
        try {
            List<Map<String, Object>> rows = jdbc.query(SQL_LINES, (rs, n) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("lineId", rs.getLong("line_id"));
                row.put("lineCode", rs.getString("line_code"));
                row.put("lineName", rs.getString("line_name"));
                row.put("stationCount", rs.getLong("station_count"));
                row.put("alertCount", rs.getLong("alert_count"));
                row.put("worstElapsed", rs.getLong("worst_elapsed"));
                return row;
            });
            return ApiResponse.success(rows, req);
        } catch (Exception e) {
            return ApiResponse.success(List.of(), req);
        }
    }

    @GetMapping("/trend")
    public ApiResponse<Map<String, Object>> trend(HttpServletRequest req) {
        Map<Integer, Long> created = new LinkedHashMap<>();
        Map<Integer, Long> closed = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            created.put(h, 0L);
            closed.put(h, 0L);
        }
        jdbc.query(
                "SELECT HOUR(create_time) AS hr, COUNT(*) AS cnt FROM pro_andon_record "
                        + "WHERE create_time >= CURDATE() GROUP BY HOUR(create_time)",
                (RowCallbackHandler) rs -> created.put(rs.getInt("hr"), rs.getLong("cnt")));
        jdbc.query(
                "SELECT HOUR(handle_time) AS hr, COUNT(*) AS cnt FROM pro_andon_record "
                        + "WHERE status = 'HANDLED' AND handle_time >= CURDATE() GROUP BY HOUR(handle_time)",
                (RowCallbackHandler) rs -> closed.put(rs.getInt("hr"), rs.getLong("cnt")));
        List<Integer> hours = new ArrayList<>();
        List<Long> createdList = new ArrayList<>();
        List<Long> closedList = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hours.add(h);
            createdList.add(created.get(h));
            closedList.add(closed.get(h));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hours", hours);
        m.put("created", createdList);
        m.put("closed", closedList);
        return ApiResponse.success(m, req);
    }

    @GetMapping("/timeline")
    public ApiResponse<List<Map<String, Object>>> timeline(
            @RequestParam(defaultValue = "20") int limit, HttpServletRequest req) {
        int cap = Math.min(limit, 30);
        List<Map<String, Object>> items = new ArrayList<>();
        items.addAll(jdbc.query(
                "SELECT record_id, workstation_name, andon_reason, nick_name AS actor, create_time AS event_time, "
                        + "'CREATED' AS event_type, status FROM pro_andon_record "
                        + "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR) "
                        + "ORDER BY create_time DESC LIMIT ?",
                (rs, n) -> mapTimelineRow(rs), cap));
        items.addAll(jdbc.query(
                "SELECT record_id, workstation_name, andon_reason, "
                        + "COALESCE(handler_nick_name, nick_name) AS actor, handle_time AS event_time, "
                        + "'HANDLED' AS event_type, status FROM pro_andon_record "
                        + "WHERE status = 'HANDLED' AND handle_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR) "
                        + "ORDER BY handle_time DESC LIMIT ?",
                (rs, n) -> mapTimelineRow(rs), cap));
        items.addAll(jdbc.query(
                "SELECT record_id, workstation_name, andon_reason, nick_name AS actor, create_time AS event_time, "
                        + "'SLA_OVERDUE' AS event_type, status FROM pro_andon_record "
                        + "WHERE status = 'ACTIVE' AND create_time < DATE_SUB(NOW(), INTERVAL 30 MINUTE) "
                        + "ORDER BY create_time ASC LIMIT ?",
                (rs, n) -> mapTimelineRow(rs), Math.min(limit, 10)));
        items.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("eventTime"))).reversed());
        if (items.size() > limit) {
            items = items.subList(0, limit);
        }
        return ApiResponse.success(items, req);
    }

    private Map<String, Object> mapTimelineRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("recordId", rs.getLong("record_id"));
        row.put("workstationName", rs.getString("workstation_name"));
        row.put("andonReason", rs.getString("andon_reason"));
        row.put("actor", rs.getString("actor"));
        row.put("eventTime", rs.getString("event_time"));
        row.put("eventType", rs.getString("event_type"));
        row.put("status", rs.getString("status"));
        return row;
    }

    private Long currentUserId(HttpServletRequest req) {
        Object v = req.getAttribute("currentUserId");
        if (v == null) {
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

    private long count(String sql, Object... args) {
        Long c = jdbc.queryForObject(sql, Long.class, args);
        return c == null ? 0 : c;
    }
}
