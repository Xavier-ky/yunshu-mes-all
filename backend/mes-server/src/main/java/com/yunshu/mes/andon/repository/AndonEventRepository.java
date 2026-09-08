package com.yunshu.mes.andon.repository;

import com.yunshu.mes.andon.vo.AndonEventVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AndonEventRepository {

    private static final String SELECT_LIST = """
            SELECT a.andon_id, a.andon_no, t.type_name, l.line_name,
                   a.exception_desc, a.status, a.occur_time
            FROM andon_event a
            LEFT JOIN andon_type t ON a.andon_type_id = t.andon_type_id
            LEFT JOIN production_line l ON a.line_id = l.line_id
            ORDER BY a.andon_id DESC
            """;
    private static final String SELECT_BY_ID = """
            SELECT a.andon_id, a.andon_no, t.type_name, l.line_name,
                   a.exception_desc, a.status, a.occur_time
            FROM andon_event a
            LEFT JOIN andon_type t ON a.andon_type_id = t.andon_type_id
            LEFT JOIN production_line l ON a.line_id = l.line_id
            WHERE a.andon_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public AndonEventRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<AndonEventVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new AndonEventVO(
                rs.getLong("andon_id"), rs.getString("andon_no"),
                rs.getString("type_name"), rs.getString("line_name"),
                rs.getString("exception_desc"), rs.getString("status"),
                rs.getString("occur_time")));
    }

    public Optional<AndonEventVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<AndonEventVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new AndonEventVO(
                rs.getLong("andon_id"), rs.getString("andon_no"),
                rs.getString("type_name"), rs.getString("line_name"),
                rs.getString("exception_desc"), rs.getString("status"),
                rs.getString("occur_time")), id);
        return list.stream().findFirst();
    }

    public Long insert(String andonNo, Long andonTypeId, Long reasonId, Long lineId, Long stationId, Long reportUserId, String exceptionDesc) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO andon_event (andon_no, andon_type_id, reason_id, line_id, station_id, report_user_id, exception_desc, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, andonNo); ps.setObject(2, andonTypeId); ps.setObject(3, reasonId);
            ps.setObject(4, lineId); ps.setObject(5, stationId); ps.setObject(6, reportUserId);
            ps.setString(7, exceptionDesc);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void updateStatus(Long id, String status) {
        JdbcTemplate jdbc = requireJdbc();
        if ("CLOSED".equals(status)) {
            jdbc.update("UPDATE andon_event SET status = ?, close_time = ? WHERE andon_id = ?", status, Timestamp.valueOf(LocalDateTime.now()), id);
        } else {
            jdbc.update("UPDATE andon_event SET status = ? WHERE andon_id = ?", status, id);
        }
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM andon_event WHERE andon_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        return jdbc;
    }
}
