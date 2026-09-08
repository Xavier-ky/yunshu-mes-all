package com.yunshu.mes.inventory.repository;

import com.yunshu.mes.inventory.vo.InventoryBatchVO;
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
 * 库存批次数据访问。
 */
@Repository
public class InventoryBatchRepository {

    private static final String SELECT_LIST = """
            SELECT ib.batch_id, ib.batch_no, m.material_name, w.warehouse_name,
                   ib.available_qty, ib.status
            FROM inventory_batch ib
            LEFT JOIN material m ON ib.material_id = m.material_id
            LEFT JOIN warehouse w ON ib.warehouse_id = w.warehouse_id
            ORDER BY ib.batch_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT ib.batch_id, ib.batch_no, m.material_name, w.warehouse_name,
                   ib.available_qty, ib.status
            FROM inventory_batch ib
            LEFT JOIN material m ON ib.material_id = m.material_id
            LEFT JOIN warehouse w ON ib.warehouse_id = w.warehouse_id
            WHERE ib.batch_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public InventoryBatchRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<InventoryBatchVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new InventoryBatchVO(
                rs.getLong("batch_id"),
                rs.getString("batch_no"),
                rs.getString("material_name"),
                rs.getString("warehouse_name"),
                rs.getLong("available_qty"),
                rs.getString("status")));
    }

    public Optional<InventoryBatchVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<InventoryBatchVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new InventoryBatchVO(
                rs.getLong("batch_id"),
                rs.getString("batch_no"),
                rs.getString("material_name"),
                rs.getString("warehouse_name"),
                rs.getLong("available_qty"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String batchNo, Long materialId, Long warehouseId, Long locationId,
                       Long availableQty, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO inventory_batch (batch_no, material_id, warehouse_id, location_id, available_qty, status) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, batchNo);
            ps.setLong(2, materialId);
            ps.setLong(3, warehouseId);
            if (locationId != null) {
                ps.setLong(4, locationId);
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setLong(5, availableQty);
            ps.setString(6, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String batchNo, Long materialId, Long warehouseId, Long locationId,
                       Long availableQty, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                UPDATE inventory_batch
                SET batch_no = ?, material_id = ?, warehouse_id = ?, location_id = ?, available_qty = ?, status = ?
                WHERE batch_id = ?
                """, batchNo, materialId, warehouseId, locationId, availableQty, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM inventory_batch WHERE batch_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
