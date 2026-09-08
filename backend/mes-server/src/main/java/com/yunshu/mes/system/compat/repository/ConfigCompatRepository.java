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
public class ConfigCompatRepository {

    private static final String BASE = "SELECT config_id, config_name, config_key, config_value, config_type, remark, create_time FROM sys_config";

    private final JdbcTemplate jdbc;

    public ConfigCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "config_name", params.get("configName"));
        SysCompatHelper.like(sql, args, "config_key", params.get("configKey"));
        SysCompatHelper.eq(sql, args, "config_type", params.get("configType"));
        sql.append(" ORDER BY config_id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapConfig(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_config WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "config_name", params.get("configName"));
        SysCompatHelper.like(sql, args, "config_key", params.get("configKey"));
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Integer configId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE config_id = ?", (rs, n) -> mapConfig(rs), configId);
        return rows.stream().findFirst();
    }

    public Optional<String> findValueByKey(String configKey) {
        List<String> rows = jdbc.query("SELECT config_value FROM sys_config WHERE config_key = ?",
                (rs, n) -> rs.getString(1), configKey);
        return rows.stream().findFirst();
    }

    public boolean configKeyExists(String configKey, Integer excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_config WHERE config_key = ?"
                : "SELECT COUNT(*) FROM sys_config WHERE config_key = ? AND config_id <> ?";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, configKey)
                : jdbc.queryForObject(sql, Long.class, configKey, excludeId);
        return c != null && c > 0;
    }

    public Integer insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time) VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("configName")));
            ps.setString(i++, SysCompatHelper.str(body.get("configKey")));
            ps.setString(i++, SysCompatHelper.str(body.get("configValue")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("configType"), "N"));
            ps.setString(i++, SysCompatHelper.str(body.get("remark")));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, SysCompatHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.intValue();
    }

    public int update(Map<String, Object> body) {
        Integer configId = SysCompatHelper.intObj(body.get("configId"));
        if (configId == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE sys_config SET config_name=?, config_key=?, config_value=?, config_type=?, remark=?,
                    update_by=?, update_time=?
                WHERE config_id=?
                """,
                SysCompatHelper.str(body.get("configName")),
                SysCompatHelper.str(body.get("configKey")),
                SysCompatHelper.str(body.get("configValue")),
                SysCompatHelper.strOr(body.get("configType"), "N"),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.currentUsername(),
                SysCompatHelper.now(),
                configId);
    }

    public int deleteByIds(List<Integer> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        return jdbc.update("DELETE FROM sys_config WHERE config_id IN (" + ph + ")", ids.toArray());
    }

    private Map<String, Object> mapConfig(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("configId", rs.getInt("config_id"));
        m.put("configName", rs.getString("config_name"));
        m.put("configKey", rs.getString("config_key"));
        m.put("configValue", rs.getString("config_value"));
        m.put("configType", rs.getString("config_type"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", SysCompatHelper.getTs(rs, "create_time"));
        return m;
    }
}
