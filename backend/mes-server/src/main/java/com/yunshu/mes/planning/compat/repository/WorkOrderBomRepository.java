package com.yunshu.mes.planning.compat.repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WorkOrderBomRepository {

    private final JdbcTemplate jdbc;

    public WorkOrderBomRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findByWorkOrderId(Long workOrderId) {
        return jdbc.query("""
                SELECT wob.*, m.material_code AS mat_code, m.material_name AS mat_name
                FROM work_order_bom wob
                LEFT JOIN material m ON wob.material_id = m.material_id
                WHERE wob.work_order_id = ?
                ORDER BY wob.line_id
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineId", rs.getLong("line_id"));
            row.put("workorderId", rs.getLong("work_order_id"));
            row.put("itemId", rs.getLong("material_id"));
            row.put("itemCode", rs.getString("material_code"));
            row.put("itemName", rs.getString("material_name"));
            row.put("itemSpc", rs.getString("specification"));
            row.put("unitOfMeasure", rs.getString("unit_code"));
            row.put("unitName", rs.getString("unit_code"));
            row.put("itemOrProduct", rs.getString("item_or_product"));
            row.put("quantity", rs.getBigDecimal("quantity"));
            row.put("remark", rs.getString("remark"));
            return row;
        }, workOrderId);
    }

    public void deleteByWorkOrderId(Long workOrderId) {
        jdbc.update("DELETE FROM work_order_bom WHERE work_order_id = ?", workOrderId);
    }

    public void insert(Long workOrderId, Long materialId, String materialCode, String materialName,
                       String specification, String unitCode, String itemOrProduct, BigDecimal quantity, String remark) {
        jdbc.update("""
                INSERT INTO work_order_bom
                (work_order_id, material_id, material_code, material_name, specification,
                 unit_code, item_or_product, quantity, remark)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, workOrderId, materialId, materialCode, materialName, specification,
                unitCode, itemOrProduct, quantity, remark);
    }

    public void update(Long lineId, Map<String, Object> body) {
        jdbc.update("""
                UPDATE work_order_bom SET
                  material_id = COALESCE(?, material_id),
                  material_code = COALESCE(?, material_code),
                  material_name = COALESCE(?, material_name),
                  specification = COALESCE(?, specification),
                  unit_code = COALESCE(?, unit_code),
                  item_or_product = COALESCE(?, item_or_product),
                  quantity = COALESCE(?, quantity),
                  remark = COALESCE(?, remark)
                WHERE line_id = ?
                """,
                body.get("itemId"), body.get("itemCode"), body.get("itemName"), body.get("itemSpc"),
                body.get("unitOfMeasure"), body.get("itemOrProduct"), body.get("quantity"), body.get("remark"),
                lineId);
    }

    public void delete(Long lineId) {
        jdbc.update("DELETE FROM work_order_bom WHERE line_id = ?", lineId);
    }
}
