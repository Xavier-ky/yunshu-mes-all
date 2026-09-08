package com.yunshu.mes.masterdata.repository;

import com.yunshu.mes.masterdata.vo.UomVO;
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
 * 计量单位数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class UomRepository {

    private static final String SELECT_LIST = """
            SELECT unit_id, unit_code, unit_name, status
            FROM uom ORDER BY unit_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT unit_id, unit_code, unit_name, status
            FROM uom WHERE unit_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public UomRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<UomVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new UomVO(
                rs.getLong("unit_id"),
                rs.getString("unit_code"),
                rs.getString("unit_name"),
                rs.getString("status")));
    }

    public Optional<UomVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<UomVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new UomVO(
                rs.getLong("unit_id"),
                rs.getString("unit_code"),
                rs.getString("unit_name"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String unitCode, String unitName, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO uom (unit_code, unit_name, status) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, unitCode);
            ps.setString(2, unitName);
            ps.setString(3, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String unitCode, String unitName, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE uom SET unit_code = ?, unit_name = ?, status = ? WHERE unit_id = ?",
                unitCode, unitName, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM uom WHERE unit_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
