package com.yunshu.mes.planning.compat.repository;

import com.yunshu.mes.masterdata.vo.ProductVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClientCatalogRepository {

    private static final String SELECT_PRODUCTS = """
            SELECT product_id, product_code, product_name, product_model, product_category, status
            FROM product
            WHERE status = 'ENABLED' AND (is_deleted = 0 OR is_deleted IS NULL)
            ORDER BY product_id
            """;

    private static final String SELECT_SPECS = """
            SELECT ps.product_id, ps.spec_name, ps.spec_value
            FROM product_spec ps
            JOIN product p ON p.product_id = ps.product_id
            WHERE p.status = 'ENABLED' AND (p.is_deleted = 0 OR p.is_deleted IS NULL)
            ORDER BY ps.product_id, ps.spec_id
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ClientCatalogRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ProductVO> findEnabledProducts() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_PRODUCTS, (rs, n) -> new ProductVO(
                rs.getLong("product_id"),
                rs.getString("product_code"),
                rs.getString("product_name"),
                rs.getString("product_model"),
                rs.getString("product_category"),
                rs.getString("status")));
    }

    public Map<Long, List<String>> findSpecsByProduct() {
        JdbcTemplate jdbc = requireJdbc();
        Map<Long, List<String>> map = new LinkedHashMap<>();
        jdbc.query(SELECT_SPECS, rs -> {
            long productId = rs.getLong("product_id");
            String line = rs.getString("spec_name") + "：" + rs.getString("spec_value");
            map.computeIfAbsent(productId, k -> new ArrayList<>()).add(line);
        });
        return map;
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置");
        }
        return jdbc;
    }
}
