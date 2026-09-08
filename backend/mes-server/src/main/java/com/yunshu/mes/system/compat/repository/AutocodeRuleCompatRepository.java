package com.yunshu.mes.system.compat.repository;

import com.yunshu.mes.system.compat.SysCompatHelper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AutocodeRuleCompatRepository {

    private static final String BASE = """
            SELECT rule_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method,
                   enable_flag, remark, create_time, update_time
            FROM sys_auto_code_rule
            """;

    private final JdbcTemplate jdbc;

    public AutocodeRuleCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "rule_code", params.get("ruleCode"));
        SysCompatHelper.like(sql, args, "rule_name", params.get("ruleName"));
        sql.append(" ORDER BY rule_id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapRule(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_auto_code_rule WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "rule_code", params.get("ruleCode"));
        SysCompatHelper.like(sql, args, "rule_name", params.get("ruleName"));
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long ruleId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE rule_id = ?", (rs, n) -> mapRule(rs), ruleId);
        return rows.stream().findFirst();
    }

    public Optional<Map<String, Object>> findByCode(String ruleCode) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE rule_code = ?", (rs, n) -> mapRule(rs), ruleCode);
        return rows.stream().findFirst();
    }

    public boolean ruleCodeExists(String ruleCode, Long excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_auto_code_rule WHERE rule_code = ?"
                : "SELECT COUNT(*) FROM sys_auto_code_rule WHERE rule_code = ? AND rule_id <> ?";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, ruleCode)
                : jdbc.queryForObject(sql, Long.class, ruleCode, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO sys_auto_code_rule (rule_code, rule_name, rule_desc, max_length, is_padded, padded_char,
                        padded_method, enable_flag, remark, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("ruleCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("ruleName")));
            ps.setString(i++, SysCompatHelper.str(body.get("ruleDesc")));
            ps.setObject(i++, SysCompatHelper.intObj(body.get("maxLength")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("isPadded"), "N"));
            ps.setString(i++, SysCompatHelper.str(body.get("paddedChar")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("paddedMethod"), "L"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("enableFlag"), "Y"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("remark"), ""));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, SysCompatHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        Long ruleId = SysCompatHelper.longObj(body.get("ruleId"));
        if (ruleId == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE sys_auto_code_rule SET rule_code=?, rule_name=?, rule_desc=?, max_length=?, is_padded=?,
                    padded_char=?, padded_method=?, enable_flag=?, remark=?, update_by=?, update_time=?
                WHERE rule_id=?
                """,
                SysCompatHelper.str(body.get("ruleCode")),
                SysCompatHelper.str(body.get("ruleName")),
                SysCompatHelper.str(body.get("ruleDesc")),
                SysCompatHelper.intObj(body.get("maxLength")),
                SysCompatHelper.strOr(body.get("isPadded"), "N"),
                SysCompatHelper.str(body.get("paddedChar")),
                SysCompatHelper.strOr(body.get("paddedMethod"), "L"),
                SysCompatHelper.strOr(body.get("enableFlag"), "Y"),
                SysCompatHelper.strOr(body.get("remark"), ""),
                SysCompatHelper.currentUsername(),
                SysCompatHelper.now(),
                ruleId);
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        jdbc.update("DELETE FROM sys_auto_code_part WHERE rule_id IN (" + ph + ")", ids.toArray());
        jdbc.update("DELETE FROM sys_auto_code_result WHERE rule_id IN (" + ph + ")", ids.toArray());
        return jdbc.update("DELETE FROM sys_auto_code_rule WHERE rule_id IN (" + ph + ")", ids.toArray());
    }

    private Map<String, Object> mapRule(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ruleId", rs.getLong("rule_id"));
        m.put("ruleCode", rs.getString("rule_code"));
        m.put("ruleName", rs.getString("rule_name"));
        m.put("ruleDesc", rs.getString("rule_desc"));
        m.put("maxLength", rs.getObject("max_length"));
        m.put("isPadded", rs.getString("is_padded"));
        m.put("paddedChar", rs.getString("padded_char"));
        m.put("paddedMethod", rs.getString("padded_method"));
        m.put("enableFlag", rs.getString("enable_flag"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", SysCompatHelper.getTs(rs, "create_time"));
        return m;
    }
}
