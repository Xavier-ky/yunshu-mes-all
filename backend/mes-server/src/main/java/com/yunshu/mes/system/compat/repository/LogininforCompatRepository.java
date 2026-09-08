package com.yunshu.mes.system.compat.repository;

import com.yunshu.mes.system.compat.SysCompatHelper;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class LogininforCompatRepository {

    private static final String BASE = """
            SELECT info_id, user_name, ipaddr, login_location, browser, os, status, msg, login_time
            FROM sys_logininfor
            """;

    private final JdbcTemplate jdbc;

    public LogininforCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY info_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapInfo(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_logininfor WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public void insert(String userName, String ipaddr, String status, String msg) {
        jdbc.update("""
                INSERT INTO sys_logininfor (user_name, ipaddr, login_location, browser, os, status, msg, login_time)
                VALUES (?,?,?,?,?,?,?,?)
                """,
                userName == null ? "" : userName,
                ipaddr == null ? "" : ipaddr,
                "",
                "",
                "",
                status,
                msg == null ? "" : msg,
                SysCompatHelper.now());
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        return jdbc.update("DELETE FROM sys_logininfor WHERE info_id IN (" + ph + ")", ids.toArray());
    }

    public int clean() {
        return jdbc.update("TRUNCATE TABLE sys_logininfor");
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        SysCompatHelper.like(sql, args, "user_name", params.get("userName"));
        SysCompatHelper.like(sql, args, "ipaddr", params.get("ipaddr"));
        String status = params.get("status");
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = ?");
            args.add(status.trim());
        }
        SysCompatHelper.dateRange(sql, args, "login_time", params, "beginTime", "endTime");
    }

    private Map<String, Object> mapInfo(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("infoId", rs.getLong("info_id"));
        m.put("userName", rs.getString("user_name"));
        m.put("ipaddr", rs.getString("ipaddr"));
        m.put("loginLocation", rs.getString("login_location"));
        m.put("browser", rs.getString("browser"));
        m.put("os", rs.getString("os"));
        m.put("status", rs.getString("status"));
        m.put("msg", rs.getString("msg"));
        m.put("loginTime", SysCompatHelper.getTs(rs, "login_time"));
        return m;
    }
}
