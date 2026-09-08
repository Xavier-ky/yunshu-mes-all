package com.yunshu.mes.system.repository;

import com.yunshu.mes.system.dto.AuthUser;
import com.yunshu.mes.system.vo.UserVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 系统用户查询与写操作，基于 JdbcTemplate。JdbcTemplate 不可用时抛
 * {@link DataAccessResourceFailureException}，由上层 facade 回退到 Mock。
 */
@Repository
public class UserRepository {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public UserRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<UserVO> findAllWithRoles(String keyword, String status) {
        JdbcTemplate jdbc = requireJdbc();
        StringBuilder sql = new StringBuilder("""
                SELECT u.user_id, u.username, u.real_name, u.employee_no, d.dept_name, u.status,
                       r.role_code, r.role_name
                FROM sys_user u
                LEFT JOIN sys_department d ON u.dept_id = d.dept_id
                LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
                LEFT JOIN sys_role r ON ur.role_id = r.role_id
                WHERE u.is_deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (u.username LIKE ? OR u.real_name LIKE ? OR u.employee_no LIKE ?)");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND u.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY u.user_id, r.role_id");
        Map<Long, UserAgg> agg = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(sql.toString(), args.toArray())) {
            Long id = longValue(row.get("user_id"));
            UserAgg u = agg.computeIfAbsent(id, k -> new UserAgg(
                    id,
                    stringValue(row.get("username")),
                    stringValue(row.get("real_name")),
                    stringValue(row.get("employee_no")),
                    stringValue(row.get("dept_name")),
                    stringValue(row.get("status"))));
            String roleCode = stringValue(row.get("role_code"));
            String roleName = stringValue(row.get("role_name"));
            if (roleCode != null) {
                u.roleCodes.add(roleCode);
            }
            if (roleName != null) {
                u.roleNames.add(roleName);
            }
        }
        return agg.values().stream().map(UserAgg::toVo).toList();
    }

    public Optional<UserVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        String sql = """
                SELECT u.user_id, u.username, u.real_name, u.employee_no, d.dept_name, u.status,
                       r.role_code, r.role_name
                FROM sys_user u
                LEFT JOIN sys_department d ON u.dept_id = d.dept_id
                LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
                LEFT JOIN sys_role r ON ur.role_id = r.role_id
                WHERE u.user_id = ? AND u.is_deleted = 0
                """;
        Map<Long, UserAgg> agg = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(sql, id)) {
            Long uid = longValue(row.get("user_id"));
            UserAgg u = agg.computeIfAbsent(uid, k -> new UserAgg(
                    uid,
                    stringValue(row.get("username")),
                    stringValue(row.get("real_name")),
                    stringValue(row.get("employee_no")),
                    stringValue(row.get("dept_name")),
                    stringValue(row.get("status"))));
            String roleCode = stringValue(row.get("role_code"));
            String roleName = stringValue(row.get("role_name"));
            if (roleCode != null) {
                u.roleCodes.add(roleCode);
            }
            if (roleName != null) {
                u.roleNames.add(roleName);
            }
        }
        return agg.values().stream().findFirst().map(UserAgg::toVo);
    }

    public Optional<AuthUser> findAuthUserByUsername(String username) {
        JdbcTemplate jdbc = requireJdbc();
        String sql = """
                SELECT u.user_id, u.username, u.password_hash, u.real_name, d.dept_name, u.status,
                       r.role_code, r.role_name
                FROM sys_user u
                LEFT JOIN sys_department d ON u.dept_id = d.dept_id
                LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
                LEFT JOIN sys_role r ON ur.role_id = r.role_id
                WHERE u.username = ? AND u.is_deleted = 0
                """;
        Map<Long, AuthAgg> agg = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(sql, username)) {
            Long id = longValue(row.get("user_id"));
            AuthAgg u = agg.computeIfAbsent(id, k -> new AuthAgg(
                    id,
                    stringValue(row.get("username")),
                    stringValue(row.get("password_hash")),
                    stringValue(row.get("real_name")),
                    stringValue(row.get("dept_name")),
                    stringValue(row.get("status"))));
            String roleCode = stringValue(row.get("role_code"));
            String roleName = stringValue(row.get("role_name"));
            if (roleCode != null) {
                u.roleCodes.add(roleCode);
            }
            if (roleName != null) {
                u.roleNames.add(roleName);
            }
        }
        return agg.values().stream().findFirst().map(AuthAgg::toAuthUser);
    }

    public Long insert(String username, String passwordHash, String employeeNo, String realName, Long deptId, String status) {
        JdbcTemplate jdbc = requireJdbc();
        String sql = "INSERT INTO sys_user (username, password_hash, employee_no, real_name, dept_id, status, is_deleted, version) "
                + "VALUES (?, ?, ?, ?, ?, ?, 0, 0)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, employeeNo);
            ps.setString(4, realName);
            if (deptId == null) {
                ps.setNull(5, Types.BIGINT);
            } else {
                ps.setLong(5, deptId);
            }
            ps.setString(6, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String username, String realName, String employeeNo, Long deptId, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE sys_user SET username = ?, real_name = ?, employee_no = ?, dept_id = ?, status = ? WHERE user_id = ?",
                username, realName, employeeNo, deptId, status, id);
    }

    public void softDelete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE sys_user SET is_deleted = 1 WHERE user_id = ?", id);
    }

    public void syncRoles(Long userId, List<String> roleCodes) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        if (roleCodes.size() > 1) {
            throw new IllegalArgumentException("每个用户只能绑定一个角色");
        }
        String code = roleCodes.get(0);
        Long roleId = findRoleIdByCode(code);
        if (roleId != null) {
            jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        }
    }

    private Long findRoleIdByCode(String roleCode) {
        JdbcTemplate jdbc = requireJdbc();
        var list = jdbc.query("SELECT role_id FROM sys_role WHERE role_code = ?",
            (rs, n) -> rs.getLong("role_id"), roleCode);
        return list.isEmpty() ? null : list.get(0);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static final class UserAgg {
        final Long userId;
        final String username;
        final String realName;
        final String employeeNo;
        final String deptName;
        final String status;
        final List<String> roleCodes = new ArrayList<>();
        final List<String> roleNames = new ArrayList<>();

        UserAgg(Long userId, String username, String realName, String employeeNo, String deptName, String status) {
            this.userId = userId;
            this.username = username;
            this.realName = realName;
            this.employeeNo = employeeNo;
            this.deptName = deptName;
            this.status = status;
        }

        UserVO toVo() {
            return new UserVO(userId, username, realName, employeeNo, deptName, status,
                    List.copyOf(roleCodes), List.copyOf(roleNames));
        }
    }

    private static final class AuthAgg {
        final Long userId;
        final String username;
        final String passwordHash;
        final String realName;
        final String deptName;
        final String status;
        final List<String> roleCodes = new ArrayList<>();
        final List<String> roleNames = new ArrayList<>();

        AuthAgg(Long userId, String username, String passwordHash, String realName, String deptName, String status) {
            this.userId = userId;
            this.username = username;
            this.passwordHash = passwordHash;
            this.realName = realName;
            this.deptName = deptName;
            this.status = status;
        }

        AuthUser toAuthUser() {
            return new AuthUser(userId, username, passwordHash, realName, deptName, status,
                    List.copyOf(roleCodes), List.copyOf(roleNames));
        }
    }
}
