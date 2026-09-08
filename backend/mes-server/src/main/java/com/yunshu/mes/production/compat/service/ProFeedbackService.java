package com.yunshu.mes.production.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import com.yunshu.mes.planning.repository.DispatchTaskRepository;
import com.yunshu.mes.planning.workflow.WorkOrderLifecycleService;
import com.yunshu.mes.production.workflow.ProductionReportSyncService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProFeedbackService {

    private final JdbcTemplate jdbc;
    private final DispatchTaskRepository dispatchTaskRepository;
    private final WorkOrderLifecycleService lifecycle;
    private final ProductionReportSyncService reportSync;

    public ProFeedbackService(
            JdbcTemplate jdbc,
            DispatchTaskRepository dispatchTaskRepository,
            WorkOrderLifecycleService lifecycle,
            ProductionReportSyncService reportSync) {
        this.jdbc = jdbc;
        this.dispatchTaskRepository = dispatchTaskRepository;
        this.lifecycle = lifecycle;
        this.reportSync = reportSync;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "pro_feedback", "record_id", MdDocSchemas.PRO_FEEDBACK,
                MdDocSchemas.filter(params, "feedbackType", "workorderCode", "workstationName", "itemCode", "status", "userName", "nickName"),
                PageUtil.offset(pn, ps), ps);
    }

    public long count(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "pro_feedback", MdDocSchemas.PRO_FEEDBACK,
                MdDocSchemas.filter(params, "feedbackType", "workorderCode", "workstationName", "itemCode", "status", "userName", "nickName"));
    }

    public Map<String, Object> getById(Long id) {
        return WmSqlHelper.getById(jdbc, "pro_feedback", "record_id", "recordId", MdDocSchemas.PRO_FEEDBACK, id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        if (body.get("feedbackTime") == null) {
            body.put("feedbackTime", Timestamp.valueOf(LocalDateTime.now()));
        }
        if (body.get("status") == null) {
            body.put("status", "PREPARE");
        }
        Long recordId = WmSqlHelper.insert(jdbc, "pro_feedback", MdDocSchemas.PRO_FEEDBACK, body);
        return recordId;
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("recordId")));
        return WmSqlHelper.update(jdbc, "pro_feedback", "record_id", MdDocSchemas.PRO_FEEDBACK, body, id);
    }

    @Transactional
    public int delete(Long id) {
        return WmSqlHelper.delete(jdbc, "pro_feedback", "record_id", id);
    }

    @Transactional
    public int execute(Long recordId) {
        Map<String, Object> fb = getById(recordId);
        if (fb == null) {
            throw new IllegalArgumentException("报工单不存在");
        }
        String currentStatus = String.valueOf(fb.getOrDefault("status", ""));
        if ("FINISHED".equalsIgnoreCase(currentStatus)) {
            syncDispatchProgressFromFeedback(fb);
            return 1;
        }
        Long workOrderId = longOrNull(fb.get("workorderId"));
        if (workOrderId == null) {
            workOrderId = lifecycle.resolveWorkOrderIdFromCode(String.valueOf(fb.get("workorderCode")));
        }
        lifecycle.validateFeedbackAllowed(workOrderId);

        Object qty = fb.get("quantityQualified");
        double uncheck = 0;
        if (qty != null) {
            uncheck = Double.parseDouble(String.valueOf(qty));
        }
        int rows = jdbc.update("""
                UPDATE pro_feedback SET status = 'FINISHED',
                  quantity_uncheck = ?,
                  update_time = ?
                WHERE record_id = ?""",
                uncheck, Timestamp.valueOf(LocalDateTime.now()), recordId);
        syncDispatchProgressFromFeedback(fb);
        if (workOrderId != null) {
            lifecycle.advanceIfNullOrEarlier(workOrderId, WorkOrderLifecycleService.IN_PROGRESS);
            lifecycle.advanceIfNullOrEarlier(workOrderId, WorkOrderLifecycleService.QC_PENDING);
        }
        reportSync.syncFromFeedback(recordId, fb);
        return rows;
    }

    /** 以已执行报工汇总为唯一来源，回写派工单与生产任务完成数 */
    private void syncDispatchProgressFromFeedback(Map<String, Object> fb) {
        Object taskId = fb.get("taskId");
        if (taskId == null) {
            return;
        }
        long taskRef = Long.parseLong(String.valueOf(taskId));
        Long dispatchId = resolveDispatchId(taskRef);
        if (dispatchId == null) {
            return;
        }
        Double total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(pf.quantity_qualified), 0)
                FROM pro_feedback pf
                INNER JOIN dispatch_task dt ON dt.dispatch_id = ?
                WHERE pf.status = 'FINISHED'
                  AND (pf.task_id = dt.dispatch_id OR pf.task_id = dt.task_id)
                """, Double.class, dispatchId);
        double completed = total != null ? total : 0;
        dispatchTaskRepository.setCompletedQty(dispatchId, completed);
        Long productionTaskId = jdbc.query("""
                SELECT task_id FROM dispatch_task WHERE dispatch_id = ?
                """, rs -> rs.next() ? rs.getLong("task_id") : null, dispatchId);
        if (productionTaskId != null) {
            jdbc.update("""
                    UPDATE production_task SET
                      completed_qty = ?,
                      status = CASE
                        WHEN ? >= IFNULL(task_qty, 0) THEN 'COMPLETED'
                        ELSE 'RUNNING'
                      END
                    WHERE task_id = ?
                    """, completed, completed, productionTaskId);
        }
    }

    private Long resolveDispatchId(long taskRef) {
        List<Long> byDispatch = jdbc.query(
                "SELECT dispatch_id FROM dispatch_task WHERE dispatch_id = ? LIMIT 1",
                (rs, n) -> rs.getLong("dispatch_id"), taskRef);
        if (!byDispatch.isEmpty()) {
            return byDispatch.get(0);
        }
        List<Long> byTask = jdbc.query(
                "SELECT dispatch_id FROM dispatch_task WHERE task_id = ? ORDER BY dispatch_id LIMIT 1",
                (rs, n) -> rs.getLong("dispatch_id"), taskRef);
        return byTask.isEmpty() ? null : byTask.get(0);
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
