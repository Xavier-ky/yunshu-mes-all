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
public class DictDataCompatRepository {

    private static final String BASE = """
            SELECT dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                   is_default, status, remark, create_time
            FROM sys_dict_data
            """;

    private final JdbcTemplate jdbc;

    public DictDataCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "dict_label", params.get("dictLabel"));
        SysCompatHelper.eq(sql, args, "dict_type", params.get("dictType"));
        SysCompatHelper.eq(sql, args, "status", params.get("status"));
        sql.append(" ORDER BY dict_sort, dict_code LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapData(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_dict_data WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "dict_label", params.get("dictLabel"));
        SysCompatHelper.eq(sql, args, "dict_type", params.get("dictType"));
        SysCompatHelper.eq(sql, args, "status", params.get("status"));
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public List<Map<String, Object>> selectByType(String dictType) {
        return jdbc.query(BASE + " WHERE dict_type = ? AND status = '0' ORDER BY dict_sort, dict_code",
                (rs, n) -> mapData(rs), dictType);
    }

    public Optional<Map<String, Object>> findById(Long dictCode) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE dict_code = ?", (rs, n) -> mapData(rs), dictCode);
        return rows.stream().findFirst();
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                        is_default, status, remark, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setInt(i++, SysCompatHelper.intObj(body.get("dictSort")) == null ? 0 : SysCompatHelper.intObj(body.get("dictSort")));
            ps.setString(i++, SysCompatHelper.str(body.get("dictLabel")));
            ps.setString(i++, SysCompatHelper.str(body.get("dictValue")));
            ps.setString(i++, SysCompatHelper.str(body.get("dictType")));
            ps.setString(i++, SysCompatHelper.str(body.get("cssClass")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("listClass"), "default"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("isDefault"), "N"));
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
        Long dictCode = SysCompatHelper.longObj(body.get("dictCode"));
        if (dictCode == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE sys_dict_data SET dict_sort=?, dict_label=?, dict_value=?, dict_type=?, css_class=?,
                    list_class=?, is_default=?, status=?, remark=?, update_by=?, update_time=?
                WHERE dict_code=?
                """,
                SysCompatHelper.intObj(body.get("dictSort")) == null ? 0 : SysCompatHelper.intObj(body.get("dictSort")),
                SysCompatHelper.str(body.get("dictLabel")),
                SysCompatHelper.str(body.get("dictValue")),
                SysCompatHelper.str(body.get("dictType")),
                SysCompatHelper.str(body.get("cssClass")),
                SysCompatHelper.strOr(body.get("listClass"), "default"),
                SysCompatHelper.strOr(body.get("isDefault"), "N"),
                SysCompatHelper.strOr(body.get("status"), "0"),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.currentUsername(),
                SysCompatHelper.now(),
                dictCode);
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        return jdbc.update("DELETE FROM sys_dict_data WHERE dict_code IN (" + ph + ")", ids.toArray());
    }

    private Map<String, Object> mapData(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dictCode", rs.getLong("dict_code"));
        m.put("dictSort", rs.getInt("dict_sort"));
        m.put("dictLabel", rs.getString("dict_label"));
        m.put("dictValue", rs.getString("dict_value"));
        m.put("dictType", rs.getString("dict_type"));
        m.put("cssClass", rs.getString("css_class"));
        m.put("listClass", rs.getString("list_class"));
        m.put("isDefault", rs.getString("is_default"));
        m.put("status", rs.getString("status"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", SysCompatHelper.getTs(rs, "create_time"));
        return m;
    }
}
