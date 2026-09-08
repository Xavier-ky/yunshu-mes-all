package com.yunshu.mes.system.compat.repository;

import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.ResolveResult;
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
public class DeptCompatRepository {

    private static final String BASE = """
            SELECT dept_id, dept_code, dept_name, parent_dept_id, ancestors, order_num,
                   leader, phone, email, status, is_deleted, created_at, updated_at
            FROM sys_department
            """;

    private final JdbcTemplate jdbc;

    public DeptCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> selectList(Map<String, String> params) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE is_deleted = 0 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY parent_dept_id, order_num, dept_id");
        return jdbc.query(sql.toString(), (rs, n) -> mapDept(rs), args.toArray());
    }

    public Optional<Map<String, Object>> findById(Long deptId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE dept_id = ? AND is_deleted = 0",
                (rs, n) -> mapDept(rs), deptId);
        return rows.stream().findFirst();
    }

    public boolean hasChildren(Long deptId) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_department WHERE parent_dept_id = ? AND is_deleted = 0",
                Long.class, deptId);
        return c != null && c > 0;
    }

    public boolean hasUsers(Long deptId) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE dept_id = ? AND is_deleted = 0",
                Long.class, deptId);
        return c != null && c > 0;
    }

    public boolean deptNameExists(String deptName, Long parentId, Long excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_department WHERE dept_name = ? AND parent_dept_id <=> ? AND is_deleted = 0"
                : "SELECT COUNT(*) FROM sys_department WHERE dept_name = ? AND parent_dept_id <=> ? AND dept_id <> ? AND is_deleted = 0";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, deptName, parentId)
                : jdbc.queryForObject(sql, Long.class, deptName, parentId, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        Long parentId = SysCompatHelper.longObj(body.get("parentId"));
        if (parentId == null) {
            parentId = 0L;
        }
        String ancestors = buildAncestors(parentId);
        KeyHolder kh = new GeneratedKeyHolder();
        Long finalParentId = parentId;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO sys_department (dept_code, dept_name, parent_dept_id, ancestors, order_num,
                        leader, phone, email, dept_type, status, created_at, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,'MANAGEMENT',?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            String code = SysCompatHelper.str(body.get("deptCode"));
            if (!StringUtils.hasText(code)) {
                code = "D" + System.currentTimeMillis() % 100000;
            }
            ps.setString(i++, code);
            ps.setString(i++, SysCompatHelper.str(body.get("deptName")));
            ps.setObject(i++, finalParentId == 0L ? null : finalParentId);
            ps.setString(i++, ancestors);
            ps.setInt(i++, SysCompatHelper.intObj(body.get("orderNum")) == null ? 0 : SysCompatHelper.intObj(body.get("orderNum")));
            ps.setString(i++, SysCompatHelper.str(body.get("leader")));
            ps.setString(i++, SysCompatHelper.str(body.get("phone")));
            ps.setString(i++, SysCompatHelper.str(body.get("email")));
            ps.setString(i++, SysCompatHelper.fromApiStatusCode(body.get("status")));
            var now = SysCompatHelper.now();
            ps.setTimestamp(i++, now);
            ps.setTimestamp(i, now);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        Long deptId = SysCompatHelper.longObj(body.get("deptId"));
        if (deptId == null) {
            return 0;
        }
        Long parentId = SysCompatHelper.longObj(body.get("parentId"));
        if (parentId == null) {
            parentId = 0L;
        }
        String ancestors = buildAncestors(parentId);
        return jdbc.update("""
                UPDATE sys_department SET dept_name=?, parent_dept_id=?, ancestors=?, order_num=?,
                    leader=?, phone=?, email=?, status=?, updated_at=?
                WHERE dept_id=? AND is_deleted=0
                """,
                SysCompatHelper.str(body.get("deptName")),
                parentId == 0L ? null : parentId,
                ancestors,
                SysCompatHelper.intObj(body.get("orderNum")) == null ? 0 : SysCompatHelper.intObj(body.get("orderNum")),
                SysCompatHelper.str(body.get("leader")),
                SysCompatHelper.str(body.get("phone")),
                SysCompatHelper.str(body.get("email")),
                SysCompatHelper.fromApiStatusCode(body.get("status")),
                SysCompatHelper.now(),
                deptId);
    }

    public int deleteById(Long deptId) {
        return jdbc.update("UPDATE sys_department SET is_deleted=1, updated_at=? WHERE dept_id=?",
                SysCompatHelper.now(), deptId);
    }

    public List<Long> selectDeptIdsByRoleId(Long roleId) {
        return jdbc.query("SELECT dept_id FROM sys_role_dept WHERE role_id = ?", (rs, n) -> rs.getLong(1), roleId);
    }

    public List<Map<String, Object>> listAllForReference() {
        return jdbc.query(
                "SELECT dept_code, dept_name FROM sys_department WHERE is_deleted = 0 ORDER BY dept_code",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("deptCode", rs.getString("dept_code"));
                    m.put("deptName", rs.getString("dept_name"));
                    return m;
                });
    }

    public ResolveResult resolveDeptId(String deptCode, String deptName) {
        if (StringUtils.hasText(deptCode)) {
            List<Long> ids = jdbc.query(
                    "SELECT dept_id FROM sys_department WHERE dept_code = ? AND is_deleted = 0",
                    (rs, n) -> rs.getLong(1),
                    deptCode.trim());
            if (ids.isEmpty()) {
                return ResolveResult.failure("部门编码「" + deptCode.trim() + "」不存在");
            }
            if (ids.size() > 1) {
                return ResolveResult.failure("部门编码「" + deptCode.trim() + "」存在多条记录");
            }
            return ResolveResult.success(ids.get(0));
        }
        if (StringUtils.hasText(deptName)) {
            List<Long> ids = jdbc.query(
                    "SELECT dept_id FROM sys_department WHERE dept_name = ? AND is_deleted = 0",
                    (rs, n) -> rs.getLong(1),
                    deptName.trim());
            if (ids.isEmpty()) {
                return ResolveResult.failure("部门名称「" + deptName.trim() + "」不存在");
            }
            if (ids.size() > 1) {
                return ResolveResult.failure("部门名称「" + deptName.trim() + "」存在多个匹配，请填写部门编码");
            }
            return ResolveResult.success(ids.get(0));
        }
        return ResolveResult.failure("部门名称或部门编码至少填写一项");
    }

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        List<String> rows = jdbc.query(
                "SELECT ancestors FROM sys_department WHERE dept_id = ?",
                (rs, n) -> rs.getString(1), parentId);
        if (rows.isEmpty()) {
            return "0," + parentId;
        }
        return rows.get(0) + "," + parentId;
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        SysCompatHelper.like(sql, args, "dept_name", params.get("deptName"));
        String status = params.get("status");
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = ?");
            args.add(SysCompatHelper.fromApiStatusCode(status));
        }
    }

    private Map<String, Object> mapDept(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("deptId", rs.getLong("dept_id"));
        m.put("parentId", rs.getObject("parent_dept_id") == null ? 0L : rs.getLong("parent_dept_id"));
        m.put("deptCode", rs.getString("dept_code"));
        m.put("deptName", rs.getString("dept_name"));
        m.put("ancestors", rs.getString("ancestors"));
        m.put("orderNum", rs.getInt("order_num"));
        m.put("leader", rs.getString("leader"));
        m.put("phone", rs.getString("phone"));
        m.put("email", rs.getString("email"));
        m.put("status", SysCompatHelper.toApiStatusCode(rs.getString("status")));
        m.put("delFlag", SysCompatHelper.delFlag(rs.getInt("is_deleted")));
        m.put("createTime", SysCompatHelper.getTs(rs, "created_at"));
        return m;
    }
}
