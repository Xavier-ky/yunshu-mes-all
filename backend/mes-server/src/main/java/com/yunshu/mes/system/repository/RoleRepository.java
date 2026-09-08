package com.yunshu.mes.system.repository;

import com.yunshu.mes.system.vo.RoleVO;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 系统角色查询，基于 JdbcTemplate。JdbcTemplate 不可用时抛
 * {@link DataAccessResourceFailureException}，由上层 facade 回退到 Mock。
 */
@Repository
public class RoleRepository {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public RoleRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<RoleVO> findAllRoles() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        String sql = "SELECT role_id, role_code, role_name, role_desc, status FROM sys_role ORDER BY role_id";
        return jdbc.query(sql, (rs, rowNum) -> new RoleVO(
                rs.getLong("role_id"),
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getString("role_desc"),
                rs.getString("status")
        ));
    }
}
