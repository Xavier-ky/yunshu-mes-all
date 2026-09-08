package com.yunshu.mes.production.workflow;

import com.yunshu.mes.production.repository.ProductionReportRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * pro_feedback execute 后同步写入 production_report（fail-safe，不影响报工事务）。
 */
@Service
public class ProductionReportSyncService {

    private static final Logger log = LoggerFactory.getLogger(ProductionReportSyncService.class);

    private final JdbcTemplate jdbc;
    private final ProductionReportRepository reportRepo;

    public ProductionReportSyncService(JdbcTemplate jdbc, ProductionReportRepository reportRepo) {
        this.jdbc = jdbc;
        this.reportRepo = reportRepo;
    }

    public void syncFromFeedback(Long recordId, Map<String, Object> fb) {
        try {
            if (recordId == null || fb == null) {
                return;
            }
            if (existsForFeedback(recordId)) {
                return;
            }
            Long workOrderId = longOrNull(fb.get("workorderId"));
            if (workOrderId == null) {
                workOrderId = resolveWorkOrderId(String.valueOf(fb.get("workorderCode")));
            }
            if (workOrderId == null) {
                return;
            }
            Long dispatchId = resolveDispatchId(fb.get("taskId"));
            Long stepId = longOrNull(fb.get("processId"));
            if (stepId == null || stepId == 0) {
                stepId = lookupStepFromDispatch(dispatchId);
            }
            if (stepId == null || stepId == 0) {
                stepId = lookupDefaultStep(workOrderId);
            }
            if (stepId == null) {
                log.warn("production_report sync skipped: no step for feedback {}", recordId);
                return;
            }
            Long stationId = longOrNull(fb.get("workstationId"));
            Long operatorId = resolveOperatorId(fb.get("userName"));
            if (operatorId == null) {
                operatorId = 1L;
            }
            String feedbackCode = fb.get("feedbackCode") != null
                    ? String.valueOf(fb.get("feedbackCode"))
                    : "FB-" + recordId;
            String reportNo = "RPT-" + feedbackCode;
            String goodQty = String.valueOf(fb.getOrDefault("quantityQualified", "0"));
            String defectQty = String.valueOf(fb.getOrDefault("quantityUnquanlified", "0"));

            Long reportId = reportRepo.insertFull(
                    reportNo, dispatchId, workOrderId, stepId, stationId, operatorId, null,
                    "NORMAL", goodQty, defectQty, "synced from pro_feedback:" + recordId);
            jdbc.update("""
                    INSERT INTO production_report_detail (report_id, item_type, item_code, item_value)
                    VALUES (?, 'PRO_FEEDBACK', ?, ?)
                    """, reportId, feedbackCode, String.valueOf(recordId));
        } catch (Exception e) {
            log.warn("production_report sync failed for feedback {}: {}", recordId, e.getMessage());
        }
    }

    private boolean existsForFeedback(Long recordId) {
        Long cnt = jdbc.queryForObject("""
                SELECT COUNT(*) FROM production_report_detail
                WHERE item_type = 'PRO_FEEDBACK' AND item_value = ?
                """, Long.class, String.valueOf(recordId));
        return cnt != null && cnt > 0;
    }

    private Long resolveDispatchId(Object taskId) {
        if (taskId == null) {
            return null;
        }
        long ref = Long.parseLong(String.valueOf(taskId));
        Long byDispatch = jdbc.query("""
                SELECT dispatch_id FROM dispatch_task WHERE dispatch_id = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong("dispatch_id") : null, ref);
        if (byDispatch != null) {
            return byDispatch;
        }
        return jdbc.query("""
                SELECT dispatch_id FROM dispatch_task WHERE task_id = ? ORDER BY dispatch_id LIMIT 1
                """, rs -> rs.next() ? rs.getLong("dispatch_id") : null, ref);
    }

    private Long lookupStepFromDispatch(Long dispatchId) {
        if (dispatchId == null) {
            return null;
        }
        return jdbc.query("""
                SELECT step_id FROM dispatch_task WHERE dispatch_id = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong("step_id") : null, dispatchId);
    }

    private Long lookupDefaultStep(Long workOrderId) {
        return jdbc.query("""
                SELECT step_id FROM production_task WHERE work_order_id = ? ORDER BY task_id LIMIT 1
                """, rs -> rs.next() ? rs.getLong("step_id") : null, workOrderId);
    }

    private Long resolveWorkOrderId(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return jdbc.query("""
                SELECT work_order_id FROM work_order WHERE work_order_no = ? AND is_deleted = 0 LIMIT 1
                """, rs -> rs.next() ? rs.getLong("work_order_id") : null, code.trim());
    }

    private Long resolveOperatorId(Object userName) {
        if (userName == null || String.valueOf(userName).isBlank()) {
            return null;
        }
        return jdbc.query("""
                SELECT user_id FROM sys_user WHERE username = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong("user_id") : null, String.valueOf(userName).trim());
    }

    private static Long longOrNull(Object v) {
        if (v == null || "".equals(String.valueOf(v))) {
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
}
