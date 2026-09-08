package com.yunshu.mes.process.repository;

import com.yunshu.mes.process.vo.ProcessRouteVO;
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
 * 工艺路线数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class RouteRepository {

    private static final String SELECT_LIST = """
            SELECT route_id, route_code, route_name, version_no, status
            FROM process_route ORDER BY route_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT route_id, route_code, route_name, version_no, status
            FROM process_route WHERE route_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public RouteRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ProcessRouteVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new ProcessRouteVO(
                rs.getLong("route_id"),
                rs.getString("route_code"),
                rs.getString("route_name"),
                rs.getString("version_no"),
                rs.getString("status")));
    }

    public Optional<ProcessRouteVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<ProcessRouteVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new ProcessRouteVO(
                rs.getLong("route_id"),
                rs.getString("route_code"),
                rs.getString("route_name"),
                rs.getString("version_no"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String routeCode, String routeName, String routeVersion, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO process_route (route_code, route_name, version_no, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, routeCode);
            ps.setString(2, routeName);
            ps.setString(3, routeVersion);
            ps.setString(4, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String routeCode, String routeName, String routeVersion, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE process_route SET route_code = ?, route_name = ?, version_no = ?, status = ? WHERE route_id = ?",
                routeCode, routeName, routeVersion, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM process_route WHERE route_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
