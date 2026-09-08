package com.yunshu.mes.inventory.repository;

import com.yunshu.mes.inventory.vo.WarehouseVO;
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
 * 仓库数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class WarehouseRepository {

    private static final String SELECT_LIST = """
            SELECT warehouse_id, warehouse_code, warehouse_name, warehouse_type, status
            FROM warehouse ORDER BY warehouse_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT warehouse_id, warehouse_code, warehouse_name, warehouse_type, status
            FROM warehouse WHERE warehouse_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public WarehouseRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<WarehouseVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new WarehouseVO(
                rs.getLong("warehouse_id"),
                rs.getString("warehouse_code"),
                rs.getString("warehouse_name"),
                rs.getString("warehouse_type"),
                rs.getString("status")));
    }

    public Optional<WarehouseVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<WarehouseVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new WarehouseVO(
                rs.getLong("warehouse_id"),
                rs.getString("warehouse_code"),
                rs.getString("warehouse_name"),
                rs.getString("warehouse_type"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String warehouseCode, String warehouseName, String warehouseType, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO warehouse (warehouse_code, warehouse_name, warehouse_type, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, warehouseCode);
            ps.setString(2, warehouseName);
            ps.setString(3, warehouseType);
            ps.setString(4, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String warehouseCode, String warehouseName, String warehouseType, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE warehouse SET warehouse_code = ?, warehouse_name = ?, warehouse_type = ?, status = ? WHERE warehouse_id = ?",
                warehouseCode, warehouseName, warehouseType, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM warehouse WHERE warehouse_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
