package com.yunshu.mes.integration.repository;

import com.yunshu.mes.integration.vo.ExternalSystemVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 外部系统数据访问 — JdbcTemplate。
 */
@Repository
public class ExternalSystemRepository {

    private static final String SELECT_LIST = """
            SELECT external_system_id, system_code, system_name, system_type, status
            FROM external_system ORDER BY external_system_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT external_system_id, system_code, system_name, system_type, status
            FROM external_system WHERE external_system_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ExternalSystemRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ExternalSystemVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, this::mapRow);
    }

    public List<ExternalSystemVO> search(Map<String, String> params, int offset, int limit) {
        JdbcTemplate jdbc = requireJdbc();
        StringBuilder sql = new StringBuilder("""
                SELECT external_system_id, system_code, system_name, system_type, status
                FROM external_system WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("systemCode"))) {
            sql.append(" AND system_code LIKE ?");
            args.add("%" + params.get("systemCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("systemName"))) {
            sql.append(" AND system_name LIKE ?");
            args.add("%" + params.get("systemName").trim() + "%");
        }
        sql.append(" ORDER BY external_system_id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), this::mapRow, args.toArray());
    }

    public long count(Map<String, String> params) {
        JdbcTemplate jdbc = requireJdbc();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM external_system WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("systemCode"))) {
            sql.append(" AND system_code LIKE ?");
            args.add("%" + params.get("systemCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("systemName"))) {
            sql.append(" AND system_name LIKE ?");
            args.add("%" + params.get("systemName").trim() + "%");
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<ExternalSystemVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<ExternalSystemVO> list = jdbc.query(SELECT_BY_ID, this::mapRow, id);
        return list.stream().findFirst();
    }

    public Long insert(String systemCode, String systemName, String systemType, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO external_system (system_code, system_name, system_type, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, systemCode);
            ps.setString(2, systemName);
            ps.setString(3, systemType);
            ps.setString(4, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String systemCode, String systemName, String systemType, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                UPDATE external_system SET system_code = ?, system_name = ?, system_type = ?, status = ?
                WHERE external_system_id = ?
                """, systemCode, systemName, systemType, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM external_system WHERE external_system_id = ?", id);
    }

    private ExternalSystemVO mapRow(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new ExternalSystemVO(
                rs.getLong("external_system_id"),
                rs.getString("system_code"),
                rs.getString("system_name"),
                rs.getString("system_type"),
                rs.getString("status"));
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
