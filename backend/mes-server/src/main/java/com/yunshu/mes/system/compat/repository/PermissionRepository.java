package com.yunshu.mes.system.compat.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PermissionRepository {

    private static final String PERMS_BY_USER = """
            SELECT DISTINCT m.perms
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = ?
              AND m.perms IS NOT NULL
              AND m.perms <> ''
            ORDER BY m.perms
            """;

    private static final String PERMS_BY_ROLE = """
            SELECT DISTINCT m.perms
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
            WHERE rm.role_id = ?
              AND m.perms IS NOT NULL
              AND m.perms <> ''
            ORDER BY m.perms
            """;

    private final JdbcTemplate jdbc;

    public PermissionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> findPermsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return jdbc.query(PERMS_BY_USER, (rs, n) -> rs.getString(1), userId);
    }

    public List<String> findPermsByRoleId(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return jdbc.query(PERMS_BY_ROLE, (rs, n) -> rs.getString(1), roleId);
    }

    public int countRolesForUser(Long userId) {
        if (userId == null) {
            return 0;
        }
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?", Integer.class, userId);
        return c == null ? 0 : c;
    }

    public List<String> findRoleCodesForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return jdbc.query("""
                SELECT r.role_code
                FROM sys_role r
                INNER JOIN sys_user_role ur ON ur.role_id = r.role_id
                WHERE ur.user_id = ?
                ORDER BY r.role_id
                """, (rs, n) -> rs.getString(1), userId);
    }

    public static List<String> mergeDistinct(List<String>... lists) {
        Set<String> merged = new LinkedHashSet<>();
        if (lists != null) {
            for (List<String> list : lists) {
                if (list == null) {
                    continue;
                }
                for (String item : list) {
                    if (item != null && !item.isBlank()) {
                        merged.add(item.trim());
                    }
                }
            }
        }
        return new ArrayList<>(merged);
    }
}
