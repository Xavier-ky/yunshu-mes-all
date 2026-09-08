package com.yunshu.mes.masterdata.repository;

import com.yunshu.mes.masterdata.vo.BomVO;
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
 * BOM数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class BomRepository {

    private static final String SELECT_LIST = """
            SELECT b.bom_id, b.bom_code, b.bom_name, p.product_name, b.version_no, b.status
            FROM bom b
            LEFT JOIN product p ON b.product_id = p.product_id
            ORDER BY b.bom_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT b.bom_id, b.bom_code, b.bom_name, p.product_name, b.version_no, b.status
            FROM bom b
            LEFT JOIN product p ON b.product_id = p.product_id
            WHERE b.bom_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public BomRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<BomVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new BomVO(
                rs.getLong("bom_id"),
                rs.getString("bom_code"),
                rs.getString("bom_name"),
                rs.getString("product_name"),
                rs.getString("version_no"),
                rs.getString("status")));
    }

    public Optional<BomVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<BomVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new BomVO(
                rs.getLong("bom_id"),
                rs.getString("bom_code"),
                rs.getString("bom_name"),
                rs.getString("product_name"),
                rs.getString("version_no"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String bomCode, String bomName, Long productId, String bomVersion, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO bom (bom_code, bom_name, product_id, version_no, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, bomCode);
            ps.setString(2, bomName);
            ps.setLong(3, productId);
            ps.setString(4, bomVersion);
            ps.setString(5, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String bomCode, String bomName, Long productId, String bomVersion, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE bom SET bom_code = ?, bom_name = ?, product_id = ?, version_no = ?, status = ? WHERE bom_id = ?",
                bomCode, bomName, productId, bomVersion, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM bom WHERE bom_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
