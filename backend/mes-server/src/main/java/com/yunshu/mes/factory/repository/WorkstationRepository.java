package com.yunshu.mes.factory.repository;

import com.yunshu.mes.factory.vo.WorkstationVO;
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
 * 工位数据访问。
 */
@Repository
public class WorkstationRepository {

    private static final String SELECT_LIST = """
            SELECT s.station_id, s.station_code, s.station_name, s.station_type,
                   s.line_id, l.line_name, s.status
            FROM workstation s
            LEFT JOIN production_line l ON s.line_id = l.line_id
            ORDER BY s.station_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT s.station_id, s.station_code, s.station_name, s.station_type,
                   s.line_id, l.line_name, s.status
            FROM workstation s
            LEFT JOIN production_line l ON s.line_id = l.line_id
            WHERE s.station_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public WorkstationRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<WorkstationVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new WorkstationVO(
                rs.getLong("station_id"),
                rs.getString("station_code"),
                rs.getString("station_name"),
                rs.getString("station_type"),
                rs.getLong("line_id"),
                rs.getString("line_name"),
                rs.getString("status")));
    }

    public Optional<WorkstationVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<WorkstationVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new WorkstationVO(
                rs.getLong("station_id"),
                rs.getString("station_code"),
                rs.getString("station_name"),
                rs.getString("station_type"),
                rs.getLong("line_id"),
                rs.getString("line_name"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(Long lineId, String stationCode, String stationName, String stationType, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO workstation (line_id, station_code, station_name, station_type, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, lineId);
            ps.setString(2, stationCode);
            ps.setString(3, stationName);
            ps.setString(4, stationType);
            ps.setString(5, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, Long lineId, String stationCode, String stationName, String stationType, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                UPDATE workstation
                SET line_id = ?, station_code = ?, station_name = ?, station_type = ?, status = ?
                WHERE station_id = ?
                """, lineId, stationCode, stationName, stationType, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM workstation WHERE station_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
