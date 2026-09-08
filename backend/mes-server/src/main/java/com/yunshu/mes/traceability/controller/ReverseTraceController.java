package com.yunshu.mes.traceability.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.traceability.service.WorkOrderTraceSyncService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traceability")
public class ReverseTraceController {
    private final JdbcTemplate jdbc;
    private final WorkOrderTraceSyncService traceSync;

    public ReverseTraceController(JdbcTemplate jdbc, WorkOrderTraceSyncService traceSync) {
        this.jdbc = jdbc;
        this.traceSync = traceSync;
    }

    @GetMapping("/recent-work-orders")
    public ApiResponse<List<Map<String, Object>>> recentWorkOrders(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest req) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT wo.work_order_id, wo.work_order_no, wo.lifecycle_status,
                       co.order_no, p.product_code, p.product_name, wo.plan_qty
                FROM work_order wo
                LEFT JOIN customer_order co ON wo.order_id = co.order_id
                LEFT JOIN product p ON wo.product_id = p.product_id
                WHERE wo.is_deleted = 0
                  AND (
                    wo.lifecycle_status IN ('MATERIAL_ISSUED','IN_PROGRESS','QC_PENDING','QC_PASSED','COMPLETED')
                    OR EXISTS (SELECT 1 FROM wm_issue_header h WHERE h.workorder_id = wo.work_order_id)
                    OR EXISTS (SELECT 1 FROM wm_product_recpt r WHERE r.workorder_id = wo.work_order_id)
                    OR EXISTS (SELECT 1 FROM pro_feedback f WHERE f.workorder_id = wo.work_order_id)
                  )
                ORDER BY GREATEST(
                    IFNULL((SELECT MAX(f.update_time) FROM pro_feedback f WHERE f.workorder_id = wo.work_order_id), wo.updated_at),
                    IFNULL((SELECT MAX(r.update_time) FROM wm_product_recpt r WHERE r.workorder_id = wo.work_order_id), wo.updated_at),
                    IFNULL((SELECT MAX(q.update_time) FROM qc_ipqc q WHERE q.workorder_id = wo.work_order_id), wo.updated_at),
                    wo.updated_at
                ) DESC, wo.work_order_id DESC
                LIMIT ?
                """, safeLimit);
        return ApiResponse.success(camelRows(rows), req);
    }

    @GetMapping("/batch/{batchNo}")
    public ApiResponse<Map<String,Object>> traceBatch(@PathVariable String batchNo, HttpServletRequest req) {
        // Get batch info
        var batch = jdbc.queryForMap("SELECT ib.*, m.material_code, m.material_name FROM inventory_batch ib JOIN material m ON ib.material_id=m.material_id WHERE ib.batch_no=?", batchNo);
        if (batch.isEmpty()) return ApiResponse.fail("NOT_FOUND","Batch not found",req);

        // Find all SNs that used this batch
        var sns = jdbc.queryForList("""
            SELECT DISTINCT ps.sn_code, ps.product_id, p.product_name, ps.work_order_id, wo.work_order_no, ps.status,
                   pmb.bind_time, pmb.step_id, ps2.step_name
            FROM product_material_binding pmb
            JOIN product_sn ps ON pmb.sn_id=ps.sn_id
            JOIN product p ON ps.product_id=p.product_id
            JOIN work_order wo ON ps.work_order_id=wo.work_order_id
            LEFT JOIN process_step ps2 ON pmb.step_id=ps2.step_id
            WHERE pmb.batch_id=(SELECT batch_id FROM inventory_batch WHERE batch_no=?)
            ORDER BY pmb.bind_time DESC""", batchNo);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("affectedProductCount", sns.size());
        result.put("affectedSNs", sns);
        return ApiResponse.success(result, req);
    }

    @GetMapping("/work-order/{codeOrNo}")
    public ApiResponse<Map<String,Object>> traceWorkOrder(@PathVariable String codeOrNo, HttpServletRequest req) {
        Optional<Long> woIdOpt = resolveWorkOrderId(codeOrNo);
        if (woIdOpt.isEmpty()) {
            return ApiResponse.fail("NOT_FOUND", "未找到关联工单，可输入工单号、客户订单号或入库单号", req);
        }
        Long woId = woIdOpt.get();
        var wo = jdbc.queryForList("""
                SELECT wo.work_order_id, wo.work_order_no, wo.lifecycle_status, wo.status,
                       wo.plan_qty, wo.completed_qty, co.order_no, co.customer_name,
                       p.product_code, p.product_name
                FROM work_order wo
                LEFT JOIN customer_order co ON wo.order_id = co.order_id
                LEFT JOIN product p ON wo.product_id = p.product_id
                WHERE wo.work_order_id = ? AND wo.is_deleted = 0
                """, woId);
        if (wo.isEmpty()) {
            return ApiResponse.fail("NOT_FOUND", "Work order not found", req);
        }
        traceSync.syncFromWorkOrder(woId);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("workOrder", toCamelMap(wo.get(0)));
        result.put("customerOrder", camelRows(jdbc.queryForList("""
                SELECT co.order_id, co.order_no, co.customer_name, co.status,
                       SUM(coi.order_qty) AS order_qty
                FROM customer_order co
                JOIN work_order wo ON wo.order_id = co.order_id
                LEFT JOIN customer_order_item coi ON coi.order_id = co.order_id
                WHERE wo.work_order_id = ?
                GROUP BY co.order_id, co.order_no, co.customer_name, co.status
                """, woId)));
        result.put("issues", camelRows(jdbc.queryForList(
                "SELECT issue_id, issue_code, status FROM wm_issue_header WHERE workorder_id=?", woId)));
        result.put("dispatches", camelRows(jdbc.queryForList("""
                SELECT dt.dispatch_id, dt.dispatch_no, ps.step_name, st.station_name,
                       dt.planned_qty, dt.completed_qty, dt.status
                FROM dispatch_task dt
                LEFT JOIN process_step ps ON ps.step_id = dt.step_id
                LEFT JOIN workstation st ON st.station_id = dt.station_id
                WHERE dt.work_order_id = ?
                ORDER BY dt.dispatch_id
                """, woId)));
        result.put("feedbacks", camelRows(jdbc.queryForList("""
                SELECT record_id, feedback_code, process_name, quantity_qualified, status
                FROM pro_feedback WHERE workorder_id=? ORDER BY record_id
                """, woId)));
        result.put("reports", camelRows(jdbc.queryForList(
                "SELECT report_id, report_no, good_qty FROM production_report WHERE work_order_id=?", woId)));
        result.put("ipqc", camelRows(jdbc.queryForList("""
                SELECT q.ipqc_id, q.ipqc_code,
                       COALESCE(NULLIF(q.process_name, ''), f.process_name) AS process_name,
                       q.quantity_qualified, q.check_result, q.status
                FROM qc_ipqc q
                LEFT JOIN pro_feedback f ON f.feedback_code = q.source_doc_code
                WHERE q.workorder_id = ?
                ORDER BY q.ipqc_id
                """, woId)));
        result.put("recpts", camelRows(jdbc.queryForList("""
                SELECT recpt_id, recpt_code, status,
                       (SELECT IFNULL(SUM(quantity_recived),0) FROM wm_product_recpt_line l WHERE l.recpt_id = r.recpt_id) AS quantity
                FROM wm_product_recpt r WHERE workorder_id=? ORDER BY recpt_id
                """, woId)));
        result.put("proCards", camelRows(jdbc.queryForList(
                "SELECT card_id, card_code, batch_code, item_code, quantity_transfered, status FROM pro_card WHERE workorder_id=?", woId)));
        result.put("proCardProcesses", camelRows(jdbc.queryForList("""
                SELECT pcp.record_id, pcp.process_code, pcp.process_name, pcp.quantity_output, pcp.output_time
                FROM pro_card_process pcp
                JOIN pro_card pc ON pc.card_id = pcp.card_id
                WHERE pc.workorder_id = ?
                ORDER BY pcp.seq_num, pcp.record_id
                """, woId)));
        result.put("consumes", camelRows(jdbc.queryForList(
                "SELECT record_id, status, attr1 AS issueIdRef FROM wm_item_consume WHERE workorder_id=?", woId)));
        result.put("productSns", camelRows(jdbc.queryForList(
                "SELECT sn_id, sn_code, status FROM product_sn WHERE work_order_id=?", woId)));
        result.put("bindings", camelRows(jdbc.queryForList("""
            SELECT pmb.binding_id, ps.sn_code, ib.batch_no, m.material_code, pmb.bind_time
            FROM product_material_binding pmb
            JOIN product_sn ps ON pmb.sn_id=ps.sn_id
            JOIN inventory_batch ib ON pmb.batch_id=ib.batch_id
            JOIN material m ON pmb.material_id=m.material_id
            WHERE ps.work_order_id=?
            ORDER BY pmb.bind_time DESC""", woId)));
        return ApiResponse.success(result, req);
    }

    /** 支持工单号、客户订单号、成品入库单号、报工单号检索 */
    private Optional<Long> resolveWorkOrderId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String key = raw.trim();
        Long byWo = jdbc.query("""
                SELECT work_order_id FROM work_order
                WHERE work_order_no = ? AND is_deleted = 0 LIMIT 1
                """, rs -> rs.next() ? rs.getLong("work_order_id") : null, key);
        if (byWo != null) {
            return Optional.of(byWo);
        }
        Long byOrder = jdbc.query("""
                SELECT wo.work_order_id FROM work_order wo
                JOIN customer_order co ON co.order_id = wo.order_id
                WHERE co.order_no = ? AND wo.is_deleted = 0
                ORDER BY wo.work_order_id DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong("work_order_id") : null, key);
        if (byOrder != null) {
            return Optional.of(byOrder);
        }
        Long byRecpt = jdbc.query("""
                SELECT workorder_id FROM wm_product_recpt
                WHERE recpt_code = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong("workorder_id") : null, key);
        if (byRecpt != null) {
            return Optional.of(byRecpt);
        }
        Long byFeedback = jdbc.query("""
                SELECT workorder_id FROM pro_feedback
                WHERE feedback_code = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong("workorder_id") : null, key);
        return Optional.ofNullable(byFeedback);
    }

    private static List<Map<String, Object>> camelRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            out.add(toCamelMap(row));
        }
        return out;
    }

    private static Map<String, Object> toCamelMap(Map<String, Object> row) {
        if (row == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            out.put(toCamelKey(e.getKey()), e.getValue());
        }
        return out;
    }

    private static String toCamelKey(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : key.toCharArray()) {
            if (c == '_') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    @GetMapping("/material/{materialId}")
    public ApiResponse<Map<String,Object>> traceMaterial(@PathVariable Long materialId, HttpServletRequest req) {
        var material = jdbc.queryForMap("SELECT * FROM material WHERE material_id=?", materialId);
        var batches = jdbc.queryForList("""
            SELECT ib.batch_no, COUNT(DISTINCT pmb.sn_id) as affected_sn_count
            FROM inventory_batch ib
            LEFT JOIN product_material_binding pmb ON ib.batch_id=pmb.batch_id
            WHERE ib.material_id=? GROUP BY ib.batch_id""", materialId);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("material", material);
        result.put("batches", batches);
        return ApiResponse.success(result, req);
    }
}
