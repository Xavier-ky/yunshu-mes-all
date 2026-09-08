package com.yunshu.mes.cal.compat.controller;

import com.yunshu.mes.cal.compat.service.CalTeamshiftService;
import com.yunshu.mes.cal.compat.service.CalPlanMutationService;
import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.sql.Timestamp;
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
@RequestMapping("/api/mes/cal/calplan")
public class CalPlanController {

    private static final String INITIAL_STATUS = "PREPARE";

    private final JdbcTemplate jdbc;
    private final CalTeamshiftService teamshiftService;
    private final CalPlanMutationService mutationService;

    public CalPlanController(JdbcTemplate jdbc, CalTeamshiftService teamshiftService,
            CalPlanMutationService mutationService) {
        this.jdbc = jdbc;
        this.teamshiftService = teamshiftService;
        this.mutationService = mutationService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        if (!tableExists("cal_plan")) {
            return MesApiResponse.table(defaultPlans(), 1);
        }
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT plan_id, plan_code, plan_name, calendar_type, start_date, end_date,
                       shift_type, shift_method, shift_count, status, remark, enable_flag, create_time
                FROM cal_plan ORDER BY plan_id LIMIT ? OFFSET ?
                """, (rs, n) -> mapPlan(rs), ps, PageUtil.offset(pn, ps));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM cal_plan", Long.class);
        return MesApiResponse.table(rows, total == null ? 0 : total);
    }

    @GetMapping("/{planId}")
    public Map<String, Object> getInfo(@PathVariable Long planId) {
        if (!tableExists("cal_plan")) {
            return MesApiResponse.ok(defaultPlans().get(0));
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT plan_id, plan_code, plan_name, calendar_type, start_date, end_date,
                       shift_type, shift_method, shift_count, status, remark, enable_flag, create_time
                FROM cal_plan WHERE plan_id = ?
                """, (rs, n) -> mapPlan(rs), planId);
        return rows.isEmpty() ? MesApiResponse.error("排班计划不存在") : MesApiResponse.ok(rows.get(0));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_plan")) {
            return creationResponse(1L);
        }
        String shiftType = str(body, "shiftType", "SHIFT");
        jdbc.update("""
                INSERT INTO cal_plan (plan_code, plan_name, calendar_type, start_date, end_date,
                  shift_type, shift_method, shift_count, status, remark, enable_flag, create_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(3))
                """,
                str(body, "planCode"), str(body, "planName"), str(body, "calendarType"),
                body.get("startDate"), body.get("endDate"), shiftType,
                str(body, "shiftMethod"), intVal(body, "shiftCount", 1),
                INITIAL_STATUS, str(body, "remark", ""), str(body, "enableFlag", "Y"));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        teamshiftService.addDefaultShifts(id, shiftType);
        return creationResponse(id);
    }

    private Map<String, Object> creationResponse(Long id) {
        Map<String, Object> response = MesApiResponse.ok(id);
        response.put("status", INITIAL_STATUS);
        response.put("msg", "排班计划已按草稿状态创建；请通过修改接口完成确认");
        return response;
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_plan")) {
            return MesApiResponse.toAjax(1);
        }
        CalPlanMutationService.Result result = mutationService.edit(body);
        if (result.error() != null) {
            return MesApiResponse.error(result.error());
        }
        return MesApiResponse.toAjax(result.rows());
    }

    @DeleteMapping("/{planIds}")
    public Map<String, Object> remove(@PathVariable String planIds) {
        if (!tableExists("cal_plan")) {
            return MesApiResponse.toAjax(1);
        }
        int n = 0;
        for (String p : planIds.split(",")) {
            n += jdbc.update("DELETE FROM cal_plan WHERE plan_id=?", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }

    private Map<String, Object> mapPlan(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("planId", rs.getLong("plan_id"));
        m.put("planCode", rs.getString("plan_code"));
        m.put("planName", rs.getString("plan_name"));
        m.put("calendarType", rs.getString("calendar_type"));
        m.put("startDate", rs.getDate("start_date"));
        m.put("endDate", rs.getDate("end_date"));
        m.put("shiftType", rs.getString("shift_type"));
        m.put("shiftMethod", rs.getString("shift_method"));
        m.put("shiftCount", rs.getObject("shift_count"));
        m.put("status", rs.getString("status"));
        m.put("remark", rs.getString("remark"));
        m.put("enableFlag", rs.getString("enable_flag"));
        m.put("createTime", rs.getTimestamp("create_time"));
        return m;
    }

    private List<Map<String, Object>> defaultPlans() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("planId", 1L);
        p.put("planCode", "PLAN-DEFAULT");
        p.put("planName", "默认排班计划");
        p.put("shiftType", "SHIFT");
        p.put("enableFlag", "Y");
        p.put("createTime", new Timestamp(System.currentTimeMillis()));
        rows.add(p);
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

    private static int intVal(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
