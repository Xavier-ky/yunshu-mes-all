package com.yunshu.mes.traceability.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成品入库完成后，同步追溯中心所需数据：流转卡、产出记录、成品批次主数据。
 */
@Service
public class WorkOrderTraceSyncService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderTraceSyncService.class);

    private final JdbcTemplate jdbc;

    public WorkOrderTraceSyncService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void syncFromWorkOrder(Long workOrderId) {
        if (workOrderId == null) {
            return;
        }
        List<Map<String, Object>> woRows = jdbc.queryForList("""
                SELECT wo.work_order_id, wo.work_order_no, wo.work_order_name, wo.plan_qty,
                       p.product_id, p.product_code, p.product_name, p.product_model
                FROM work_order wo
                JOIN product p ON p.product_id = wo.product_id
                WHERE wo.work_order_id = ? AND wo.is_deleted = 0
                """, workOrderId);
        if (woRows.isEmpty()) {
            return;
        }
        Map<String, Object> wo = woRows.get(0);
        String woCode = String.valueOf(wo.get("work_order_no"));

        List<Map<String, Object>> recptLines = jdbc.queryForList("""
                SELECT r.recpt_id, r.recpt_code, r.status AS recpt_status,
                       l.line_id, l.item_id, l.item_code, l.item_name, l.specification,
                       l.unit_name, l.quantity_recived, l.batch_code
                FROM wm_product_recpt r
                JOIN wm_product_recpt_line l ON l.recpt_id = r.recpt_id
                WHERE r.workorder_id = ? AND r.status = 'FINISHED'
                ORDER BY r.recpt_id DESC, l.line_id DESC
                """, workOrderId);
        if (recptLines.isEmpty()) {
            return;
        }
        Map<String, Object> line = recptLines.get(0);
        String batchCode = str(line.get("batch_code"));
        if (batchCode == null || batchCode.isBlank()) {
            batchCode = "PB-" + woCode.replace("WO", "");
        }

        upsertProCard(wo, line, batchCode);
        upsertProductProduce(wo, line, batchCode);
        upsertFinishedGoodsBatch(wo, line, batchCode);
        syncProCardProcess(workOrderId, woCode);

        log.info("Trace sync completed for work order {}", woCode);
    }

    private void upsertProCard(Map<String, Object> wo, Map<String, Object> line, String batchCode) {
        String woCode = String.valueOf(wo.get("work_order_no"));
        Long woId = ((Number) wo.get("work_order_id")).longValue();
        String cardCode = "CARD-" + woCode;

        Long existing = jdbc.query("""
                SELECT card_id FROM pro_card WHERE workorder_id = ? ORDER BY card_id DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong("card_id") : null, woId);
        double qty = toDouble(line.get("quantity_recived"));
        if (existing != null) {
            jdbc.update("""
                    UPDATE pro_card SET card_code=?, workorder_code=?, workorder_name=?, batch_code=?,
                      item_code=?, item_name=?, specification=?, quantity_transfered=?, status='FINISHED',
                      update_time=NOW(3)
                    WHERE card_id=?
                    """,
                    cardCode, woCode, str(wo.get("work_order_name")), batchCode,
                    str(line.get("item_code")), str(line.get("item_name")), str(line.get("specification")),
                    qty, existing);
            return;
        }
        jdbc.update("""
                INSERT INTO pro_card (card_code, workorder_id, workorder_code, workorder_name, batch_code,
                  item_code, item_name, specification, unit_of_measure, quantity_transfered, status, create_by, create_time)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW(3))
                """,
                cardCode, woId, woCode, str(wo.get("work_order_name")), batchCode,
                str(line.get("item_code")), str(line.get("item_name")), str(line.get("specification")),
                "PCS", qty, "FINISHED", "system");
    }

    private void syncProCardProcess(Long workOrderId, String woCode) {
        Long cardId = jdbc.query("""
                SELECT card_id FROM pro_card WHERE workorder_id = ? ORDER BY card_id DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong("card_id") : null, workOrderId);
        if (cardId == null) {
            return;
        }
        Long existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pro_card_process WHERE card_id = ?", Long.class, cardId);
        if (existing != null && existing > 0) {
            return;
        }
        List<Map<String, Object>> steps = jdbc.queryForList("""
                SELECT ps.step_code, ps.step_name, dt.station_id, ws.station_name,
                       dt.completed_qty, dt.actual_start_time, dt.actual_end_time, dt.step_id
                FROM dispatch_task dt
                JOIN process_step ps ON ps.step_id = dt.step_id
                LEFT JOIN workstation ws ON ws.station_id = dt.station_id
                WHERE dt.work_order_id = ? AND dt.status = 'COMPLETED'
                ORDER BY dt.step_id, dt.dispatch_id
                """, workOrderId);
        int seq = 10;
        for (Map<String, Object> step : steps) {
            long stationId = step.get("station_id") instanceof Number n ? n.longValue() : 0L;
            jdbc.update("""
                    INSERT INTO pro_card_process (card_id, card_code, seq_num, process_code, process_name,
                      workstation_id, quantity_input, quantity_output, input_time, output_time, user_id, create_by, create_time)
                    SELECT ?, c.card_code, ?, ?, ?, ?, ?, ?, ?, ?, 0, 'system', NOW(3)
                    FROM pro_card c WHERE c.card_id = ?
                    """,
                    cardId, seq, str(step.get("step_code")), str(step.get("step_name")),
                    stationId, toDouble(step.get("completed_qty")), toDouble(step.get("completed_qty")),
                    step.get("actual_start_time"), step.get("actual_end_time"), cardId);
            seq += 10;
        }
        if (steps.isEmpty()) {
            jdbc.update("""
                    INSERT INTO pro_card_process (card_id, card_code, seq_num, process_code, process_name,
                      workstation_id, quantity_input, quantity_output, user_id, create_by, create_time)
                    SELECT card_id, card_code, 10, 'FINISHED', '成品入库', 0, quantity_transfered, quantity_transfered, 0, 'system', NOW(3)
                    FROM pro_card WHERE card_id = ?
                    """, cardId);
        }
    }

    private void upsertProductProduce(Map<String, Object> wo, Map<String, Object> line, String batchCode) {
        Long woId = ((Number) wo.get("work_order_id")).longValue();
        String woCode = String.valueOf(wo.get("work_order_no"));
        Long recptId = ((Number) line.get("recpt_id")).longValue();

        Long recordId = jdbc.query("""
                SELECT record_id FROM wm_product_produce
                WHERE workorder_id = ? AND (attr1 = ? OR attr1 IS NULL)
                ORDER BY record_id DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong("record_id") : null, woId, String.valueOf(recptId));

        if (recordId == null) {
            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                var ps = con.prepareStatement("""
                        INSERT INTO wm_product_produce (workorder_id, workorder_code, workorder_name, status, remark, attr1, create_by, create_time)
                        VALUES (?,?,?,?,?,?,?,?)
                        """, new String[] { "record_id" });
                ps.setLong(1, woId);
                ps.setString(2, woCode);
                ps.setString(3, str(wo.get("work_order_name")));
                ps.setString(4, "FINISHED");
                ps.setString(5, "synced from product recpt:" + recptId);
                ps.setString(6, String.valueOf(recptId));
                ps.setString(7, "system");
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                return ps;
            }, kh);
            Number key = kh.getKey();
            recordId = key == null ? null : key.longValue();
        } else {
            jdbc.update("UPDATE wm_product_produce SET status='FINISHED', attr1=? WHERE record_id=?",
                    String.valueOf(recptId), recordId);
        }
        if (recordId == null) {
            return;
        }

        Long lineId = jdbc.query("""
                SELECT line_id FROM wm_product_produce_line WHERE record_id = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong("line_id") : null, recordId);
        double qty = toDouble(line.get("quantity_recived"));
        if (lineId == null) {
            jdbc.update("""
                    INSERT INTO wm_product_produce_line (record_id, item_id, item_code, item_name, specification,
                      unit_of_measure, unit_name, quantity_produce, batch_code, quality_status, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(3))
                    """,
                    recordId, line.get("item_id"), str(line.get("item_code")), str(line.get("item_name")),
                    str(line.get("specification")), "PCS", str(line.get("unit_name")), qty, batchCode, "OK", "system");
        } else {
            jdbc.update("""
                    UPDATE wm_product_produce_line SET quantity_produce=?, batch_code=?, quality_status='OK'
                    WHERE line_id=?
                    """, qty, batchCode, lineId);
        }
    }

    private void upsertFinishedGoodsBatch(Map<String, Object> wo, Map<String, Object> line, String batchCode) {
        Long exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_batch WHERE batch_no = ?", Long.class, batchCode);
        if (exists != null && exists > 0) {
            return;
        }
        Long warehouseId = jdbc.query("""
                SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-FIN' LIMIT 1
                """, rs -> rs.next() ? rs.getLong("warehouse_id") : null);
        if (warehouseId == null) {
            warehouseId = jdbc.queryForObject("SELECT warehouse_id FROM warehouse ORDER BY warehouse_id LIMIT 1", Long.class);
        }
        Long materialId = jdbc.query("""
                SELECT m.material_id FROM material m
                WHERE m.material_code = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong("material_id") : null, str(line.get("item_code")));
        if (materialId == null || warehouseId == null) {
            return;
        }
        double qty = toDouble(line.get("quantity_recived"));
        jdbc.update("""
                INSERT INTO inventory_batch (material_id, warehouse_id, batch_no, supplier_batch_no,
                  available_qty, locked_qty, quality_status, received_at, status, created_at, updated_at)
                VALUES (?,?,?,?,?,0,'QUALIFIED',NOW(3),'IN_STOCK',NOW(3),NOW(3))
                """,
                materialId, warehouseId, batchCode, batchCode, qty);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static double toDouble(Object v) {
        return v == null ? 0 : Double.parseDouble(String.valueOf(v));
    }
}
