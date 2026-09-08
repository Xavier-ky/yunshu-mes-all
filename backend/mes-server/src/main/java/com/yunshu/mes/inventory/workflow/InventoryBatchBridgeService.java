package com.yunshu.mes.inventory.workflow;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

/**
 * Keeps {@code inventory_batch} (kitting / trace / native inventory) aligned with
 * {@code wm_material_stock} (WMS compat ledger) across inbound and outbound executes.
 */
@Service
public class InventoryBatchBridgeService {

    private static final Logger log = LoggerFactory.getLogger(InventoryBatchBridgeService.class);

    private final JdbcTemplate jdbc;

    public InventoryBatchBridgeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** After material inbound execute: mirror quantities into inventory_batch and link stock rows. */
    public void syncInboundFromItemRecpt(Long recptId) {
        if (recptId == null) {
            return;
        }
        List<Map<String, Object>> details = jdbc.queryForList("""
                SELECT d.detail_id, d.line_id, d.recpt_id, d.item_id, d.item_code, d.item_name,
                       d.quantity, d.batch_id, d.batch_code,
                       d.warehouse_id, d.location_id, d.area_id, r.recpt_code
                FROM wm_item_recpt_detail d
                JOIN wm_item_recpt r ON r.recpt_id = d.recpt_id
                WHERE d.recpt_id = ?
                """, recptId);
        for (Map<String, Object> detail : details) {
            applyInboundDetail(detail, "IR", "wm_item_recpt_detail");
        }
        log.info("inventory_batch bridged for item recpt {}", recptId);
    }

    /** After production return execute: add quantities back to inventory_batch. */
    public void syncInboundFromRtIssue(Long rtId) {
        if (rtId == null) {
            return;
        }
        List<Map<String, Object>> details = jdbc.queryForList("""
                SELECT d.detail_id, d.line_id, d.rt_id AS recpt_id, d.item_id, d.item_code, d.item_name,
                       d.quantity, d.batch_id, d.batch_code,
                       d.warehouse_id, d.location_id, d.area_id, h.rt_code AS recpt_code
                FROM wm_rt_issue_detail d
                JOIN wm_rt_issue h ON h.rt_id = d.rt_id
                WHERE d.rt_id = ?
                """, rtId);
        for (Map<String, Object> detail : details) {
            applyInboundDetail(detail, "RT", "wm_rt_issue_detail");
        }
        log.info("inventory_batch bridged for rt issue {}", rtId);
    }

    /** After issue execute: deduct inventory_batch balances that kitting / trace rely on. */
    public void syncOutboundFromIssue(Long issueId) {
        if (issueId == null) {
            return;
        }
        List<Map<String, Object>> details = jdbc.queryForList("""
                SELECT detail_id, material_stock_id, item_id, item_code, quantity, batch_id, batch_code
                FROM wm_issue_detail WHERE issue_id = ?
                """, issueId);
        for (Map<String, Object> detail : details) {
            double qty = toDouble(detail.get("quantity"));
            if (qty <= 0) {
                continue;
            }
            Long batchId = longOrNull(detail.get("batch_id"));
            if (batchId == null) {
                Long stockId = longOrNull(detail.get("material_stock_id"));
                if (stockId != null) {
                    batchId = jdbc.query("""
                            SELECT batch_id FROM wm_material_stock WHERE material_stock_id = ? LIMIT 1
                            """, rs -> rs.next() ? longOrNull(rs.getObject("batch_id")) : null, stockId);
                }
            }
            if (batchId != null) {
                deductBatch(batchId, qty);
                syncReservedFromBatch(batchId);
            } else {
                Optional<Long> materialId = resolveMaterialId(longOrNull(detail.get("item_id")), str(detail.get("item_code")));
                if (materialId.isPresent()) {
                    deductFifo(materialId.get(), qty);
                } else {
                    log.warn("issue {} detail {} skipped batch bridge: material not resolved", issueId, detail.get("detail_id"));
                }
            }
        }
        log.info("inventory_batch deducted for issue {}", issueId);
    }

    /**
     * Lazily mirror unlinked wm_material_stock rows for a core material into inventory_batch
     * so kitting reserve can lock batches for stock received before this bridge existed.
     */
    public void ensureMaterialStockBridged(Long materialId) {
        if (materialId == null) {
            return;
        }
        List<Map<String, Object>> stocks = jdbc.queryForList("""
                SELECT s.material_stock_id, s.item_id, s.item_code, s.item_name,
                       s.quantity_onhand, s.batch_code, s.warehouse_id, s.location_id, s.area_id
                FROM wm_material_stock s
                JOIN md_item mi ON mi.item_id = s.item_id
                  AND mi.attr1 = 'MATERIAL' AND mi.attr2 = CAST(? AS CHAR)
                WHERE s.frozen_flag = 'N'
                  AND s.quantity_onhand > 0
                  AND s.batch_id IS NULL
                """, materialId);
        for (Map<String, Object> stock : stocks) {
            double qty = toDouble(stock.get("quantity_onhand"));
            if (qty <= 0) {
                continue;
            }
            String batchNo = str(stock.get("batch_code"));
            if (batchNo == null || batchNo.isBlank()) {
                batchNo = "STK-" + stock.get("material_stock_id");
            }
            Long warehouseId = longOrNull(stock.get("warehouse_id"));
            if (warehouseId == null) {
                continue;
            }
            Long batchId = upsertInventoryBatch(materialId, warehouseId, batchNo, qty, batchNo);
            linkStockRow(longOrNull(stock.get("item_id")), warehouseId,
                    longOrNull(stock.get("location_id")), longOrNull(stock.get("area_id")),
                    batchNo, batchId);
        }
    }

    /** Combined availability for kitting: inventory_batch + unlinked wm_material_stock. */
    public BigDecimal availableQtyForMaterial(Long materialId) {
        if (materialId == null) {
            return BigDecimal.ZERO;
        }
        ensureMaterialStockBridged(materialId);
        BigDecimal fromBatch = jdbc.queryForObject("""
                SELECT COALESCE(SUM(available_qty - locked_qty), 0)
                FROM inventory_batch
                WHERE material_id = ? AND status = 'IN_STOCK'
                """, BigDecimal.class, materialId);
        return fromBatch == null ? BigDecimal.ZERO : fromBatch;
    }

    private void applyInboundDetail(Map<String, Object> detail, String docPrefix, String detailTable) {
        double qty = toDouble(detail.get("quantity"));
        if (qty <= 0) {
            return;
        }
        Long mdItemId = longOrNull(detail.get("item_id"));
        String itemCode = str(detail.get("item_code"));
        Optional<Long> materialId = resolveMaterialId(mdItemId, itemCode);
        if (materialId.isEmpty()) {
            log.warn("skip inbound bridge: material not resolved for item {} ({})", mdItemId, itemCode);
            return;
        }
        Long warehouseId = longOrNull(detail.get("warehouse_id"));
        if (warehouseId == null) {
            warehouseId = jdbc.query("""
                    SELECT warehouse_id FROM warehouse WHERE warehouse_type = 'RAW' ORDER BY warehouse_id LIMIT 1
                    """, rs -> rs.next() ? rs.getLong("warehouse_id") : null);
        }
        if (warehouseId == null) {
            log.warn("skip inbound bridge: warehouse missing for item {}", itemCode);
            return;
        }
        String batchNo = str(detail.get("batch_code"));
        if (batchNo == null || batchNo.isBlank()) {
            batchNo = docPrefix + "-" + str(detail.get("recpt_code")) + "-" + detail.get("line_id");
        }
        Long batchId = upsertInventoryBatch(materialId.get(), warehouseId, batchNo, qty, batchNo);
        Long detailId = longOrNull(detail.get("detail_id"));
        if (detailId != null && detailTable != null && !detailTable.isBlank()) {
            jdbc.update("UPDATE " + detailTable + " SET batch_id = ?, batch_code = ? WHERE detail_id = ?",
                    batchId, batchNo, detailId);
        }
        linkStockRow(mdItemId, warehouseId,
                longOrNull(detail.get("location_id")), longOrNull(detail.get("area_id")),
                batchNo, batchId);
    }

    private Long upsertInventoryBatch(Long materialId, Long warehouseId, String batchNo, double qty, String supplierBatchNo) {
        Long existingId = jdbc.query("""
                SELECT batch_id FROM inventory_batch WHERE material_id = ? AND batch_no = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong("batch_id") : null, materialId, batchNo);
        if (existingId != null) {
            jdbc.update("""
                    UPDATE inventory_batch
                    SET available_qty = available_qty + ?, updated_at = NOW(3)
                    WHERE batch_id = ?
                    """, qty, existingId);
            return existingId;
        }
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO inventory_batch (material_id, warehouse_id, batch_no, supplier_batch_no,
                      available_qty, locked_qty, quality_status, received_at, status, created_at, updated_at)
                    VALUES (?,?,?,?,?,0,'QUALIFIED',NOW(3),'IN_STOCK',NOW(3),NOW(3))
                    """, new String[] { "batch_id" });
            int i = 1;
            ps.setLong(i++, materialId);
            ps.setLong(i++, warehouseId);
            ps.setString(i++, batchNo);
            ps.setString(i++, supplierBatchNo);
            ps.setBigDecimal(i, BigDecimal.valueOf(qty));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    private void linkStockRow(Long itemId, Long warehouseId, Long locationId, Long areaId, String batchCode, Long batchId) {
        if (itemId == null || warehouseId == null || batchId == null) {
            return;
        }
        Long stockId = jdbc.query("""
                SELECT material_stock_id FROM wm_material_stock
                WHERE item_id = ? AND warehouse_id = ?
                  AND IFNULL(location_id, 0) = IFNULL(?, 0)
                  AND IFNULL(area_id, 0) = IFNULL(?, 0)
                  AND (batch_id IS NULL OR IFNULL(batch_code, '') = IFNULL(?, ''))
                ORDER BY material_stock_id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("material_stock_id") : null,
                itemId, warehouseId, locationId, areaId, batchCode);
        if (stockId == null) {
            return;
        }
        jdbc.update("""
                UPDATE wm_material_stock
                SET batch_id = ?, batch_code = ?, update_time = NOW()
                WHERE material_stock_id = ?
                """, batchId, batchCode, stockId);
    }

    private void deductBatch(Long batchId, double qty) {
        int updated = jdbc.update("""
                UPDATE inventory_batch
                SET available_qty = available_qty - ?,
                    locked_qty = GREATEST(locked_qty - ?, 0),
                    updated_at = NOW(3)
                WHERE batch_id = ? AND available_qty >= ?
                """, qty, qty, batchId, qty);
        if (updated != 1) {
            throw new IllegalArgumentException("库存批次余额不足，无法完成领料同步");
        }
    }

    private void deductFifo(Long materialId, double qty) {
        double remaining = qty;
        List<Map<String, Object>> batches = jdbc.queryForList("""
                SELECT batch_id, available_qty, locked_qty
                FROM inventory_batch
                WHERE material_id = ? AND status = 'IN_STOCK'
                ORDER BY batch_id
                """, materialId);
        for (Map<String, Object> batch : batches) {
            if (remaining <= 0) {
                break;
            }
            Long batchId = longOrNull(batch.get("batch_id"));
            double available = toDouble(batch.get("available_qty")) - toDouble(batch.get("locked_qty"));
            if (available <= 0 || batchId == null) {
                continue;
            }
            double take = Math.min(remaining, available);
            deductBatch(batchId, take);
            syncReservedFromBatch(batchId);
            remaining -= take;
        }
        if (remaining > 0.0001) {
            throw new IllegalArgumentException("物料库存批次不足，无法完成领料同步");
        }
    }

    private void syncReservedFromBatch(Long batchId) {
        jdbc.update("""
                UPDATE wm_material_stock stock
                JOIN inventory_batch batch ON batch.batch_id = stock.batch_id
                SET stock.quantity_reserved = batch.locked_qty, stock.update_time = NOW()
                WHERE stock.batch_id = ?
                """, batchId);
    }

    public Optional<Long> resolveMaterialId(Long mdItemId, String itemCode) {
        if (mdItemId != null) {
            Long mapped = jdbc.query("""
                    SELECT CAST(mi.attr2 AS UNSIGNED) AS material_id
                    FROM md_item mi
                    WHERE mi.item_id = ? AND mi.attr1 = 'MATERIAL'
                      AND mi.attr2 IS NOT NULL AND mi.attr2 <> ''
                    LIMIT 1
                    """, rs -> rs.next() ? longOrNull(rs.getObject("material_id")) : null, mdItemId);
            if (mapped != null) {
                return Optional.of(mapped);
            }
            Long byCode = jdbc.query("""
                    SELECT m.material_id
                    FROM md_item mi
                    JOIN material m ON m.material_code = mi.item_code AND m.is_deleted = 0
                    WHERE mi.item_id = ?
                    LIMIT 1
                    """, rs -> rs.next() ? rs.getLong("material_id") : null, mdItemId);
            if (byCode != null) {
                return Optional.of(byCode);
            }
        }
        if (itemCode != null && !itemCode.isBlank()) {
            Long direct = jdbc.query("""
                    SELECT material_id FROM material WHERE material_code = ? AND is_deleted = 0 LIMIT 1
                    """, rs -> rs.next() ? rs.getLong("material_id") : null, itemCode);
            if (direct != null) {
                return Optional.of(direct);
            }
        }
        return Optional.empty();
    }

    private static Long longOrNull(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double toDouble(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof BigDecimal bd) {
            return bd.doubleValue();
        }
        return Double.parseDouble(String.valueOf(v));
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v).trim();
    }
}
