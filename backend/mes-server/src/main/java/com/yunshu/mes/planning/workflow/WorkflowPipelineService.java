package com.yunshu.mes.planning.workflow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WorkflowPipelineService {

    private final JdbcTemplate jdbc;

    public WorkflowPipelineService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> todosForRole(String role) {
        String filter = lifecycleFilterForRole(role);
        if (filter == null) {
            return List.of();
        }
        return jdbc.queryForList("""
                SELECT wo.work_order_id AS workOrderId, wo.work_order_no AS workOrderCode,
                       wo.lifecycle_status AS lifecycleStatus, wo.status, wo.plan_qty AS planQty,
                       wo.completed_qty AS completedQty, wo.created_at AS createdAt,
                       p.product_code AS productCode, p.product_name AS productName,
                       co.order_no AS orderNo
                FROM work_order wo
                LEFT JOIN product p ON wo.product_id = p.product_id
                LEFT JOIN customer_order co ON wo.order_id = co.order_id
                WHERE wo.is_deleted = 0 AND (%s)
                ORDER BY wo.created_at DESC, wo.work_order_id DESC
                LIMIT 20
                """.formatted(filter));
    }

    public Map<String, Object> pipelineByKey(String key) {
        Long workOrderId = resolveWorkOrderId(key);
        if (workOrderId == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workOrder", jdbc.queryForMap("""
                SELECT wo.work_order_id AS workOrderId, wo.work_order_no AS workOrderCode,
                       wo.lifecycle_status AS lifecycleStatus, wo.status, wo.plan_qty AS planQty,
                       wo.completed_qty AS completedQty, wo.order_id AS orderId,
                       co.order_no AS orderNo, p.product_code AS productCode, p.product_name AS productName
                FROM work_order wo
                LEFT JOIN customer_order co ON wo.order_id = co.order_id
                LEFT JOIN product p ON wo.product_id = p.product_id
                WHERE wo.work_order_id = ? AND wo.is_deleted = 0
                """, workOrderId));
        result.put("tasks", jdbc.queryForList("""
                SELECT task_id AS taskId, task_no AS taskNo, task_qty AS taskQty,
                       completed_qty AS completedQty, status
                FROM production_task WHERE work_order_id = ?
                """, workOrderId));
        result.put("dispatches", jdbc.queryForList("""
                SELECT dispatch_id AS dispatchId, dispatch_no AS dispatchNo,
                       planned_qty AS plannedQty, completed_qty AS completedQty, status
                FROM dispatch_task WHERE work_order_id = ?
                """, workOrderId));
        result.put("issues", jdbc.queryForList("""
                SELECT issue_id AS issueId, issue_code AS issueCode, status
                FROM wm_issue_header WHERE workorder_id = ?
                """, workOrderId));
        result.put("feedbacks", jdbc.queryForList("""
                SELECT record_id AS recordId, feedback_code AS feedbackCode, status,
                       quantity_qualified AS quantityQualified
                FROM pro_feedback WHERE workorder_id = ?
                """, workOrderId));
        result.put("reports", jdbc.queryForList("""
                SELECT report_id AS reportId, report_no AS reportNo, good_qty AS goodQty,
                       defect_qty AS defectQty, report_time AS reportTime
                FROM production_report WHERE work_order_id = ?
                """, workOrderId));
        result.put("ipqc", jdbc.queryForList("""
                SELECT ipqc_id AS ipqcId, ipqc_code AS ipqcCode, check_result AS checkResult, status
                FROM qc_ipqc WHERE workorder_id = ?
                """, workOrderId));
        result.put("recpts", jdbc.queryForList("""
                SELECT recpt_id AS recptId, recpt_code AS recptCode, status
                FROM wm_product_recpt WHERE workorder_id = ?
                """, workOrderId));
        result.put("consumes", jdbc.queryForList("""
                SELECT record_id AS recordId, status, consume_date AS consumeDate, attr1 AS issueIdRef
                FROM wm_item_consume WHERE workorder_id = ?
                """, workOrderId));
        result.put("productSns", jdbc.queryForList("""
                SELECT sn_id AS snId, sn_code AS snCode, status
                FROM product_sn WHERE work_order_id = ?
                """, workOrderId));
        result.put("bindings", jdbc.queryForList("""
                SELECT pmb.binding_id AS bindingId, ps.sn_code AS snCode,
                       ib.batch_no AS batchNo, m.material_code AS materialCode,
                       pmb.bind_time AS bindTime
                FROM product_material_binding pmb
                JOIN product_sn ps ON pmb.sn_id = ps.sn_id
                JOIN inventory_batch ib ON pmb.batch_id = ib.batch_id
                JOIN material m ON pmb.material_id = m.material_id
                WHERE ps.work_order_id = ?
                """, workOrderId));
        result.put("nextAction", suggestNextAction(String.valueOf(result.get("workOrder") != null
                ? ((Map<?, ?>) result.get("workOrder")).get("lifecycleStatus") : null), roleHint(null)));
        return result;
    }

    public Map<String, Object> traceWorkOrder(String workOrderNo) {
        Map<String, Object> pipeline = pipelineByKey(workOrderNo);
        if (pipeline.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> trace = new LinkedHashMap<>(pipeline);
        @SuppressWarnings("unchecked")
        Map<String, Object> wo = (Map<String, Object>) pipeline.get("workOrder");
        if (wo != null && wo.get("workOrderId") != null) {
            long woId = ((Number) wo.get("workOrderId")).longValue();
            trace.put("orderItems", jdbc.queryForList("""
                    SELECT oi.order_item_id AS orderItemId, oi.order_qty AS orderQty,
                           p.product_code AS productCode
                    FROM customer_order_item oi
                    JOIN product p ON oi.product_id = p.product_id
                    WHERE oi.order_id = (SELECT order_id FROM work_order WHERE work_order_id = ?)
                    """, woId));
        }
        return trace;
    }

    private Long resolveWorkOrderId(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.matches("\\d+")) {
            return Long.parseLong(trimmed);
        }
        return jdbc.query("""
                SELECT work_order_id FROM work_order WHERE work_order_no = ? AND is_deleted = 0 LIMIT 1
                """, rs -> rs.next() ? rs.getLong("work_order_id") : null, trimmed);
    }

    private static String lifecycleFilterForRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "wo.lifecycle_status IS NOT NULL";
        }
        return switch (role.toUpperCase()) {
            case "WAREHOUSE_CLERK" -> """
                    wo.lifecycle_status IN ('RELEASED','KITTING_OK','SCHEDULED','QC_PASSED')
                    OR (wo.lifecycle_status = 'MATERIAL_ISSUED' AND EXISTS (
                        SELECT 1 FROM wm_issue_header ih
                        WHERE ih.workorder_id = wo.work_order_id AND ih.status = 'APPROVED'))
                    """;
            case "LINE_OPERATOR", "TESTER" -> """
                    wo.lifecycle_status IN ('MATERIAL_ISSUED','IN_PROGRESS','QC_PENDING')
                    """;
            case "QUALITY_INSPECTOR" -> "wo.lifecycle_status = 'QC_PENDING'";
            case "PROD_SUPERVISOR", "MANAGER" -> """
                    wo.lifecycle_status IS NULL
                    OR wo.lifecycle_status IN ('DRAFT','RELEASED','KITTING_OK','SCHEDULED','QC_FAILED')
                    """;
            default -> "wo.lifecycle_status IS NOT NULL";
        };
    }

    private static Map<String, String> suggestNextAction(Object lifecycleObj, String role) {
        String ls = lifecycleObj != null ? String.valueOf(lifecycleObj) : "";
        Map<String, String> action = new LinkedHashMap<>();
        switch (ls) {
            case "RELEASED", "KITTING_OK", "SCHEDULED" -> {
                action.put("label", "生产领料");
                action.put("route", "/app/inventory/outbound?tab=issue");
                action.put("role", "WAREHOUSE_CLERK");
            }
            case "MATERIAL_ISSUED", "IN_PROGRESS" -> {
                action.put("label", "现场报工");
                action.put("route", "/app/my-work");
                action.put("role", "LINE_OPERATOR");
            }
            case "QC_PENDING" -> {
                action.put("label", "过程质检");
                action.put("route", "/app/quality/workbench");
                action.put("role", "QUALITY_INSPECTOR");
            }
            case "QC_PASSED" -> {
                action.put("label", "成品入库");
                action.put("route", "/app/inventory/inbound?tab=product-recpt");
                action.put("role", "WAREHOUSE_CLERK");
            }
            case "COMPLETED" -> {
                action.put("label", "追溯查询");
                action.put("route", "/app/analytics/trace");
                action.put("role", "ALL");
            }
            default -> {
                action.put("label", "查看工单");
                action.put("route", "/app/planning/work-orders");
                action.put("role", "PROD_SUPERVISOR");
            }
        }
        return action;
    }

    private static String roleHint(String role) {
        return role;
    }
}
