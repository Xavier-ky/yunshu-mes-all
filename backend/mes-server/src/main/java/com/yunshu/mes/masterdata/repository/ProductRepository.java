package com.yunshu.mes.masterdata.repository;

import com.yunshu.mes.masterdata.vo.ProductVO;
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
 * 产品数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class ProductRepository {

    private static final String SELECT_LIST = """
            SELECT product_id, product_code, product_name, product_model, product_category, status
            FROM product ORDER BY product_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT product_id, product_code, product_name, product_model, product_category, status
            FROM product WHERE product_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ProductRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ProductVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new ProductVO(
                rs.getLong("product_id"),
                rs.getString("product_code"),
                rs.getString("product_name"),
                rs.getString("product_model"),
                rs.getString("product_category"),
                rs.getString("status")));
    }

    public Optional<ProductVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<ProductVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new ProductVO(
                rs.getLong("product_id"),
                rs.getString("product_code"),
                rs.getString("product_name"),
                rs.getString("product_model"),
                rs.getString("product_category"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String productCode, String productName, String productModel, String category, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product (product_code, product_name, product_model, product_category, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, productCode);
            ps.setString(2, productName);
            ps.setString(3, productModel);
            ps.setString(4, category);
            ps.setString(5, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String productCode, String productName, String productModel, String category, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE product SET product_code = ?, product_name = ?, product_model = ?, product_category = ?, status = ? WHERE product_id = ?",
                productCode, productName, productModel, category, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM product WHERE product_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
