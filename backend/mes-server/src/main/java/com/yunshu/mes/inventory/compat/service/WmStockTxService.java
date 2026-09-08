package com.yunshu.mes.inventory.compat.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmStockTxService {

    private final JdbcTemplate jdbc;

    public WmStockTxService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Long processTransaction(Map<String, Object> tx) {
        double qty = toDouble(tx.get("transactionQuantity"));
        int flag = toInt(tx.get("transactionFlag"), 1);
        double delta = qty * flag;
        if (qty <= 0) {
            throw new IllegalArgumentException("事务数量必须大于0");
        }
        Long stockId = longObj(tx.get("materialStockId"));
        Map<String, Object> stock;
        if (stockId != null) {
            stock = loadStock(stockId).orElseThrow(() -> new IllegalArgumentException("库存记录不存在"));
            double onhand = toDouble(stock.get("quantityOnhand"));
            double next = onhand + delta;
            if (next < -0.0001) {
                throw new IllegalArgumentException("库存数量不足！");
            }
            jdbc.update("UPDATE wm_material_stock SET quantity_onhand = ?, update_time = ? WHERE material_stock_id = ?",
                    next, Timestamp.valueOf(LocalDateTime.now()), stockId);
        } else {
            stock = findOrCreateStock(tx, delta);
            stockId = longObj(stock.get("materialStockId"));
        }
        final Long txStockId = stockId;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO wm_transaction (transaction_type, item_id, item_code, item_name, specification, unit_name,
                      batch_id, batch_code, warehouse_id, warehouse_code, warehouse_name,
                      location_id, location_code, location_name, area_id, area_code, area_name,
                      source_doc_type, source_doc_id, source_doc_code, source_doc_line_id,
                      material_stock_id, transaction_flag, transaction_quantity, transaction_date, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, str(tx, "transactionType"));
            ps.setLong(i++, longVal(tx.get("itemId")));
            ps.setString(i++, str(tx, "itemCode"));
            ps.setString(i++, str(tx, "itemName"));
            ps.setString(i++, str(tx, "specification"));
            ps.setString(i++, str(tx, "unitName"));
            setLong(ps, i++, tx.get("batchId"));
            ps.setString(i++, str(tx, "batchCode"));
            ps.setLong(i++, longVal(tx.get("warehouseId")));
            ps.setString(i++, str(tx, "warehouseCode"));
            ps.setString(i++, str(tx, "warehouseName"));
            setLong(ps, i++, tx.get("locationId"));
            ps.setString(i++, str(tx, "locationCode"));
            ps.setString(i++, str(tx, "locationName"));
            setLong(ps, i++, tx.get("areaId"));
            ps.setString(i++, str(tx, "areaCode"));
            ps.setString(i++, str(tx, "areaName"));
            ps.setString(i++, str(tx, "sourceDocType"));
            setLong(ps, i++, tx.get("sourceDocId"));
            ps.setString(i++, str(tx, "sourceDocCode"));
            setLong(ps, i++, tx.get("sourceDocLineId"));
            ps.setLong(i++, txStockId);
            ps.setInt(i++, flag);
            ps.setDouble(i++, qty);
            ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(i, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? txStockId : key.longValue();
    }

    private Map<String, Object> findOrCreateStock(Map<String, Object> tx, double delta) {
        Long wh = longVal(tx.get("warehouseId"));
        Long loc = longObj(tx.get("locationId"));
        Long area = longObj(tx.get("areaId"));
        Long item = longVal(tx.get("itemId"));
        String batch = str(tx, "batchCode");
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT material_stock_id, quantity_onhand FROM wm_material_stock
                WHERE item_id = ? AND warehouse_id = ?
                  AND IFNULL(location_id,0) = IFNULL(?,0)
                  AND IFNULL(area_id,0) = IFNULL(?,0)
                  AND IFNULL(batch_code,'') = IFNULL(?, '')
                LIMIT 1
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("materialStockId", rs.getLong("material_stock_id"));
            m.put("quantityOnhand", rs.getDouble("quantity_onhand"));
            return m;
        }, item, wh, loc, area, batch);
        if (!rows.isEmpty()) {
            Map<String, Object> s = rows.get(0);
            double next = toDouble(s.get("quantityOnhand")) + delta;
            if (next < -0.0001) {
                throw new IllegalArgumentException("库存数量不足！");
            }
            jdbc.update("UPDATE wm_material_stock SET quantity_onhand = ?, update_time = ? WHERE material_stock_id = ?",
                    next, Timestamp.valueOf(LocalDateTime.now()), s.get("materialStockId"));
            return s;
        }
        if (delta < 0) {
            throw new IllegalArgumentException("库存记录不存在，无法出库");
        }
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO wm_material_stock (item_id, item_code, item_name, specification, unit_name, batch_code,
                      warehouse_id, warehouse_code, warehouse_name, location_id, location_code, location_name,
                      area_id, area_code, area_name, quantity_onhand, frozen_flag, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setLong(i++, item);
            ps.setString(i++, str(tx, "itemCode"));
            ps.setString(i++, str(tx, "itemName"));
            ps.setString(i++, str(tx, "specification"));
            ps.setString(i++, str(tx, "unitName"));
            ps.setString(i++, batch);
            ps.setLong(i++, wh);
            ps.setString(i++, str(tx, "warehouseCode"));
            ps.setString(i++, str(tx, "warehouseName"));
            setLong(ps, i++, loc);
            ps.setString(i++, str(tx, "locationCode"));
            ps.setString(i++, str(tx, "locationName"));
            setLong(ps, i++, area);
            ps.setString(i++, str(tx, "areaCode"));
            ps.setString(i++, str(tx, "areaName"));
            ps.setDouble(i++, delta);
            ps.setString(i++, "N");
            ps.setTimestamp(i, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, kh);
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("materialStockId", kh.getKey().longValue());
        return created;
    }

    private Optional<Map<String, Object>> loadStock(Long id) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT material_stock_id, quantity_onhand, frozen_flag FROM wm_material_stock WHERE material_stock_id = ?",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("materialStockId", rs.getLong("material_stock_id"));
                    m.put("quantityOnhand", rs.getDouble("quantity_onhand"));
                    m.put("frozenFlag", rs.getString("frozen_flag"));
                    return m;
                }, id);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        if ("Y".equals(rows.get(0).get("frozenFlag"))) {
            throw new IllegalArgumentException("库存已冻结");
        }
        return Optional.of(rows.get(0));
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private static double toDouble(Object v) {
        if (v == null) {
            return 0;
        }
        return Double.parseDouble(String.valueOf(v));
    }

    private static int toInt(Object v, int def) {
        if (v == null) {
            return def;
        }
        return Integer.parseInt(String.valueOf(v));
    }

    private static Long longVal(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            throw new IllegalArgumentException("缺少必要ID");
        }
        return Long.parseLong(String.valueOf(v));
    }

    private static Long longObj(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        return Long.parseLong(String.valueOf(v));
    }

    private static void setLong(java.sql.PreparedStatement ps, int idx, Object v) throws java.sql.SQLException {
        Long l = longObj(v);
        if (l == null) {
            ps.setNull(idx, java.sql.Types.BIGINT);
        } else {
            ps.setLong(idx, l);
        }
    }
}
