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
@RequestMapping("/api/mes/cal/shift")
public class CalShiftController {

    private final JdbcTemplate jdbc;

    public CalShiftController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        if (!tableExists("cal_shift")) {
            return MesApiResponse.table(List.of(), 0);
        }
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        String planId = params.get("planId");
        String sql = """
                SELECT shift_id, plan_id, order_num, shift_name, start_time, end_time, remark, enable_flag
                FROM cal_shift
                """;
        String countSql = "SELECT COUNT(*) FROM cal_shift";
        List<Object> args = new ArrayList<>();
        if (planId != null && !planId.isBlank()) {
            sql += " WHERE plan_id = ?";
            countSql += " WHERE plan_id = ?";
            args.add(Long.parseLong(planId));
        }
        sql += " ORDER BY order_num, shift_id LIMIT ? OFFSET ?";
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(ps);
        queryArgs.add(PageUtil.offset(pn, ps));
        List<Map<String, Object>> rows = jdbc.query(sql, (rs, n) -> mapShift(rs), queryArgs.toArray());
        Long total = args.isEmpty()
                ? jdbc.queryForObject(countSql, Long.class)
                : jdbc.queryForObject(countSql, Long.class, args.toArray());
        return MesApiResponse.table(rows, total == null ? 0 : total);
    }

    @GetMapping("/{shiftId}")
    public Map<String, Object> getInfo(@PathVariable Long shiftId) {
        if (!tableExists("cal_shift")) {
            return MesApiResponse.error("班次不存在");
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT shift_id, plan_id, order_num, shift_name, start_time, end_time, remark, enable_flag
                FROM cal_shift WHERE shift_id = ?
                """, (rs, n) -> mapShift(rs), shiftId);
        return rows.isEmpty() ? MesApiResponse.error("班次不存在") : MesApiResponse.ok(rows.get(0));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_shift")) {
            return MesApiResponse.ok(1L);
        }
        jdbc.update("""
                INSERT INTO cal_shift (plan_id, order_num, shift_name, start_time, end_time, remark, enable_flag, create_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(3))
                """, longOrNull(body, "planId"), intVal(body, "orderNum", 1),
                str(body, "shiftName"), body.get("startTime"), body.get("endTime"),
                str(body, "remark", ""), str(body, "enableFlag", "Y"));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_shift")) {
            return MesApiResponse.toAjax(1);
        }
        int n = jdbc.update("""
                UPDATE cal_shift SET plan_id=?, order_num=?, shift_name=?, start_time=?, end_time=?,
                  remark=?, enable_flag=?, update_time=NOW(3)
                WHERE shift_id=?
                """, longOrNull(body, "planId"), intVal(body, "orderNum", 1),
                str(body, "shiftName"), body.get("startTime"), body.get("endTime"),
                str(body, "remark", ""), str(body, "enableFlag", "Y"), longVal(body, "shiftId"));
        return MesApiResponse.toAjax(n);
    }

    @DeleteMapping("/{shiftIds}")
    public Map<String, Object> remove(@PathVariable String shiftIds) {
        if (!tableExists("cal_shift")) {
            return MesApiResponse.toAjax(1);
        }
        int n = 0;
        for (String p : shiftIds.split(",")) {
            n += jdbc.update("DELETE FROM cal_shift WHERE shift_id=?", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }

    private Map<String, Object> mapShift(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shiftId", rs.getLong("shift_id"));
        m.put("planId", rs.getObject("plan_id"));
        m.put("orderNum", rs.getObject("order_num"));
        m.put("shiftName", rs.getString("shift_name"));
        m.put("startTime", rs.getTime("start_time"));
        m.put("endTime", rs.getTime("end_time"));
        m.put("remark", rs.getString("remark"));
        m.put("enableFlag", rs.getString("enable_flag"));
        return m;
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

    private static Long longOrNull(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
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
