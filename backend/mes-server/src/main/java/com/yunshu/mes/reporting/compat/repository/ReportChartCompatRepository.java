package com.yunshu.mes.reporting.compat.repository;

import com.yunshu.mes.reporting.compat.AnalyticsJdbcHelper;
import com.yunshu.mes.system.compat.SysCompatHelper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ReportChartCompatRepository {

    private static final String BASE = """
            SELECT chart_id, chart_code, chart_name, chart_type, business_type, api, options,
                   chart_pic, enable_flag, remark, create_by, create_time, update_by, update_time
            FROM report_chart
            """;

    private final JdbcTemplate jdbc;

    public ReportChartCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY chart_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), AnalyticsJdbcHelper::chartRow, args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM report_chart WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long chartId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE chart_id = ?",
                AnalyticsJdbcHelper::chartRow, chartId);
        return rows.stream().findFirst();
    }

    public List<Long> roleIds(Long chartId) {
        return jdbc.query("SELECT role_id FROM report_chart_role WHERE chart_id = ?",
                (rs, n) -> rs.getLong("role_id"), chartId);
    }

    public List<Map<String, Object>> getMyCharts(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", roleIds.stream().map(i -> "?").toList());
        String sql = """
                SELECT DISTINCT c.chart_id, c.chart_code, c.chart_name, c.chart_type, c.business_type,
                       c.api, c.options, c.chart_pic, c.enable_flag, c.remark,
                       c.create_by, c.create_time, c.update_by, c.update_time
                FROM report_chart c
                INNER JOIN report_chart_role cr ON c.chart_id = cr.chart_id
                WHERE cr.role_id IN (%s) AND c.enable_flag = 'Y'
                ORDER BY c.create_time ASC
                """.formatted(placeholders);
        return jdbc.query(sql, AnalyticsJdbcHelper::chartRow, roleIds.toArray());
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String user = SysCompatHelper.currentUsername();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO report_chart (chart_code, chart_name, chart_type, business_type, api, options,
                      chart_pic, enable_flag, remark, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("chartCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("chartName")));
            ps.setString(i++, SysCompatHelper.str(body.get("chartType")));
            ps.setString(i++, SysCompatHelper.str(body.get("businessType")));
            ps.setString(i++, SysCompatHelper.str(body.get("api")));
            ps.setString(i++, SysCompatHelper.str(body.get("options")));
            ps.setString(i++, SysCompatHelper.str(body.get("chartPic")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("enableFlag"), "Y"));
            ps.setString(i++, SysCompatHelper.str(body.get("remark")));
            ps.setString(i++, user);
            ps.setTimestamp(i, now);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbc.update("""
                UPDATE report_chart SET chart_code=?, chart_name=?, chart_type=?, business_type=?,
                  api=?, options=?, chart_pic=?, enable_flag=?, remark=?, update_by=?, update_time=?
                WHERE chart_id=?
                """,
                SysCompatHelper.str(body.get("chartCode")),
                SysCompatHelper.str(body.get("chartName")),
                SysCompatHelper.str(body.get("chartType")),
                SysCompatHelper.str(body.get("businessType")),
                SysCompatHelper.str(body.get("api")),
                SysCompatHelper.str(body.get("options")),
                SysCompatHelper.str(body.get("chartPic")),
                SysCompatHelper.strOr(body.get("enableFlag"), "Y"),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.currentUsername(),
                now,
                SysCompatHelper.longVal(body.get("chartId")));
    }

    public void replaceRoles(Long chartId, Object roleIdsObj) {
        jdbc.update("DELETE FROM report_chart_role WHERE chart_id = ?", chartId);
        if (roleIdsObj == null) {
            return;
        }
        List<Long> roleIds = new ArrayList<>();
        if (roleIdsObj instanceof List<?> list) {
            for (Object o : list) {
                roleIds.add(SysCompatHelper.longVal(o));
            }
        }
        for (Long roleId : roleIds) {
            jdbc.update("INSERT INTO report_chart_role (chart_id, role_id) VALUES (?,?)", chartId, roleId);
        }
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        for (Long id : ids) {
            jdbc.update("DELETE FROM report_chart_role WHERE chart_id = ?", id);
        }
        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        return jdbc.update("DELETE FROM report_chart WHERE chart_id IN (" + placeholders + ")", ids.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        SysCompatHelper.like(sql, args, "chart_code", params.get("chartCode"));
        SysCompatHelper.like(sql, args, "chart_name", params.get("chartName"));
        SysCompatHelper.eq(sql, args, "business_type", params.get("businessType"));
        SysCompatHelper.eq(sql, args, "enable_flag", params.get("enableFlag"));
    }
}
