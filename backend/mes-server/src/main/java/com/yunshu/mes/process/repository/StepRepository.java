package com.yunshu.mes.process.repository;

import com.yunshu.mes.process.vo.ProcessStepVO;
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
 * 工序数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class StepRepository {

    private static final String SELECT_LIST = """
            SELECT step_id, step_code, step_name, step_type, standard_time_sec, status
            FROM process_step ORDER BY step_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT step_id, step_code, step_name, step_type, standard_time_sec, status
            FROM process_step WHERE step_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public StepRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ProcessStepVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new ProcessStepVO(
                rs.getLong("step_id"),
                rs.getString("step_code"),
                rs.getString("step_name"),
                rs.getString("step_type"),
                rs.getString("standard_time_sec") != null ? String.valueOf(rs.getInt("standard_time_sec")) : null,
                rs.getString("status")));
    }

    public Optional<ProcessStepVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<ProcessStepVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new ProcessStepVO(
                rs.getLong("step_id"),
                rs.getString("step_code"),
                rs.getString("step_name"),
                rs.getString("step_type"),
                rs.getString("standard_time_sec") != null ? String.valueOf(rs.getInt("standard_time_sec")) : null,
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String stepCode, String stepName, String stepType, String standardHours, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO process_step (step_code, step_name, step_type, standard_time_sec, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, stepCode);
            ps.setString(2, stepName);
            ps.setString(3, stepType);
            ps.setString(4, standardHours);
            ps.setString(5, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String stepCode, String stepName, String stepType, String standardHours, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE process_step SET step_code = ?, step_name = ?, step_type = ?, standard_time_sec = ?, status = ? WHERE step_id = ?",
                stepCode, stepName, stepType, standardHours, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM process_step WHERE step_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
