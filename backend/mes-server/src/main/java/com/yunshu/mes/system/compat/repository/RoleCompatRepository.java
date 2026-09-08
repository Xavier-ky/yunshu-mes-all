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
public class RoleCompatRepository {

    private static final String BASE = """
            SELECT role_id, role_code, role_name, role_key, role_sort, data_scope,
                   menu_check_strictly, dept_check_strictly, role_desc, status, remark, created_at, updated_at
            FROM sys_role
            """;

    private final JdbcTemplate jdbc;

    public RoleCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY role_sort, role_id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapRole(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_role WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public List<Map<String, Object>> selectAll() {
        return jdbc.query(BASE + " ORDER BY role_sort, role_id", (rs, n) -> mapRole(rs));
    }

    public Optional<Map<String, Object>> findById(Long roleId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE role_id = ?", (rs, n) -> mapRole(rs), roleId);
        return rows.stream().findFirst();
    }

    public List<Map<String, Object>> listAllForReference() {
        return jdbc.query(
                "SELECT role_code, role_key, role_name FROM sys_role ORDER BY role_sort, role_id",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    String key = rs.getString("role_key") != null ? rs.getString("role_key") : rs.getString("role_code");
                    m.put("roleKey", key);
                    m.put("roleName", rs.getString("role_name"));
                    return m;
                });
    }

    public ResolveResult resolveRoleId(String roleKey, String roleName) {
        if (StringUtils.hasText(roleKey)) {
            String key = roleKey.trim();
            List<Long> ids = jdbc.query(
                    "SELECT role_id FROM sys_role WHERE role_key = ? OR role_code = ?",
                    (rs, n) -> rs.getLong(1),
                    key, key);
            if (ids.isEmpty()) {
                return ResolveResult.failure("角色编码「" + key + "」不存在");
            }
            if (ids.size() > 1) {
                return ResolveResult.failure("角色编码「" + key + "」存在多条记录");
            }
            return ResolveResult.success(ids.get(0));
        }
        if (StringUtils.hasText(roleName)) {
            List<Long> ids = jdbc.query(
                    "SELECT role_id FROM sys_role WHERE role_name = ?",
                    (rs, n) -> rs.getLong(1),
                    roleName.trim());
            if (ids.isEmpty()) {
                return ResolveResult.failure("角色名称「" + roleName.trim() + "」不存在");
            }
            if (ids.size() > 1) {
                return ResolveResult.failure("角色名称「" + roleName.trim() + "」存在多个匹配，请填写角色编码");
            }
            return ResolveResult.success(ids.get(0));
        }
        return ResolveResult.failure("角色名称或角色编码至少填写一项");
    }

    public boolean roleNameExists(String roleName, Long excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_role WHERE role_name = ?"
                : "SELECT COUNT(*) FROM sys_role WHERE role_name = ? AND role_id <> ?";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, roleName)
                : jdbc.queryForObject(sql, Long.class, roleName, excludeId);
        return c != null && c > 0;
    }

    public boolean roleKeyExists(String roleKey, Long excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_role WHERE role_key = ? OR role_code = ?"
                : "SELECT COUNT(*) FROM sys_role WHERE (role_key = ? OR role_code = ?) AND role_id <> ?";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, roleKey, roleKey)
                : jdbc.queryForObject(sql, Long.class, roleKey, roleKey, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO sys_role (role_code, role_name, role_key, role_sort, data_scope,
                        menu_check_strictly, dept_check_strictly, role_desc, status, remark, created_at, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            String roleKey = SysCompatHelper.strOr(body.get("roleKey"), SysCompatHelper.str(body.get("roleCode")));
            ps.setString(i++, roleKey);
            ps.setString(i++, SysCompatHelper.str(body.get("roleName")));
            ps.setString(i++, roleKey);
            ps.setInt(i++, SysCompatHelper.intObj(body.get("roleSort")) == null ? 0 : SysCompatHelper.intObj(body.get("roleSort")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("dataScope"), "1"));
            ps.setInt(i++, SysCompatHelper.intObj(body.get("menuCheckStrictly")) == null ? 1 : SysCompatHelper.intObj(body.get("menuCheckStrictly")));
            ps.setInt(i++, SysCompatHelper.intObj(body.get("deptCheckStrictly")) == null ? 1 : SysCompatHelper.intObj(body.get("deptCheckStrictly")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("roleDesc"), SysCompatHelper.str(body.get("remark"))));
            ps.setString(i++, SysCompatHelper.fromApiStatusCode(body.get("status")));
            ps.setString(i++, SysCompatHelper.str(body.get("remark")));
            var now = SysCompatHelper.now();
            ps.setTimestamp(i++, now);
            ps.setTimestamp(i, now);
            return ps;
        }, kh);
        Number key = kh.getKey();
        Long roleId = key == null ? null : key.longValue();
        if (roleId != null) {
            saveRoleMenus(roleId, body.get("menuIds"));
            saveRoleDepts(roleId, body.get("deptIds"));
        }
        return roleId;
    }

    public int update(Map<String, Object> body) {
        Long roleId = SysCompatHelper.longObj(body.get("roleId"));
        if (roleId == null) {
            return 0;
        }
        String roleKey = SysCompatHelper.strOr(body.get("roleKey"), SysCompatHelper.str(body.get("roleCode")));
        int rows = jdbc.update("""
                UPDATE sys_role SET role_name=?, role_key=?, role_code=?, role_sort=?, data_scope=?,
                    menu_check_strictly=?, dept_check_strictly=?, role_desc=?, status=?, remark=?, updated_at=?
                WHERE role_id=?
                """,
                SysCompatHelper.str(body.get("roleName")),
                roleKey,
                roleKey,
                SysCompatHelper.intObj(body.get("roleSort")) == null ? 0 : SysCompatHelper.intObj(body.get("roleSort")),
                SysCompatHelper.strOr(body.get("dataScope"), "1"),
                SysCompatHelper.intObj(body.get("menuCheckStrictly")) == null ? 1 : SysCompatHelper.intObj(body.get("menuCheckStrictly")),
                SysCompatHelper.intObj(body.get("deptCheckStrictly")) == null ? 1 : SysCompatHelper.intObj(body.get("deptCheckStrictly")),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.fromApiStatusCode(body.get("status")),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.now(),
                roleId);
        if (rows > 0 && body.containsKey("menuIds")) {
            saveRoleMenus(roleId, body.get("menuIds"));
        }
        return rows;
    }

    public int updateDataScope(Map<String, Object> body) {
        Long roleId = SysCompatHelper.longObj(body.get("roleId"));
        if (roleId == null) {
            return 0;
        }
        int rows = jdbc.update("""
                UPDATE sys_role SET data_scope=?, dept_check_strictly=?, updated_at=? WHERE role_id=?
                """,
                SysCompatHelper.strOr(body.get("dataScope"), "1"),
                SysCompatHelper.intObj(body.get("deptCheckStrictly")) == null ? 1 : SysCompatHelper.intObj(body.get("deptCheckStrictly")),
                SysCompatHelper.now(),
                roleId);
        if (rows > 0) {
            saveRoleDepts(roleId, body.get("deptIds"));
        }
        return rows;
    }

    public int changeStatus(Long roleId, String status) {
        return jdbc.update("UPDATE sys_role SET status=?, updated_at=? WHERE role_id=?",
                SysCompatHelper.fromApiStatusCode(status), SysCompatHelper.now(), roleId);
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        for (Long id : ids) {
            jdbc.update("DELETE FROM sys_role_menu WHERE role_id = ?", id);
            jdbc.update("DELETE FROM sys_role_dept WHERE role_id = ?", id);
            jdbc.update("DELETE FROM sys_user_role WHERE role_id = ?", id);
        }
        return jdbc.update("DELETE FROM sys_role WHERE role_id IN (" + ph + ")", ids.toArray());
    }

    public List<Long> selectMenuIdsByRoleId(Long roleId) {
        return jdbc.query("SELECT menu_id FROM sys_role_menu WHERE role_id = ?", (rs, n) -> rs.getLong(1), roleId);
    }

    public List<Long> selectDeptIdsByRoleId(Long roleId) {
        return jdbc.query("SELECT dept_id FROM sys_role_dept WHERE role_id = ?", (rs, n) -> rs.getLong(1), roleId);
    }

    public int deleteAuthUser(Long roleId, Long userId) {
        return jdbc.update("DELETE FROM sys_user_role WHERE role_id = ? AND user_id = ?", roleId, userId);
    }

    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            return 0;
        }
        int total = 0;
        for (Long userId : userIds) {
            total += deleteAuthUser(roleId, userId);
        }
        return total;
    }

    public int insertAuthUsers(Long roleId, Long[] userIds) {
        if (userIds == null) {
            return 0;
        }
        int total = 0;
        for (Long userId : userIds) {
            if (userId != null) {
                Long c = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_user_role WHERE role_id = ? AND user_id = ?",
                        Long.class, roleId, userId);
                if (c == null || c == 0) {
                    total += jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?,?)", userId, roleId);
                }
            }
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private void saveRoleMenus(Long roleId, Object menuIdsObj) {
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        if (menuIdsObj instanceof List<?> list) {
            for (Object o : list) {
                Long menuId = SysCompatHelper.longObj(o);
                if (menuId != null) {
                    jdbc.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?,?)", roleId, menuId);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void saveRoleDepts(Long roleId, Object deptIdsObj) {
        jdbc.update("DELETE FROM sys_role_dept WHERE role_id = ?", roleId);
        if (deptIdsObj instanceof List<?> list) {
            for (Object o : list) {
                Long deptId = SysCompatHelper.longObj(o);
                if (deptId != null) {
                    jdbc.update("INSERT INTO sys_role_dept (role_id, dept_id) VALUES (?,?)", roleId, deptId);
                }
            }
        }
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        SysCompatHelper.like(sql, args, "role_name", params.get("roleName"));
        SysCompatHelper.like(sql, args, "role_key", params.get("roleKey"));
        String status = params.get("status");
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = ?");
            args.add(SysCompatHelper.fromApiStatusCode(status));
        }
        SysCompatHelper.dateRange(sql, args, "created_at", params, "beginTime", "endTime");
    }

    private Map<String, Object> mapRole(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roleId", rs.getLong("role_id"));
        m.put("roleName", rs.getString("role_name"));
        m.put("roleKey", rs.getString("role_key") != null ? rs.getString("role_key") : rs.getString("role_code"));
        m.put("roleSort", rs.getInt("role_sort"));
        m.put("dataScope", rs.getString("data_scope"));
        m.put("menuCheckStrictly", rs.getInt("menu_check_strictly"));
        m.put("deptCheckStrictly", rs.getInt("dept_check_strictly"));
        m.put("status", SysCompatHelper.toApiStatusCode(rs.getString("status")));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", SysCompatHelper.getTs(rs, "created_at"));
        return m;
    }
}
