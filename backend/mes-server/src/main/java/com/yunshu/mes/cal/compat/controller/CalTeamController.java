package com.yunshu.mes.cal.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/cal/team")
public class CalTeamController {

    private final JdbcTemplate jdbc;

    public CalTeamController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(fetchTeams(params), countTeams(params));
    }

    @GetMapping("/listAll")
    public Map<String, Object> listAll() {
        return MesApiResponse.ok(fetchTeams(Map.of()));
    }

    @GetMapping("/{teamId}")
    public Map<String, Object> getInfo(@PathVariable Long teamId) {
        List<Map<String, Object>> rows = fetchTeams(Map.of("teamId", String.valueOf(teamId)));
        return rows.isEmpty() ? MesApiResponse.error("班组不存在") : MesApiResponse.ok(rows.get(0));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_team")) {
            return MesApiResponse.ok(1L);
        }
        jdbc.update("""
                INSERT INTO cal_team (team_code, team_name, calendar_type, remark, enable_flag, create_time)
                VALUES (?, ?, ?, ?, ?, NOW(3))
                """, str(body, "teamCode"), str(body, "teamName"), str(body, "calendarType"),
                str(body, "remark", ""), str(body, "enableFlag", "Y"));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_team")) {
            return MesApiResponse.toAjax(1);
        }
        int n = jdbc.update("""
                UPDATE cal_team SET team_code=?, team_name=?, calendar_type=?, remark=?, enable_flag=?, update_time=NOW(3)
                WHERE team_id=?
                """, str(body, "teamCode"), str(body, "teamName"), str(body, "calendarType"),
                str(body, "remark", ""), str(body, "enableFlag", "Y"), longVal(body, "teamId"));
        return MesApiResponse.toAjax(n);
    }

    @DeleteMapping("/{teamIds}")
    public Map<String, Object> remove(@PathVariable String teamIds) {
        if (!tableExists("cal_team")) {
            return MesApiResponse.toAjax(1);
        }
        int n = 0;
        for (String p : teamIds.split(",")) {
            long tid = Long.parseLong(p.trim());
            if (tableExists("cal_team_member")) {
                jdbc.update("DELETE FROM cal_team_member WHERE team_id=?", tid);
            }
            if (tableExists("cal_plan_team")) {
                jdbc.update("DELETE FROM cal_plan_team WHERE team_id=?", tid);
            }
            n += jdbc.update("DELETE FROM cal_team WHERE team_id=?", tid);
        }
        return MesApiResponse.toAjax(n);
    }

    private List<Map<String, Object>> fetchTeams(Map<String, String> params) {
        if (!tableExists("cal_team")) {
            return defaultTeams();
        }
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        String teamId = params.get("teamId");
        if (teamId != null && !teamId.isBlank()) {
            return jdbc.query("""
                    SELECT team_id, team_code, team_name, calendar_type, remark, enable_flag
                    FROM cal_team WHERE team_id = ?
                    """, (rs, n) -> mapTeam(rs), Long.parseLong(teamId));
        }
        return jdbc.query("""
                SELECT team_id, team_code, team_name, calendar_type, remark, enable_flag
                FROM cal_team ORDER BY team_id LIMIT ? OFFSET ?
                """, (rs, n) -> mapTeam(rs), ps, PageUtil.offset(pn, ps));
    }

    private long countTeams(Map<String, String> params) {
        if (!tableExists("cal_team")) {
            return defaultTeams().size();
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM cal_team", Long.class);
        return total == null ? 0 : total;
    }

    private Map<String, Object> mapTeam(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("teamId", rs.getLong("team_id"));
        m.put("teamCode", rs.getString("team_code"));
        m.put("teamName", rs.getString("team_name"));
        m.put("calendarType", rs.getString("calendar_type"));
        m.put("remark", rs.getString("remark"));
        m.put("enableFlag", rs.getString("enable_flag"));
        return m;
    }

    private List<Map<String, Object>> defaultTeams() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("teamId", 1L);
        a.put("teamCode", "TEAM-A");
        a.put("teamName", "A班组");
        a.put("enableFlag", "Y");
        rows.add(a);
        return rows;
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
