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
import org.springframework.util.StringUtils;

@Repository
public class DictTypeCompatRepository {

    private static final String BASE = "SELECT dict_id, dict_name, dict_type, status, remark, create_time FROM sys_dict_type";

    private final JdbcTemplate jdbc;

    public DictTypeCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "dict_name", params.get("dictName"));
        SysCompatHelper.like(sql, args, "dict_type", params.get("dictType"));
        SysCompatHelper.eq(sql, args, "status", params.get("status"));
        SysCompatHelper.dateRange(sql, args, "create_time", params, "beginTime", "endTime");
        sql.append(" ORDER BY dict_id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapType(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_dict_type WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "dict_name", params.get("dictName"));
        SysCompatHelper.like(sql, args, "dict_type", params.get("dictType"));
        SysCompatHelper.eq(sql, args, "status", params.get("status"));
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long dictId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE dict_id = ?", (rs, n) -> mapType(rs), dictId);
        return rows.stream().findFirst();
    }

    public boolean dictTypeExists(String dictType, Long excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_dict_type WHERE dict_type = ?"
                : "SELECT COUNT(*) FROM sys_dict_type WHERE dict_type = ? AND dict_id <> ?";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, dictType)
                : jdbc.queryForObject(sql, Long.class, dictType, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sys_dict_type (dict_name, dict_type, status, remark, create_by, create_time) VALUES (?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("dictName")));
            ps.setString(i++, SysCompatHelper.str(body.get("dictType")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("status"), "0"));
            ps.setString(i++, SysCompatHelper.str(body.get("remark")));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, SysCompatHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        Long dictId = SysCompatHelper.longObj(body.get("dictId"));
        if (dictId == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE sys_dict_type SET dict_name=?, dict_type=?, status=?, remark=?, update_by=?, update_time=?
                WHERE dict_id=?
                """,
                SysCompatHelper.str(body.get("dictName")),
                SysCompatHelper.str(body.get("dictType")),
                SysCompatHelper.strOr(body.get("status"), "0"),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.currentUsername(),
                SysCompatHelper.now(),
                dictId);
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        List<Map<String, Object>> types = jdbc.query(
                "SELECT dict_type FROM sys_dict_type WHERE dict_id IN (" + ph + ")",
                (rs, n) -> Map.of("dictType", rs.getString(1)), ids.toArray());
        for (Map<String, Object> t : types) {
            jdbc.update("DELETE FROM sys_dict_data WHERE dict_type = ?", t.get("dictType"));
        }
        return jdbc.update("DELETE FROM sys_dict_type WHERE dict_id IN (" + ph + ")", ids.toArray());
    }

    private Map<String, Object> mapType(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dictId", rs.getLong("dict_id"));
        m.put("dictName", rs.getString("dict_name"));
        m.put("dictType", rs.getString("dict_type"));
        m.put("status", rs.getString("status"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", SysCompatHelper.getTs(rs, "create_time"));
        return m;
    }
}
