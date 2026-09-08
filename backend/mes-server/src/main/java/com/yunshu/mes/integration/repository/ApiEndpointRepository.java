package com.yunshu.mes.integration.repository;

import com.yunshu.mes.integration.vo.ApiEndpointVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ApiEndpointRepository {

    private static final String SELECT_LIST = """
            SELECT endpoint_id, endpoint_code, endpoint_name, api_path, http_method, direction, status
            FROM api_endpoint ORDER BY endpoint_id
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ApiEndpointRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ApiEndpointVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new ApiEndpointVO(
                rs.getLong("endpoint_id"),
                rs.getString("endpoint_code"),
                rs.getString("endpoint_name"),
                rs.getString("api_path"),
                rs.getString("http_method"),
                rs.getString("direction"),
                rs.getString("status")));
    }

    public List<ApiEndpointVO> search(Map<String, String> params, int offset, int limit) {
        JdbcTemplate jdbc = requireJdbc();
        StringBuilder sql = new StringBuilder("""
                SELECT endpoint_id, endpoint_code, endpoint_name, api_path, http_method, direction, status
                FROM api_endpoint WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("endpointName"))) {
            sql.append(" AND endpoint_name LIKE ?");
            args.add("%" + params.get("endpointName").trim() + "%");
        }
        if (StringUtils.hasText(params.get("apiPath"))) {
            sql.append(" AND api_path LIKE ?");
            args.add("%" + params.get("apiPath").trim() + "%");
        }
        if (StringUtils.hasText(params.get("externalSystemId"))) {
            sql.append(" AND external_system_id = ?");
            args.add(Long.parseLong(params.get("externalSystemId").trim()));
        }
        sql.append(" ORDER BY endpoint_id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> new ApiEndpointVO(
                rs.getLong("endpoint_id"),
                rs.getString("endpoint_code"),
                rs.getString("endpoint_name"),
                rs.getString("api_path"),
                rs.getString("http_method"),
                rs.getString("direction"),
                rs.getString("status")), args.toArray());
    }

    public long count(Map<String, String> params) {
        JdbcTemplate jdbc = requireJdbc();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM api_endpoint WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("endpointName"))) {
            sql.append(" AND endpoint_name LIKE ?");
            args.add("%" + params.get("endpointName").trim() + "%");
        }
        if (StringUtils.hasText(params.get("apiPath"))) {
            sql.append(" AND api_path LIKE ?");
            args.add("%" + params.get("apiPath").trim() + "%");
        }
        if (StringUtils.hasText(params.get("externalSystemId"))) {
            sql.append(" AND external_system_id = ?");
            args.add(Long.parseLong(params.get("externalSystemId").trim()));
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<ApiEndpointVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<ApiEndpointVO> list = jdbc.query(
                SELECT_LIST.replace("ORDER BY endpoint_id", "") + " WHERE endpoint_id = ?",
                (rs, n) -> new ApiEndpointVO(
                        rs.getLong("endpoint_id"),
                        rs.getString("endpoint_code"),
                        rs.getString("endpoint_name"),
                        rs.getString("api_path"),
                        rs.getString("http_method"),
                        rs.getString("direction"),
                        rs.getString("status")),
                id);
        return list.stream().findFirst();
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置");
        }
        return jdbc;
    }
}
