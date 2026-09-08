package com.yunshu.mes.cal.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import java.sql.Date;
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
@RequestMapping("/api/mes/cal/calholiday")
public class CalHolidayController {

    private final JdbcTemplate jdbc;

    public CalHolidayController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        if (!tableExists("cal_holiday")) {
            return MesApiResponse.ok(List.of());
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT holiday_id, the_day, holiday_type, start_time, end_time, remark
                FROM cal_holiday ORDER BY the_day
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("holidayId", rs.getLong("holiday_id"));
            m.put("theDay", rs.getDate("the_day") != null ? rs.getDate("the_day").toString() : null);
            m.put("holidayType", rs.getString("holiday_type"));
            m.put("startTime", rs.getTimestamp("start_time"));
            m.put("endTime", rs.getTimestamp("end_time"));
            m.put("remark", rs.getString("remark"));
            return m;
        });
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/{holidayId}")
    public Map<String, Object> getInfo(@PathVariable Long holidayId) {
        if (!tableExists("cal_holiday")) {
            return MesApiResponse.error("记录不存在");
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT holiday_id, the_day, holiday_type, start_time, end_time, remark
                FROM cal_holiday WHERE holiday_id = ?
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("holidayId", rs.getLong("holiday_id"));
            m.put("theDay", rs.getDate("the_day") != null ? rs.getDate("the_day").toString() : null);
            m.put("holidayType", rs.getString("holiday_type"));
            m.put("remark", rs.getString("remark"));
            return m;
        }, holidayId);
        return rows.isEmpty() ? MesApiResponse.error("记录不存在") : MesApiResponse.ok(rows.get(0));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_holiday")) {
            return MesApiResponse.toAjax(1);
        }
        Object theDay = body.get("theDay");
        List<Long> existing = jdbc.query("""
                SELECT holiday_id FROM cal_holiday WHERE the_day = ?
                """, (rs, n) -> rs.getLong("holiday_id"), parseDay(theDay));
        if (!existing.isEmpty()) {
            body.put("holidayId", existing.get(0));
            return edit(body);
        }
        jdbc.update("""
                INSERT INTO cal_holiday (the_day, holiday_type, start_time, end_time, remark, create_time)
                VALUES (?, ?, ?, ?, ?, NOW(3))
                """, parseDay(theDay), str(body, "holidayType", "HOLIDAY"),
                body.get("startTime"), body.get("endTime"), str(body, "remark", ""));
        return MesApiResponse.toAjax(1);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_holiday")) {
            return MesApiResponse.toAjax(1);
        }
        int n = jdbc.update("""
                UPDATE cal_holiday SET the_day=?, holiday_type=?, start_time=?, end_time=?, remark=?, update_time=NOW(3)
                WHERE holiday_id=?
                """, parseDay(body.get("theDay")), str(body, "holidayType", "HOLIDAY"),
                body.get("startTime"), body.get("endTime"), str(body, "remark", ""),
                longVal(body, "holidayId"));
        return MesApiResponse.toAjax(n);
    }

    @DeleteMapping("/{holidayIds}")
    public Map<String, Object> remove(@PathVariable String holidayIds) {
        if (!tableExists("cal_holiday")) {
            return MesApiResponse.toAjax(1);
        }
        int n = 0;
        for (String p : holidayIds.split(",")) {
            n += jdbc.update("DELETE FROM cal_holiday WHERE holiday_id=?", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }

    private Date parseDay(Object v) {
        if (v == null) {
            return new Date(System.currentTimeMillis());
        }
        if (v instanceof Date d) {
            return d;
        }
        String s = String.valueOf(v);
        if (s.length() >= 10) {
            s = s.substring(0, 10);
        }
        return Date.valueOf(s);
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
