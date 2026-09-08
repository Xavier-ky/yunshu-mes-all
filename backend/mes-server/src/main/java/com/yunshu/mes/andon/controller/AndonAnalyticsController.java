package com.yunshu.mes.andon.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/andon/analytics")
public class AndonAnalyticsController {

    private final JdbcTemplate jdbc;

    public AndonAnalyticsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(HttpServletRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalRecords", queryLong("SELECT COUNT(*) FROM pro_andon_record"));
        m.put("activeRecords", queryLong("SELECT COUNT(*) FROM pro_andon_record WHERE status='ACTIVE'"));
        m.put("handledRecords", queryLong("SELECT COUNT(*) FROM pro_andon_record WHERE status='HANDLED'"));
        m.put("openEvents", queryLong("SELECT COUNT(*) FROM andon_event WHERE status='OPEN'"));
        List<Map<String, Object>> top = jdbc.query("""
                SELECT andon_reason AS reason, COUNT(*) AS cnt FROM pro_andon_record
                GROUP BY andon_reason ORDER BY cnt DESC LIMIT 10
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("reason", rs.getString("reason"));
            row.put("cnt", rs.getLong("cnt"));
            return row;
        });
        m.put("reasonTop", top);
        List<Map<String, Object>> levelTop = jdbc.query("""
                SELECT andon_level AS level, COUNT(*) AS cnt FROM pro_andon_record
                GROUP BY andon_level ORDER BY cnt DESC
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("level", rs.getString("level"));
            row.put("cnt", rs.getLong("cnt"));
            return row;
        });
        m.put("levelTop", levelTop);
        m.put("statusDistribution", List.of(
                Map.of("status", "ACTIVE", "cnt", queryLong("SELECT COUNT(*) FROM pro_andon_record WHERE status='ACTIVE'")),
                Map.of("status", "HANDLED", "cnt", queryLong("SELECT COUNT(*) FROM pro_andon_record WHERE status='HANDLED'"))
        ));
        return ApiResponse.success(m, req);
    }

    private long queryLong(String sql) {
        Long v = jdbc.queryForObject(sql, Long.class);
        return v == null ? 0 : v;
    }
}
