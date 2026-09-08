package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/access")
public class AccessSummaryController {

    private final JdbcTemplate jdbc;

    public AccessSummaryController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userTotal", count("SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0"));
        data.put("enabledUserCount", count("SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0 AND status = '0'"));
        data.put("disabledUserCount", count("SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0 AND status = '1'"));
        data.put("roleTotal", count("SELECT COUNT(*) FROM sys_role"));
        data.put("enabledRoleCount", count("SELECT COUNT(*) FROM sys_role WHERE status = '0'"));
        data.put("deptTotal", count("SELECT COUNT(*) FROM sys_department WHERE is_deleted = 0"));
        data.put("enabledDeptCount", count("SELECT COUNT(*) FROM sys_department WHERE is_deleted = 0 AND status = '0'"));
        data.put("postTotal", count("SELECT COUNT(*) FROM sys_post"));
        data.put("onlineCount", count("""
                SELECT COUNT(DISTINCT user_name) FROM sys_logininfor
                WHERE status = '0' AND login_time >= CURDATE()
                """));
        return MesApiResponse.ok(data);
    }

    @GetMapping("/org-stats")
    public Map<String, Object> orgStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deptUserCounts", loadDeptUserCounts());
        data.put("userTotal", count("SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0"));
        data.put("deptTotal", count("SELECT COUNT(*) FROM sys_department WHERE is_deleted = 0"));
        return MesApiResponse.ok(data);
    }

    @GetMapping("/role-stats")
    public Map<String, Object> roleStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roleUserCounts", loadRoleUserCounts());
        data.put("roleTotal", count("SELECT COUNT(*) FROM sys_role"));
        return MesApiResponse.ok(data);
    }

    private Map<String, Long> loadRoleUserCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT role_id, COUNT(*) AS cnt
                FROM sys_user_role
                GROUP BY role_id
                """);
        for (Map<String, Object> row : rows) {
            Object roleId = row.get("role_id");
            if (roleId == null) {
                continue;
            }
            Object cnt = row.get("cnt");
            long value = cnt instanceof Number n ? n.longValue() : 0L;
            counts.put(String.valueOf(roleId), value);
        }
        return counts;
    }

    private Map<String, Long> loadDeptUserCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT dept_id, COUNT(*) AS cnt
                FROM sys_user
                WHERE is_deleted = 0 AND dept_id IS NOT NULL
                GROUP BY dept_id
                """);
        for (Map<String, Object> row : rows) {
            Object deptId = row.get("dept_id");
            if (deptId == null) {
                continue;
            }
            Object cnt = row.get("cnt");
            long value = cnt instanceof Number n ? n.longValue() : 0L;
            counts.put(String.valueOf(deptId), value);
        }
        return counts;
    }

    private long count(String sql) {
        Long c = jdbc.queryForObject(sql, Long.class);
        return c == null ? 0 : c;
    }
}
