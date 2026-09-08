package com.yunshu.mes.planning.compat.service;

import com.yunshu.mes.planning.config.WorkflowProperties;
import com.yunshu.mes.planning.repository.DispatchTaskRepository;
import com.yunshu.mes.planning.workflow.WorkOrderLifecycleService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 甘特排产（production_task）→ 现场派工（dispatch_task）同步。
 */
@Service
public class DispatchSyncService {

    private final JdbcTemplate jdbc;
    private final DispatchTaskRepository dispatchRepo;
    private final WorkOrderLifecycleService lifecycle;
    private final WorkflowProperties workflowProperties;

    public DispatchSyncService(
            JdbcTemplate jdbc,
            DispatchTaskRepository dispatchRepo,
            WorkOrderLifecycleService lifecycle,
            WorkflowProperties workflowProperties) {
        this.jdbc = jdbc;
        this.dispatchRepo = dispatchRepo;
        this.lifecycle = lifecycle;
        this.workflowProperties = workflowProperties;
    }

    public void syncFromProductionTask(Long taskId) {
        if (taskId == null || !workflowProperties.isAutoDispatch()) {
            return;
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT pt.task_id, pt.task_no, pt.work_order_id, pt.step_id, pt.workstation_id,
                       pt.task_qty, pt.start_time, pt.end_time, pt.status
                FROM production_task pt WHERE pt.task_id = ?
                """, taskId);
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> task = rows.get(0);
        Long workOrderId = longVal(task.get("work_order_id"));
        Long stepId = longVal(task.get("step_id"));
        if (stepId == null && workOrderId != null) {
            stepId = resolveDefaultStepId(workOrderId);
        }
        if (stepId == null) {
            return;
        }
        Long stationId = longVal(task.get("workstation_id"));
        BigDecimal plannedQty = decimal(task.get("task_qty"), BigDecimal.ONE);
        String taskNo = stringVal(task.get("task_no"), "PT-" + taskId);
        String dispatchNo = "DT-" + taskNo;

        Long operatorId = resolveDefaultOperatorId();
        String status = mapDispatchStatus(stringVal(task.get("status"), "CREATED"));

        Long existingId = findDispatchIdByTaskId(taskId);
        if (existingId != null) {
            dispatchRepo.update(existingId, dispatchNo, taskId, workOrderId, plannedQty,
                    stepId, stationId, operatorId, status);
        } else {
            dispatchRepo.insert(dispatchNo, taskId, workOrderId, plannedQty, stepId, stationId, operatorId, status);
        }
        if (workOrderId != null) {
            lifecycle.advanceIfNullOrEarlier(workOrderId, WorkOrderLifecycleService.SCHEDULED);
        }
    }

    private Long findDispatchIdByTaskId(Long taskId) {
        List<Long> ids = jdbc.query("""
                SELECT dispatch_id FROM dispatch_task WHERE task_id = ? ORDER BY dispatch_id LIMIT 1
                """, (rs, n) -> rs.getLong("dispatch_id"), taskId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long resolveDefaultStepId(Long workOrderId) {
        return jdbc.query("""
                SELECT prs.step_id
                FROM work_order wo
                JOIN process_route_step prs ON prs.route_id = wo.route_id
                WHERE wo.work_order_id = ?
                ORDER BY prs.step_seq, prs.route_step_id
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("step_id") : null, workOrderId);
    }

    private Long resolveDefaultOperatorId() {
        return jdbc.query("""
                SELECT user_id FROM sys_user WHERE username IN ('worker01', 'worker') LIMIT 1
                """, rs -> rs.next() ? rs.getLong("user_id") : null);
    }

    private static String mapDispatchStatus(String taskStatus) {
        if (!StringUtils.hasText(taskStatus)) {
            return "CREATED";
        }
        return switch (taskStatus.toUpperCase()) {
            case "RUNNING", "NORMAL" -> "CREATED";
            case "COMPLETED", "FINISHED" -> "COMPLETED";
            default -> "CREATED";
        };
    }

    private static Long longVal(Object v) {
        if (v == null || !StringUtils.hasText(String.valueOf(v))) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    private static BigDecimal decimal(Object v, BigDecimal def) {
        if (v == null) {
            return def;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(String.valueOf(v));
    }

    private static String stringVal(Object v, String def) {
        return v == null || !StringUtils.hasText(String.valueOf(v)) ? def : String.valueOf(v);
    }
}
