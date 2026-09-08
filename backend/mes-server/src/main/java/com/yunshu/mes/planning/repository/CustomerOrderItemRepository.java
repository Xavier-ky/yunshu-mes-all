package com.yunshu.mes.planning.repository;

import com.yunshu.mes.planning.vo.CustomerOrderItemVO;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerOrderItemRepository {

    private static final String SELECT_BY_ORDER = """
            SELECT oi.order_item_id, oi.order_id, oi.product_id, p.product_code, p.product_name,
                   oi.order_qty, oi.technical_requirement
            FROM customer_order_item oi
            JOIN product p ON oi.product_id = p.product_id
            WHERE oi.order_id = ?
            ORDER BY oi.order_item_id
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public CustomerOrderItemRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<CustomerOrderItemVO> findByOrderId(Long orderId) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_BY_ORDER, (rs, n) -> new CustomerOrderItemVO(
                rs.getLong("order_item_id"),
                rs.getLong("order_id"),
                rs.getLong("product_id"),
                rs.getString("product_code"),
                rs.getString("product_name"),
                rs.getLong("order_qty"),
                rs.getString("technical_requirement")), orderId);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
