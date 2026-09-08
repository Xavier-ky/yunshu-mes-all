package com.yunshu.mes.masterdata.repository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRouteRepository {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ProductRouteRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public void bind(Long productId, Long routeId) {
        requireJdbc().update("INSERT INTO product_route (product_id, route_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE route_id = VALUES(route_id)",
            productId, routeId);
    }

    public void unbind(Long productId, Long routeId) {
        requireJdbc().update("DELETE FROM product_route WHERE product_id = ? AND route_id = ?", productId, routeId);
    }

    public Long findRouteIdByProduct(Long productId) {
        var list = requireJdbc().query("SELECT route_id FROM product_route WHERE product_id = ?",
            (rs, n) -> rs.getLong("route_id"), productId);
        return list.isEmpty() ? null : list.get(0);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) throw new DataAccessResourceFailureException("JdbcTemplate 未配置");
        return jdbc;
    }
}
