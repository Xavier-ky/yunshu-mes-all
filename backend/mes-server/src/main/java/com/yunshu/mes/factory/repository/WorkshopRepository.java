package com.yunshu.mes.factory.repository;

import com.yunshu.mes.factory.vo.WorkshopVO;
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
 * 车间数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class WorkshopRepository {

    private static final String SELECT_LIST = """
            SELECT workshop_id, workshop_code, workshop_name, status
            FROM workshop ORDER BY workshop_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT workshop_id, workshop_code, workshop_name, status
            FROM workshop WHERE workshop_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public WorkshopRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<WorkshopVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new WorkshopVO(
                rs.getLong("workshop_id"),
                rs.getString("workshop_code"),
                rs.getString("workshop_name"),
                rs.getString("status")));
    }

    public Optional<WorkshopVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<WorkshopVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new WorkshopVO(
                rs.getLong("workshop_id"),
                rs.getString("workshop_code"),
                rs.getString("workshop_name"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String workshopCode, String workshopName, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO workshop (workshop_code, workshop_name, status) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, workshopCode);
            ps.setString(2, workshopName);
            ps.setString(3, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String workshopCode, String workshopName, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE workshop SET workshop_code = ?, workshop_name = ?, status = ? WHERE workshop_id = ?",
                workshopCode, workshopName, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM workshop WHERE workshop_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
