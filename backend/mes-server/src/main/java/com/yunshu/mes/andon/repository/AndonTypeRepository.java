package com.yunshu.mes.andon.repository;

import com.yunshu.mes.andon.vo.AndonTypeVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 安灯类型数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class AndonTypeRepository {

    private static final String SELECT_LIST = """
            SELECT andon_type_id AS type_id, type_code, type_name, status
            FROM andon_type ORDER BY andon_type_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT andon_type_id AS type_id, type_code, type_name, status
            FROM andon_type WHERE andon_type_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public AndonTypeRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<AndonTypeVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new AndonTypeVO(
                rs.getLong("type_id"),
                rs.getString("type_code"),
                rs.getString("type_name"),
                rs.getString("status")));
    }

    public Optional<AndonTypeVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<AndonTypeVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new AndonTypeVO(
                rs.getLong("type_id"),
                rs.getString("type_code"),
                rs.getString("type_name"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String typeCode, String typeName, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO andon_type (type_code, type_name, handle_mode, priority, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, typeCode);
            ps.setString(2, typeName);
            ps.setString(3, "ASSIST_HANDLE");
            ps.setString(4, "NORMAL");
            ps.setString(5, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String typeCode, String typeName, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE andon_type SET type_code = ?, type_name = ?, status = ? WHERE andon_type_id = ?",
                typeCode, typeName, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM andon_type WHERE andon_type_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
