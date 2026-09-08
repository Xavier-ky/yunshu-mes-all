package com.yunshu.mes.process.repository;

import com.yunshu.mes.process.vo.ProcessRouteStepVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessRouteStepRepository {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ProcessRouteStepRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ProcessRouteStepVO> findByRouteId(Long routeId) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(
            "SELECT prs.route_step_id, prs.route_id, prs.step_id, ps.step_code, ps.step_name, prs.step_seq, prs.station_type, prs.is_must_pass FROM process_route_step prs LEFT JOIN process_step ps ON prs.step_id = ps.step_id WHERE prs.route_id = ? ORDER BY prs.step_seq",
            (rs, n) -> new ProcessRouteStepVO(rs.getLong("route_step_id"), rs.getLong("route_id"), rs.getLong("step_id"),
                rs.getString("step_code"), rs.getString("step_name"),
                rs.getInt("step_seq"), rs.getString("station_type"), rs.getBoolean("is_must_pass")),
            routeId);
    }

    public Long insert(Long routeId, Long stepId, Integer stepSeq, String stationType, Boolean isMustPass) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO process_route_step (route_id, step_id, step_seq, station_type, is_must_pass) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, routeId); ps.setLong(2, stepId); ps.setInt(3, stepSeq);
            ps.setString(4, stationType); ps.setBoolean(5, isMustPass);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, Integer stepSeq, String stationType, Boolean isMustPass) {
        requireJdbc().update("UPDATE process_route_step SET step_seq=?, station_type=?, is_must_pass=? WHERE route_step_id=?",
            stepSeq, stationType, isMustPass, id);
    }

    public void delete(Long id) {
        requireJdbc().update("DELETE FROM process_route_step WHERE route_step_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) throw new DataAccessResourceFailureException("JdbcTemplate 未配置");
        return jdbc;
    }
}
