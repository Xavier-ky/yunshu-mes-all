package com.yunshu.mes.quality.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 待检任务聚合：IQC(到货/外协) + PQC(产出行优先/报工兜底) + OQC + RQC(退料/销退)。
 */
@RestController
@RequestMapping("/api/mes/qc/pending")
public class QcPendingController {

    private final JdbcTemplate jdbc;

    public QcPendingController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = new ArrayList<>();
        addSafe(rows, this::listArrivalIqcPending);
        addSafe(rows, this::listOutsourceIqcPending);
        addSafe(rows, this::listOqcPending);
        addSafe(rows, this::listProducePqcPending);
        addSafe(rows, this::listFeedbackPqcPending);
        addSafe(rows, this::listRqcIssuePending);
        addSafe(rows, this::listRqcSalesPending);

        String qcType = params.get("qcType");
        if (StringUtils.hasText(qcType)) {
            rows.removeIf(r -> !qcType.equals(String.valueOf(r.get("qcType"))));
        }
        String itemCode = params.get("itemCode");
        if (StringUtils.hasText(itemCode)) {
            rows.removeIf(r -> {
                Object c = r.get("itemCode");
                return c == null || !String.valueOf(c).contains(itemCode.trim());
            });
        }
        rows.sort((a, b) -> {
            long tb = toSortEpochMillis(b.get("createTime"));
            long ta = toSortEpochMillis(a.get("createTime"));
            if (tb != ta) {
                return Long.compare(tb, ta);
            }
            return Long.compare(longValue(b.get("sourceDocId")), longValue(a.get("sourceDocId")));
        });
        return MesApiResponse.table(rows, rows.size());
    }

    private static long toSortEpochMillis(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.getTime();
        }
        if (value instanceof java.util.Date d) {
            return d.getTime();
        }
        try {
            return java.sql.Timestamp.valueOf(String.valueOf(value).replace("T", " ").substring(0, 19)).getTime();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static long longValue(Object value) {
        if (value == null || "".equals(String.valueOf(value))) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void addSafe(List<Map<String, Object>> rows, java.util.function.Supplier<List<Map<String, Object>>> s) {
        try {
            rows.addAll(s.get());
        } catch (Exception ignored) {
        }
    }

    private List<Map<String, Object>> listArrivalIqcPending() {
        return jdbc.query("""
                SELECT 'IQC' AS qcType, 'IQC' AS qcDetailType, 'ARRIVAL_NOTICE' AS sourceDocType,
                       h.notice_id AS sourceDocId, h.notice_code AS sourceDocCode, h.notice_name AS sourceDocName,
                       l.line_id AS sourceLineId, l.item_id AS itemId, l.item_code AS itemCode,
                       l.item_name AS itemName, l.specification AS specification,
                       l.unit_of_measure AS unitOfMeasure, l.unit_name AS unitName,
                       l.quantity_arrival AS quantityCheck, NULL AS batchCode,
                       h.vendor_id AS clientId, h.vendor_code AS clientCode, h.vendor_name AS clientName,
                       NULL AS workOrderId, NULL AS workOrderCode, NULL AS workOrderName,
                       NULL AS taskId, NULL AS taskCode,
                       NULL AS workstationId, NULL AS workstationCode, NULL AS workstationName, NULL AS processName,
                       NULL AS customerOrderNo,
                       h.create_time AS createTime
                FROM wm_arrival_notice_line l
                JOIN wm_arrival_notice h ON h.notice_id = l.notice_id
                WHERE l.iqc_check = 'Y' AND l.iqc_id IS NULL AND h.status = 'APPROVING'
                ORDER BY h.notice_id DESC LIMIT 100
                """, (rs, n) -> mapRow(rs));
    }

    private List<Map<String, Object>> listOutsourceIqcPending() {
        return jdbc.query("""
                SELECT 'IQC' AS qcType, 'OIQC' AS qcDetailType, 'OUTSOURCE_RECPT' AS sourceDocType,
                       h.recpt_id AS sourceDocId, h.recpt_code AS sourceDocCode, h.recpt_name AS sourceDocName,
                       l.line_id AS sourceLineId, l.item_id AS itemId, l.item_code AS itemCode,
                       l.item_name AS itemName, l.specification AS specification,
                       l.unit_of_measure AS unitOfMeasure, l.unit_name AS unitName,
                       l.quantity_recived AS quantityCheck, l.batch_code AS batchCode,
                       h.vendor_id AS clientId, h.vendor_code AS clientCode, h.vendor_name AS clientName,
                       h.workorder_id AS workOrderId, h.workorder_code AS workOrderCode, NULL AS workOrderName,
                       NULL AS taskId, NULL AS taskCode,
                       NULL AS workstationId, NULL AS workstationCode, NULL AS workstationName, NULL AS processName,
                       co.order_no AS customerOrderNo,
                       h.create_time AS createTime
                FROM wm_outsource_recpt_line l
                JOIN wm_outsource_recpt h ON h.recpt_id = l.recpt_id
                LEFT JOIN work_order wo ON wo.work_order_id = h.workorder_id
                LEFT JOIN customer_order co ON co.order_id = wo.order_id
                WHERE h.status = 'UNCHECK' AND IFNULL(l.quality_status,'NT') = 'NT'
                ORDER BY h.recpt_id DESC LIMIT 100
                """, (rs, n) -> mapRow(rs));
    }

    private List<Map<String, Object>> listOqcPending() {
        return jdbc.query("""
                SELECT 'OQC' AS qcType, 'OQC' AS qcDetailType, 'PRODUCT_SALES' AS sourceDocType,
                       h.sales_id AS sourceDocId, h.sales_code AS sourceDocCode, h.sales_name AS sourceDocName,
                       l.line_id AS sourceLineId, l.item_id AS itemId, l.item_code AS itemCode,
                       l.item_name AS itemName, l.specification AS specification,
                       l.unit_of_measure AS unitOfMeasure, l.unit_name AS unitName,
                       l.quantity_sales AS quantityCheck, l.batch_code AS batchCode,
                       h.client_id AS clientId, h.client_code AS clientCode, h.client_name AS clientName,
                       NULL AS workOrderId, NULL AS workOrderCode, NULL AS workOrderName,
                       NULL AS taskId, NULL AS taskCode,
                       NULL AS workstationId, NULL AS workstationCode, NULL AS workstationName, NULL AS processName,
                       NULL AS customerOrderNo,
                       h.create_time AS createTime
                FROM wm_product_sales_line l
                JOIN wm_product_sales h ON h.sales_id = l.sales_id
                WHERE (l.oqc_id IS NULL OR l.oqc_id = 0)
                  AND (l.quality_status IS NULL OR l.quality_status = '' OR l.quality_status IN ('WAIT','NT'))
                  AND h.status NOT IN ('FINISHED','CANCELED')
                ORDER BY h.sales_id DESC LIMIT 100
                """, (rs, n) -> mapRow(rs));
    }

    /** 优先：产出行待检 */
    private List<Map<String, Object>> listProducePqcPending() {
        return jdbc.query("""
                SELECT 'PQC' AS qcType, 'IPQC' AS qcDetailType, 'PRODUCT_PRODUCE' AS sourceDocType,
                       p.record_id AS sourceDocId, IFNULL(p.workorder_code, CONCAT('PP-', p.record_id)) AS sourceDocCode,
                       IFNULL(p.workorder_name, p.workorder_code) AS sourceDocName,
                       l.line_id AS sourceLineId, l.item_id AS itemId, l.item_code AS itemCode,
                       l.item_name AS itemName, l.specification AS specification,
                       l.unit_of_measure AS unitOfMeasure, l.unit_name AS unitName,
                       l.quantity_produce AS quantityCheck, l.batch_code AS batchCode,
                       NULL AS clientId, NULL AS clientCode, NULL AS clientName,
                       p.workorder_id AS workOrderId, p.workorder_code AS workOrderCode, p.workorder_name AS workOrderName,
                       p.task_id AS taskId, p.task_code AS taskCode,
                       p.workstation_id AS workstationId, p.workstation_code AS workstationCode, p.workstation_name AS workstationName,
                       NULL AS processName,
                       co.order_no AS customerOrderNo,
                       COALESCE(p.create_time, p.update_time) AS createTime
                FROM wm_product_produce_line l
                JOIN wm_product_produce p ON p.record_id = l.record_id
                LEFT JOIN work_order wo ON wo.work_order_id = p.workorder_id
                LEFT JOIN customer_order co ON co.order_id = wo.order_id
                WHERE p.status NOT IN ('FINISHED','CANCELED')
                  AND (l.quality_status IS NULL OR l.quality_status IN ('','NT','WAIT'))
                ORDER BY p.record_id DESC LIMIT 100
                """, (rs, n) -> mapRow(rs));
    }

    /** 兜底：报工 quantity_uncheck（无产出行时） */
    private List<Map<String, Object>> listFeedbackPqcPending() {
        return jdbc.query("""
                SELECT 'PQC' AS qcType, 'IPQC' AS qcDetailType, 'FEEDBACK' AS sourceDocType,
                       f.record_id AS sourceDocId, IFNULL(f.feedback_code, CONCAT('FB-', f.record_id)) AS sourceDocCode,
                       CONCAT(IFNULL(f.workorder_code,''), '-', IFNULL(f.process_name,'')) AS sourceDocName,
                       NULL AS sourceLineId, f.item_id AS itemId, f.item_code AS itemCode,
                       f.item_name AS itemName, f.specification AS specification,
                       f.unit_of_measure AS unitOfMeasure, f.unit_name AS unitName,
                       IFNULL(f.quantity_uncheck, f.quantity_feedback) AS quantityCheck, NULL AS batchCode,
                       NULL AS clientId, NULL AS clientCode, NULL AS clientName,
                       f.workorder_id AS workOrderId, f.workorder_code AS workOrderCode, f.workorder_name AS workOrderName,
                       f.task_id AS taskId, f.task_code AS taskCode,
                       f.workstation_id AS workstationId, f.workstation_code AS workstationCode, f.workstation_name AS workstationName,
                       f.process_name AS processName,
                       co.order_no AS customerOrderNo,
                       COALESCE(f.feedback_time, f.update_time, f.create_time) AS createTime
                FROM pro_feedback f
                LEFT JOIN work_order wo ON wo.work_order_id = f.workorder_id
                LEFT JOIN customer_order co ON co.order_id = wo.order_id
                WHERE f.status = 'FINISHED' AND IFNULL(f.quantity_uncheck, 0) > 0
                  AND NOT EXISTS (
                    SELECT 1 FROM wm_product_produce p WHERE p.feedback_id = f.record_id
                  )
                ORDER BY f.record_id DESC LIMIT 100
                """, (rs, n) -> mapRow(rs));
    }

    private List<Map<String, Object>> listRqcIssuePending() {
        return jdbc.query("""
                SELECT 'RQC' AS qcType, 'PRQC' AS qcDetailType, 'RT_ISSUE' AS sourceDocType,
                       h.rt_id AS sourceDocId, h.rt_code AS sourceDocCode, h.rt_name AS sourceDocName,
                       l.line_id AS sourceLineId, l.item_id AS itemId, l.item_code AS itemCode,
                       l.item_name AS itemName, l.specification AS specification,
                       l.unit_of_measure AS unitOfMeasure, l.unit_name AS unitName,
                       l.quantity_rt AS quantityCheck, l.batch_code AS batchCode,
                       NULL AS clientId, NULL AS clientCode, NULL AS clientName,
                       h.workorder_id AS workOrderId, h.workorder_code AS workOrderCode, NULL AS workOrderName,
                       NULL AS taskId, NULL AS taskCode,
                       h.workstation_id AS workstationId, h.workstation_code AS workstationCode, h.workstation_name AS workstationName,
                       NULL AS processName,
                       co.order_no AS customerOrderNo,
                       h.create_time AS createTime
                FROM wm_rt_issue_line l
                JOIN wm_rt_issue h ON h.rt_id = l.rt_id
                LEFT JOIN work_order wo ON wo.work_order_id = h.workorder_id
                LEFT JOIN customer_order co ON co.order_id = wo.order_id
                WHERE h.status IN ('UNCHECK','PREPARE')
                  AND (l.quality_status IS NULL OR l.quality_status = '' OR l.quality_status IN ('WAIT','NT'))
                ORDER BY h.rt_id DESC LIMIT 100
                """, (rs, n) -> mapRow(rs));
    }

    private List<Map<String, Object>> listRqcSalesPending() {
        return jdbc.query("""
                SELECT 'RQC' AS qcType, 'CRQC' AS qcDetailType, 'RT_SALES' AS sourceDocType,
                       h.rt_id AS sourceDocId, h.rt_code AS sourceDocCode, h.rt_name AS sourceDocName,
                       l.line_id AS sourceLineId, l.item_id AS itemId, l.item_code AS itemCode,
                       l.item_name AS itemName, l.specification AS specification,
                       l.unit_of_measure AS unitOfMeasure, l.unit_name AS unitName,
                       l.quantity_rted AS quantityCheck, l.batch_code AS batchCode,
                       h.client_id AS clientId, h.client_code AS clientCode, h.client_name AS clientName,
                       NULL AS workOrderId, NULL AS workOrderCode, NULL AS workOrderName,
                       NULL AS taskId, NULL AS taskCode,
                       NULL AS workstationId, NULL AS workstationCode, NULL AS workstationName, NULL AS processName,
                       NULL AS customerOrderNo,
                       h.create_time AS createTime
                FROM wm_rt_sales_line l
                JOIN wm_rt_sales h ON h.rt_id = l.rt_id
                WHERE h.status IN ('UNCHECK','PREPARE')
                  AND (l.quality_status IS NULL OR l.quality_status = '' OR l.quality_status IN ('WAIT','NT'))
                ORDER BY h.rt_id DESC LIMIT 100
                """, (rs, n) -> mapRow(rs));
    }

    private Map<String, Object> mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("qcType", rs.getString("qcType"));
        m.put("qcDetailType", rs.getString("qcDetailType"));
        m.put("sourceDocType", rs.getString("sourceDocType"));
        m.put("sourceDocId", rs.getObject("sourceDocId"));
        m.put("sourceDocCode", rs.getString("sourceDocCode"));
        m.put("sourceDocName", rs.getString("sourceDocName"));
        m.put("sourceLineId", rs.getObject("sourceLineId"));
        m.put("itemId", rs.getObject("itemId"));
        m.put("itemCode", rs.getString("itemCode"));
        m.put("itemName", rs.getString("itemName"));
        m.put("specification", rs.getString("specification"));
        m.put("unitOfMeasure", rs.getString("unitOfMeasure"));
        m.put("unitName", rs.getString("unitName"));
        m.put("quantityCheck", rs.getObject("quantityCheck"));
        m.put("batchCode", rs.getString("batchCode"));
        m.put("clientId", rs.getObject("clientId"));
        m.put("clientCode", rs.getString("clientCode"));
        m.put("clientName", rs.getString("clientName"));
        m.put("workOrderId", rs.getObject("workOrderId"));
        m.put("workOrderCode", rs.getString("workOrderCode"));
        m.put("workOrderName", rs.getString("workOrderName"));
        m.put("taskId", rs.getObject("taskId"));
        m.put("taskCode", rs.getString("taskCode"));
        m.put("workstationId", rs.getObject("workstationId"));
        m.put("workstationCode", rs.getString("workstationCode"));
        m.put("workstationName", rs.getString("workstationName"));
        m.put("processName", rs.getString("processName"));
        m.put("customerOrderNo", rs.getString("customerOrderNo"));
        // pending iqc.vue 用 vendorClient* 命名
        m.put("vendorClientId", rs.getObject("clientId"));
        m.put("vendorClientCode", rs.getString("clientCode"));
        m.put("vendorClientName", rs.getString("clientName"));
        m.put("createTime", rs.getTimestamp("createTime"));
        return m;
    }
}
