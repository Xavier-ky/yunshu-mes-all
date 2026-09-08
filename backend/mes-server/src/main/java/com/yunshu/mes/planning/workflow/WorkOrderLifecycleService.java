package com.yunshu.mes.planning.workflow;

import com.yunshu.mes.planning.config.WorkflowProperties;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 工单 lifecycle_status 写回与门禁校验（compat 主线）。
 */
@Service
public class WorkOrderLifecycleService {

    public static final String DRAFT = "DRAFT";
    public static final String RELEASED = "RELEASED";
    public static final String SCHEDULED = "SCHEDULED";
    public static final String KITTING_OK = "KITTING_OK";
    public static final String MATERIAL_ISSUED = "MATERIAL_ISSUED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String QC_PENDING = "QC_PENDING";
    public static final String QC_PASSED = "QC_PASSED";
    public static final String QC_FAILED = "QC_FAILED";
    public static final String COMPLETED = "COMPLETED";

    private final JdbcTemplate jdbc;
    private final WorkflowProperties properties;

    public WorkOrderLifecycleService(JdbcTemplate jdbc, WorkflowProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    public String getLifecycle(Long workOrderId) {
        if (workOrderId == null) {
            return null;
        }
        return jdbc.query("""
                SELECT lifecycle_status FROM work_order WHERE work_order_id = ? AND is_deleted = 0
                """, rs -> rs.next() ? rs.getString("lifecycle_status") : null, workOrderId);
    }

    public void advance(Long workOrderId, String targetStatus) {
        if (workOrderId == null || !StringUtils.hasText(targetStatus)) {
            return;
        }
        jdbc.update("""
                UPDATE work_order SET lifecycle_status = ?, updated_at = NOW(3)
                WHERE work_order_id = ? AND is_deleted = 0
                """, targetStatus, workOrderId);
    }

    public void advanceIfNullOrEarlier(Long workOrderId, String targetStatus) {
        String current = getLifecycle(workOrderId);
        if (current == null || rank(targetStatus) >= rank(current)) {
            advance(workOrderId, targetStatus);
        }
    }

    /**
     * 成品入库门禁：lifecycle 已设置的工单必须 QC_PASSED（或已完成）。
     * 存量工单 lifecycle 为 null 时不拦截。
     */
    public void validateProductRecpt(Long workOrderId) {
        if (workOrderId == null) {
            return;
        }
        String ls = getLifecycle(workOrderId);
        if (ls == null) {
            return;
        }
        if (!QC_PASSED.equals(ls) && !COMPLETED.equals(ls)) {
            throw new IllegalArgumentException(
                    "工单须完成过程质检(QC_PASSED)后方可成品入库，当前 lifecycle=" + ls);
        }
    }

    /**
     * lifecycle 已设置时始终校验（不依赖 enforce 开关）。
     */
    public void validateFeedbackAllowed(Long workOrderId) {
        if (workOrderId == null) {
            return;
        }
        String ls = getLifecycle(workOrderId);
        if (ls == null) {
            return;
        }
        if (!MATERIAL_ISSUED.equals(ls) && !IN_PROGRESS.equals(ls) && !QC_PENDING.equals(ls)) {
            throw new IllegalArgumentException(
                    "工单尚未领料或不在报工阶段，当前 lifecycle=" + ls);
        }
    }

    public void validateIssueAllowed(Long workOrderId) {
        if (workOrderId == null) {
            return;
        }
        String ls = getLifecycle(workOrderId);
        if (ls == null) {
            return;
        }
        if (!RELEASED.equals(ls) && !KITTING_OK.equals(ls) && !SCHEDULED.equals(ls)) {
            throw new IllegalArgumentException(
                    "工单当前状态不允许领料，lifecycle=" + ls);
        }
    }

    public Long resolveWorkOrderIdFromCode(String workOrderCode) {
        if (!StringUtils.hasText(workOrderCode)) {
            return null;
        }
        return jdbc.query("""
                SELECT work_order_id FROM work_order WHERE work_order_no = ? AND is_deleted = 0 LIMIT 1
                """, rs -> rs.next() ? rs.getLong("work_order_id") : null, workOrderCode.trim());
    }

    private static int rank(String status) {
        return switch (Objects.requireNonNullElse(status, "")) {
            case DRAFT -> 0;
            case RELEASED -> 1;
            case KITTING_OK -> 2;
            case SCHEDULED -> 3;
            case MATERIAL_ISSUED -> 4;
            case IN_PROGRESS -> 5;
            case QC_PENDING -> 6;
            case QC_PASSED -> 7;
            case QC_FAILED -> 7;
            case COMPLETED -> 8;
            default -> -1;
        };
    }
}
