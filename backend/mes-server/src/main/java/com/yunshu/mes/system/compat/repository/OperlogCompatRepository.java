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
public class OperlogCompatRepository {

    private static final String BASE = """
            SELECT oper_id, title, business_type, method, request_method, operator_type, oper_name, dept_name,
                   oper_url, oper_ip, oper_location, oper_param, json_result, status, error_msg, oper_time
            FROM sys_oper_log
            """;

    private final JdbcTemplate jdbc;

    public OperlogCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY oper_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapLog(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_oper_log WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public int insert(Map<String, Object> body) {
        return jdbc.update("""
                INSERT INTO sys_oper_log (title, business_type, method, request_method, operator_type, oper_name,
                    dept_name, oper_url, oper_ip, oper_location, oper_param, json_result, status, error_msg, oper_time)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                SysCompatHelper.strOr(body.get("title"), ""),
                SysCompatHelper.intObj(body.get("businessType")) == null ? 0 : SysCompatHelper.intObj(body.get("businessType")),
                SysCompatHelper.strOr(body.get("method"), ""),
                SysCompatHelper.strOr(body.get("requestMethod"), ""),
                1,
                SysCompatHelper.strOr(body.get("operName"), ""),
                "",
                SysCompatHelper.strOr(body.get("operUrl"), ""),
                SysCompatHelper.strOr(body.get("operIp"), ""),
                "",
                SysCompatHelper.strOr(body.get("operParam"), ""),
                SysCompatHelper.strOr(body.get("jsonResult"), ""),
                SysCompatHelper.intObj(body.get("status")) == null ? 0 : SysCompatHelper.intObj(body.get("status")),
                SysCompatHelper.strOr(body.get("errorMsg"), ""),
                SysCompatHelper.now());
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        return jdbc.update("DELETE FROM sys_oper_log WHERE oper_id IN (" + ph + ")", ids.toArray());
    }

    public int clean() {
        return jdbc.update("TRUNCATE TABLE sys_oper_log");
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        SysCompatHelper.like(sql, args, "title", params.get("title"));
        SysCompatHelper.like(sql, args, "oper_name", params.get("operName"));
        String businessType = params.get("businessType");
        if (StringUtils.hasText(businessType)) {
            sql.append(" AND business_type = ?");
            args.add(Integer.parseInt(businessType.trim()));
        }
        String status = params.get("status");
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = ?");
            args.add(Integer.parseInt(status.trim()));
        }
        SysCompatHelper.dateRange(sql, args, "oper_time", params, "beginTime", "endTime");
    }

    private Map<String, Object> mapLog(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("operId", rs.getLong("oper_id"));
        m.put("title", rs.getString("title"));
        m.put("businessType", rs.getInt("business_type"));
        m.put("method", rs.getString("method"));
        m.put("requestMethod", rs.getString("request_method"));
        m.put("operName", rs.getString("oper_name"));
        m.put("deptName", rs.getString("dept_name"));
        m.put("operUrl", rs.getString("oper_url"));
        m.put("operIp", rs.getString("oper_ip"));
        m.put("operParam", rs.getString("oper_param"));
        m.put("jsonResult", rs.getString("json_result"));
        m.put("status", rs.getInt("status"));
        m.put("errorMsg", rs.getString("error_msg"));
        m.put("operTime", SysCompatHelper.getTs(rs, "oper_time"));
        return m;
    }
}
