package com.yunshu.mes.planning.vo;

public record ProductionTaskVO(
        Long taskId,
        String taskNo,
        Long workOrderId,
        String workOrderNo,
        String lineName,
        Long planQty,
        String status
) {
}
