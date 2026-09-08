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
public class PostCompatRepository {

    private static final String BASE = """
            SELECT post_id, post_code, post_name, post_sort, status, remark, create_time, update_time
            FROM sys_post
            """;

    private final JdbcTemplate jdbc;

    public PostCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY post_sort, post_id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapPost(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_post WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public List<Map<String, Object>> selectAll() {
        return jdbc.query(BASE + " ORDER BY post_sort, post_id", (rs, n) -> mapPost(rs));
    }

    public Optional<Map<String, Object>> findById(Long postId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE post_id = ?", (rs, n) -> mapPost(rs), postId);
        return rows.stream().findFirst();
    }

    public boolean postCodeExists(String postCode, Long excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_post WHERE post_code = ?"
                : "SELECT COUNT(*) FROM sys_post WHERE post_code = ? AND post_id <> ?";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, postCode)
                : jdbc.queryForObject(sql, Long.class, postCode, excludeId);
        return c != null && c > 0;
    }

    public boolean postNameExists(String postName, Long excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_post WHERE post_name = ?"
                : "SELECT COUNT(*) FROM sys_post WHERE post_name = ? AND post_id <> ?";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, postName)
                : jdbc.queryForObject(sql, Long.class, postName, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO sys_post (post_code, post_name, post_sort, status, remark, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("postCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("postName")));
            ps.setInt(i++, SysCompatHelper.intObj(body.get("postSort")) == null ? 0 : SysCompatHelper.intObj(body.get("postSort")));
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
        Long postId = SysCompatHelper.longObj(body.get("postId"));
        if (postId == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE sys_post SET post_code=?, post_name=?, post_sort=?, status=?, remark=?,
                    update_by=?, update_time=?
                WHERE post_id=?
                """,
                SysCompatHelper.str(body.get("postCode")),
                SysCompatHelper.str(body.get("postName")),
                SysCompatHelper.intObj(body.get("postSort")) == null ? 0 : SysCompatHelper.intObj(body.get("postSort")),
                SysCompatHelper.strOr(body.get("status"), "0"),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.currentUsername(),
                SysCompatHelper.now(),
                postId);
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        jdbc.update("DELETE FROM sys_user_post WHERE post_id IN (" + ph + ")", ids.toArray());
        return jdbc.update("DELETE FROM sys_post WHERE post_id IN (" + ph + ")", ids.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        SysCompatHelper.like(sql, args, "post_code", params.get("postCode"));
        SysCompatHelper.like(sql, args, "post_name", params.get("postName"));
        SysCompatHelper.eq(sql, args, "status", params.get("status"));
    }

    private Map<String, Object> mapPost(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("postId", rs.getLong("post_id"));
        m.put("postCode", rs.getString("post_code"));
        m.put("postName", rs.getString("post_name"));
        m.put("postSort", rs.getInt("post_sort"));
        m.put("status", rs.getString("status"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", SysCompatHelper.getTs(rs, "create_time"));
        return m;
    }
}
