package com.yunshu.mes.production.repository;

import com.yunshu.mes.production.vo.ProductSnVO;
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
 * 产品序列号数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class ProductSnRepository {

    private static final String SELECT_LIST = """
            SELECT s.sn_id, s.sn_code, p.product_name, w.work_order_no, s.status
            FROM product_sn s
            LEFT JOIN product p ON s.product_id = p.product_id
            LEFT JOIN work_order w ON s.work_order_id = w.work_order_id
            ORDER BY s.sn_id DESC
            """;
    private static final String SELECT_BY_ID = """
            SELECT s.sn_id, s.sn_code, p.product_name, w.work_order_no, s.status
            FROM product_sn s
            LEFT JOIN product p ON s.product_id = p.product_id
            LEFT JOIN work_order w ON s.work_order_id = w.work_order_id
            WHERE s.sn_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ProductSnRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ProductSnVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new ProductSnVO(
                rs.getLong("sn_id"),
                rs.getString("sn_code"),
                rs.getString("product_name"),
                rs.getString("work_order_no"),
                rs.getString("status")));
    }

    public Optional<ProductSnVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<ProductSnVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new ProductSnVO(
                rs.getLong("sn_id"),
                rs.getString("sn_code"),
                rs.getString("product_name"),
                rs.getString("work_order_no"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String snCode, Long productId, Long workOrderId, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_sn (sn_code, product_id, work_order_id, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, snCode);
            ps.setObject(2, productId);
            ps.setObject(3, workOrderId);
            ps.setString(4, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String snCode, Long productId, Long workOrderId, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE product_sn SET sn_code = ?, product_id = ?, work_order_id = ?, status = ? WHERE sn_id = ?",
                snCode, productId, workOrderId, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM product_sn WHERE sn_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
