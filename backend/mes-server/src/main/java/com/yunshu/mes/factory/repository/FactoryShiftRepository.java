package com.yunshu.mes.factory.repository;

import com.yunshu.mes.factory.vo.ShiftVO;
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
 * 班次数据访问。
 */
@Repository
public class FactoryShiftRepository {

    private static final String SELECT_LIST = """
            SELECT shift_id, shift_code, shift_name,
                   TIME_FORMAT(start_time, '%H:%i') AS start_time,
                   TIME_FORMAT(end_time, '%H:%i') AS end_time,
                   status
            FROM factory_shift ORDER BY shift_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT shift_id, shift_code, shift_name,
                   TIME_FORMAT(start_time, '%H:%i') AS start_time,
                   TIME_FORMAT(end_time, '%H:%i') AS end_time,
                   status
            FROM factory_shift WHERE shift_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public FactoryShiftRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ShiftVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new ShiftVO(
                rs.getLong("shift_id"),
                rs.getString("shift_code"),
                rs.getString("shift_name"),
                rs.getString("start_time"),
                rs.getString("end_time"),
                rs.getString("status")));
    }

    public Optional<ShiftVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<ShiftVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new ShiftVO(
                rs.getLong("shift_id"),
                rs.getString("shift_code"),
                rs.getString("shift_name"),
                rs.getString("start_time"),
                rs.getString("end_time"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String shiftCode, String shiftName, String startTime, String endTime, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO factory_shift (shift_code, shift_name, start_time, end_time, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, shiftCode);
            ps.setString(2, shiftName);
            ps.setString(3, startTime);
            ps.setString(4, endTime);
            ps.setString(5, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String shiftCode, String shiftName, String startTime, String endTime, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                UPDATE factory_shift
                SET shift_code = ?, shift_name = ?, start_time = ?, end_time = ?, status = ?
                WHERE shift_id = ?
                """, shiftCode, shiftName, startTime, endTime, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM factory_shift WHERE shift_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
