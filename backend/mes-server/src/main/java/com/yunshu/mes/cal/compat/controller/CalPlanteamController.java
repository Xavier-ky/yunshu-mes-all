package com.yunshu.mes.cal.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/cal/planteam")
public class CalPlanteamController {

    private final JdbcTemplate jdbc;

    public CalPlanteamController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        if (!tableExists("cal_plan_team")) {
            return MesApiResponse.table(List.of(), 0);
        }
        String planId = params.get("planId");
        List<Object> args = new ArrayList<>();
        String sql = """
                SELECT pt.record_id, pt.plan_id, pt.team_id, pt.team_code, pt.team_name, pt.remark,
                       t.calendar_type
                FROM cal_plan_team pt
                LEFT JOIN cal_team t ON t.team_id = pt.team_id
                """;
        if (planId != null && !planId.isBlank()) {
            sql += " WHERE pt.plan_id = ?";
            args.add(Long.parseLong(planId));
        }
        sql += " ORDER BY pt.record_id";
        List<Map<String, Object>> rows = jdbc.query(sql, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordId", rs.getLong("record_id"));
            m.put("planId", rs.getLong("plan_id"));
            m.put("teamId", rs.getLong("team_id"));
            m.put("teamCode", rs.getString("team_code"));
            m.put("teamName", rs.getString("team_name"));
            m.put("calendarType", rs.getString("calendar_type"));
            m.put("remark", rs.getString("remark"));
            return m;
        }, args.toArray());
        return MesApiResponse.table(rows, rows.size());
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_plan_team")) {
            return MesApiResponse.ok(1L);
        }
        long teamId = longVal(body, "teamId");
        String teamCode = str(body, "teamCode");
        String teamName = str(body, "teamName");
        if (teamCode == null || teamName == null) {
            List<Map<String, Object>> team = jdbc.query("""
                    SELECT team_code, team_name FROM cal_team WHERE team_id = ?
                    """, (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("teamCode", rs.getString("team_code"));
                m.put("teamName", rs.getString("team_name"));
                return m;
            }, teamId);
            if (!team.isEmpty()) {
                teamCode = String.valueOf(team.get(0).get("teamCode"));
                teamName = String.valueOf(team.get(0).get("teamName"));
            }
        }
        jdbc.update("""
                INSERT INTO cal_plan_team (plan_id, team_id, team_code, team_name, remark, create_time)
                VALUES (?, ?, ?, ?, ?, NOW(3))
                """, longVal(body, "planId"), teamId, teamCode, teamName, str(body, "remark", ""));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return MesApiResponse.ok(id);
    }

    @DeleteMapping("/{recordId}")
    public Map<String, Object> remove(@PathVariable Long recordId) {
        if (!tableExists("cal_plan_team")) {
            return MesApiResponse.toAjax(1);
        }
        int n = jdbc.update("DELETE FROM cal_plan_team WHERE record_id = ?", recordId);
        return MesApiResponse.toAjax(n);
    }

    private boolean tableExists(String table) {
        try {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = ?
                    """, Integer.class, table);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private static String str(Map<String, Object> m, String key) {
        return str(m, key, null);
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private static long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }
}
