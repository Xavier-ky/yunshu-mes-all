package com.yunshu.mes.planning.vo;

/**
 * 派工单视图（含工位页扩展字段）。
 */
public record DispatchTaskVO(
        Long dispatchId,
        String dispatchNo,
        String taskNo,
        Long taskId,
        Long workOrderId,
        String workOrderNo,
        Long stepId,
        String stepCode,
        Long stationId,
        String stationCode,
        String stepName,
        String stationName,
        Long productId,
        String productCode,
        String productName,
        String lineName,
        String lineCode,
        Long operatorId,
        String assigneeName,
        Long plannedQty,
        Long completedQty,
        String status,
        String plannedStartTime,
        String plannedEndTime,
        String actualStartTime,
        String customerOrderNo,
        String createdAt
) {
    /** Mock / 测试用精简构造 */
    public static DispatchTaskVO basic(
            Long dispatchId, String dispatchNo, String taskNo, Long workOrderId,
            Long stepId, Long stationId, String stepName, String stationName,
            Long operatorId, String assigneeName, Long plannedQty, Long completedQty, String status) {
        return new DispatchTaskVO(
                dispatchId, dispatchNo, taskNo, null, workOrderId, null,
                stepId, null, stationId, null, stepName, stationName,
                null, null, null, null, null,
                operatorId, assigneeName, plannedQty, completedQty, status,
                null, null, null, null, null);
    }
}
