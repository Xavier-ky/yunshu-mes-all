package com.yunshu.mes.system.compat.repository;

import com.yunshu.mes.system.compat.SysCompatHelper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
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
public class UserCompatRepository {

    private static final String BASE = """
            SELECT u.user_id, u.username, u.password_hash, u.real_name, u.nick_name, u.employee_no,
                   u.dept_id, d.dept_name, d.dept_code, u.phone, u.email, u.sex, u.avatar, u.remark,
                   u.status, u.is_deleted, u.last_login_at, u.created_at, u.updated_at
            FROM sys_user u
            LEFT JOIN sys_department d ON u.dept_id = d.dept_id
            """;

    private final JdbcTemplate jdbc;

    public UserCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE u.is_deleted = 0 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY u.user_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapUser(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_user u WHERE u.is_deleted = 0 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long userId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE u.user_id = ? AND u.is_deleted = 0",
                (rs, n) -> mapUser(rs), userId);
        return rows.stream().findFirst();
    }

    public Optional<Map<String, Object>> findByUsername(String username) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE u.username = ? AND u.is_deleted = 0",
                (rs, n) -> mapUser(rs), username);
        return rows.stream().findFirst();
    }

    public Optional<String> findPasswordHash(Long userId) {
        List<String> rows = jdbc.query("SELECT password_hash FROM sys_user WHERE user_id = ? AND is_deleted = 0",
                (rs, n) -> rs.getString(1), userId);
        return rows.stream().findFirst();
    }

    public List<Long> selectRoleIds(Long userId) {
        return jdbc.query("SELECT role_id FROM sys_user_role WHERE user_id = ?",
                (rs, n) -> rs.getLong(1), userId);
    }

    public List<Long> selectPostIds(Long userId) {
        return jdbc.query("SELECT post_id FROM sys_user_post WHERE user_id = ?",
                (rs, n) -> rs.getLong(1), userId);
    }

    public List<Map<String, Object>> selectRolesByUserId(Long userId) {
        return jdbc.query("""
                SELECT r.role_id, r.role_code, r.role_name, r.role_key, r.status,
                       CASE WHEN ur.user_id IS NOT NULL THEN 1 ELSE 0 END AS flag
                FROM sys_role r
                LEFT JOIN sys_user_role ur ON r.role_id = ur.role_id AND ur.user_id = ?
                ORDER BY r.role_sort, r.role_id
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("roleId", rs.getLong("role_id"));
            m.put("roleName", rs.getString("role_name"));
            m.put("roleKey", rs.getString("role_key"));
            m.put("status", SysCompatHelper.toApiStatusCode(rs.getString("status")));
            m.put("flag", rs.getInt("flag") == 1);
            return m;
        }, userId);
    }

    public List<Map<String, Object>> selectRolesForUser(Long userId) {
        return jdbc.query("""
                SELECT r.role_id, r.role_name, r.role_key
                FROM sys_role r
                INNER JOIN sys_user_role ur ON r.role_id = ur.role_id
                WHERE ur.user_id = ?
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("roleId", rs.getLong("role_id"));
            m.put("roleName", rs.getString("role_name"));
            m.put("roleKey", rs.getString("role_key"));
            return m;
        }, userId);
    }

    public List<Map<String, Object>> selectPostsForUser(Long userId) {
        return jdbc.query("""
                SELECT p.post_id, p.post_name, p.post_code
                FROM sys_post p
                INNER JOIN sys_user_post up ON p.post_id = up.post_id
                WHERE up.user_id = ?
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("postId", rs.getLong("post_id"));
            m.put("postName", rs.getString("post_name"));
            m.put("postCode", rs.getString("post_code"));
            return m;
        }, userId);
    }

    public boolean usernameExists(String username, Long excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_user WHERE username = ? AND is_deleted = 0"
                : "SELECT COUNT(*) FROM sys_user WHERE username = ? AND user_id <> ? AND is_deleted = 0";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, username)
                : jdbc.queryForObject(sql, Long.class, username, excludeId);
        return c != null && c > 0;
    }

    public boolean phoneExists(String phone, Long excludeId) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_user WHERE phone = ? AND is_deleted = 0"
                : "SELECT COUNT(*) FROM sys_user WHERE phone = ? AND user_id <> ? AND is_deleted = 0";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, phone)
                : jdbc.queryForObject(sql, Long.class, phone, excludeId);
        return c != null && c > 0;
    }

    public boolean emailExists(String email, Long excludeId) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_user WHERE email = ? AND is_deleted = 0"
                : "SELECT COUNT(*) FROM sys_user WHERE email = ? AND user_id <> ? AND is_deleted = 0";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, email)
                : jdbc.queryForObject(sql, Long.class, email, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body, String passwordHash) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO sys_user (username, password_hash, real_name, nick_name, dept_id, phone, email,
                                          sex, avatar, remark, status, created_at, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("userName")));
            ps.setString(i++, passwordHash);
            String nick = SysCompatHelper.str(body.get("nickName"));
            ps.setString(i++, nick == null ? SysCompatHelper.str(body.get("userName")) : nick);
            ps.setString(i++, nick);
            ps.setObject(i++, SysCompatHelper.longObj(body.get("deptId")));
            ps.setString(i++, SysCompatHelper.str(body.get("phonenumber")));
            ps.setString(i++, SysCompatHelper.str(body.get("email")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("sex"), "0"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("avatar"), ""));
            ps.setString(i++, SysCompatHelper.str(body.get("remark")));
            ps.setString(i++, SysCompatHelper.fromApiStatusCode(body.get("status")));
            Timestamp now = SysCompatHelper.now();
            ps.setTimestamp(i++, now);
            ps.setTimestamp(i, now);
            return ps;
        }, kh);
        Number key = kh.getKey();
        Long userId = key == null ? null : key.longValue();
        if (userId != null) {
            saveUserRoles(userId, resolveRoleIdsFromBody(body));
            saveUserPosts(userId, body.get("postIds"));
        }
        return userId;
    }

    public int update(Map<String, Object> body) {
        Long userId = SysCompatHelper.longObj(body.get("userId"));
        if (userId == null) {
            return 0;
        }
        int rows = jdbc.update("""
                UPDATE sys_user SET nick_name=?, real_name=COALESCE(?, real_name), dept_id=?, phone=?, email=?,
                    sex=?, avatar=?, remark=?, status=?, updated_at=?
                WHERE user_id=? AND is_deleted=0
                """,
                SysCompatHelper.str(body.get("nickName")),
                SysCompatHelper.str(body.get("nickName")),
                SysCompatHelper.longObj(body.get("deptId")),
                SysCompatHelper.str(body.get("phonenumber")),
                SysCompatHelper.str(body.get("email")),
                SysCompatHelper.strOr(body.get("sex"), "0"),
                SysCompatHelper.strOr(body.get("avatar"), ""),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.fromApiStatusCode(body.get("status")),
                SysCompatHelper.now(),
                userId);
        if (rows > 0) {
            saveUserRoles(userId, resolveRoleIdsFromBody(body));
            saveUserPosts(userId, body.get("postIds"));
        }
        return rows;
    }

    public int updateProfile(Long userId, Map<String, Object> body) {
        return jdbc.update("""
                UPDATE sys_user SET nick_name=?, real_name=COALESCE(?, real_name), phone=?, email=?, sex=?, updated_at=?
                WHERE user_id=? AND is_deleted=0
                """,
                SysCompatHelper.str(body.get("nickName")),
                SysCompatHelper.str(body.get("nickName")),
                SysCompatHelper.str(body.get("phonenumber")),
                SysCompatHelper.str(body.get("email")),
                SysCompatHelper.strOr(body.get("sex"), "0"),
                SysCompatHelper.now(),
                userId);
    }

    public int resetPwd(Long userId, String passwordHash) {
        return jdbc.update("UPDATE sys_user SET password_hash=?, updated_at=? WHERE user_id=? AND is_deleted=0",
                passwordHash, SysCompatHelper.now(), userId);
    }

    public int changeStatus(Long userId, String status) {
        return jdbc.update("UPDATE sys_user SET status=?, updated_at=? WHERE user_id=? AND is_deleted=0",
                SysCompatHelper.fromApiStatusCode(status), SysCompatHelper.now(), userId);
    }

    public int updateAvatar(Long userId, String avatar) {
        return jdbc.update("UPDATE sys_user SET avatar=?, updated_at=? WHERE user_id=? AND is_deleted=0",
                avatar, SysCompatHelper.now(), userId);
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String placeholders = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        List<Object> args = new ArrayList<>();
        args.add(SysCompatHelper.now());
        args.addAll(ids);
        return jdbc.update("UPDATE sys_user SET is_deleted=1, updated_at=? WHERE user_id IN (" + placeholders + ")",
                args.toArray());
    }

    public void saveUserAuth(Long userId, Long[] roleIds) {
        jdbc.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        if (roleIds == null || roleIds.length == 0) {
            return;
        }
        int count = 0;
        Long singleRoleId = null;
        for (Long roleId : roleIds) {
            if (roleId != null) {
                count++;
                singleRoleId = roleId;
            }
        }
        if (count > 1) {
            throw new IllegalArgumentException("每个用户只能绑定一个角色");
        }
        if (singleRoleId != null) {
            jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?,?)", userId, singleRoleId);
        }
    }

    public List<Map<String, Object>> selectAllocatedUsers(Map<String, String> params, Long roleId, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append("""
                 INNER JOIN sys_user_role ur ON u.user_id = ur.user_id AND ur.role_id = ?
                 WHERE u.is_deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(roleId);
        appendFilters(sql, args, params);
        sql.append(" ORDER BY u.user_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapUser(rs), args.toArray());
    }

    public long countAllocatedUsers(Map<String, String> params, Long roleId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM sys_user u
                INNER JOIN sys_user_role ur ON u.user_id = ur.user_id AND ur.role_id = ?
                WHERE u.is_deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(roleId);
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public List<Map<String, Object>> selectUnallocatedUsers(Map<String, String> params, Long roleId, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append("""
                 WHERE u.is_deleted = 0
                 AND u.user_id NOT IN (SELECT user_id FROM sys_user_role WHERE role_id = ?)
                """);
        List<Object> args = new ArrayList<>();
        args.add(roleId);
        appendFilters(sql, args, params);
        sql.append(" ORDER BY u.user_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapUser(rs), args.toArray());
    }

    public long countUnallocatedUsers(Map<String, String> params, Long roleId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM sys_user u
                WHERE u.is_deleted = 0
                AND u.user_id NOT IN (SELECT user_id FROM sys_user_role WHERE role_id = ?)
                """);
        List<Object> args = new ArrayList<>();
        args.add(roleId);
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        SysCompatHelper.like(sql, args, "u.username", params.get("userName"));
        SysCompatHelper.like(sql, args, "u.phone", params.get("phonenumber"));
        String status = params.get("status");
        if (StringUtils.hasText(status)) {
            sql.append(" AND u.status = ?");
            args.add(SysCompatHelper.fromApiStatusCode(status));
        }
        SysCompatHelper.eqLong(sql, args, "u.dept_id", params, "deptId");
        SysCompatHelper.dateRange(sql, args, "u.created_at", params, "beginTime", "endTime");
    }

    private Object resolveRoleIdsFromBody(Map<String, Object> body) {
        Object roleIds = body.get("roleIds");
        if (roleIds != null) {
            return roleIds;
        }
        return body.get("roleId");
    }

    @SuppressWarnings("unchecked")
    private void saveUserRoles(Long userId, Object roleIdsObj) {
        jdbc.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        List<Long> roleIds = parseRoleIds(roleIdsObj);
        for (Long roleId : roleIds) {
            jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?,?)", userId, roleId);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseRoleIds(Object roleIdsObj) {
        List<Long> ids = new ArrayList<>();
        if (roleIdsObj instanceof List<?> list) {
            for (Object o : list) {
                Long roleId = SysCompatHelper.longObj(o);
                if (roleId != null) {
                    ids.add(roleId);
                }
            }
        } else {
            Long roleId = SysCompatHelper.longObj(roleIdsObj);
            if (roleId != null) {
                ids.add(roleId);
            }
        }
        if (ids.size() > 1) {
            throw new IllegalArgumentException("每个用户只能绑定一个角色");
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private void saveUserPosts(Long userId, Object postIdsObj) {
        jdbc.update("DELETE FROM sys_user_post WHERE user_id = ?", userId);
        if (postIdsObj instanceof List<?> list) {
            for (Object o : list) {
                Long postId = SysCompatHelper.longObj(o);
                if (postId != null) {
                    jdbc.update("INSERT INTO sys_user_post (user_id, post_id) VALUES (?,?)", userId, postId);
                }
            }
        }
    }

    private Map<String, Object> mapUser(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", rs.getLong("user_id"));
        m.put("userName", rs.getString("username"));
        m.put("nickName", rs.getString("nick_name") != null ? rs.getString("nick_name") : rs.getString("real_name"));
        m.put("phonenumber", rs.getString("phone"));
        m.put("email", rs.getString("email"));
        m.put("sex", rs.getString("sex"));
        m.put("avatar", rs.getString("avatar"));
        m.put("remark", rs.getString("remark"));
        m.put("deptId", rs.getObject("dept_id"));
        m.put("employeeNo", rs.getString("employee_no"));
        m.put("dept", Map.of(
                "deptName", rs.getString("dept_name") == null ? "" : rs.getString("dept_name"),
                "deptCode", rs.getString("dept_code") == null ? "" : rs.getString("dept_code")));
        m.put("status", SysCompatHelper.toApiStatusCode(rs.getString("status")));
        m.put("delFlag", SysCompatHelper.delFlag(rs.getInt("is_deleted")));
        m.put("loginDate", SysCompatHelper.getTs(rs, "last_login_at"));
        m.put("createTime", SysCompatHelper.getTs(rs, "created_at"));
        return m;
    }
}
