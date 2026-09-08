package com.yunshu.mes.andon.repository;

import com.yunshu.mes.andon.vo.AndonReasonVO;
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
 * 安灯原因数据访问。
 */
@Repository
public class AndonReasonRepository {

    private static final String SELECT_LIST = """
            SELECT reason_id, reason_code, reason_name, reason_category, status
            FROM andon_reason ORDER BY reason_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT reason_id, reason_code, reason_name, reason_category, status
            FROM andon_reason WHERE reason_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public AndonReasonRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<AndonReasonVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new AndonReasonVO(
                rs.getLong("reason_id"),
                rs.getString("reason_code"),
                rs.getString("reason_name"),
                rs.getString("reason_category"),
                rs.getString("status")));
    }

    public Optional<AndonReasonVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<AndonReasonVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new AndonReasonVO(
                rs.getLong("reason_id"),
                rs.getString("reason_code"),
                rs.getString("reason_name"),
                rs.getString("reason_category"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String reasonCode, String reasonName, String reasonCategory, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO andon_reason (reason_code, reason_name, reason_category, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, reasonCode);
            ps.setString(2, reasonName);
            ps.setString(3, reasonCategory == null ? "GENERAL" : reasonCategory);
            ps.setString(4, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String reasonCode, String reasonName, String reasonCategory, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                UPDATE andon_reason SET reason_code = ?, reason_name = ?, reason_category = ?, status = ?
                WHERE reason_id = ?
                """, reasonCode, reasonName, reasonCategory, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM andon_reason WHERE reason_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
