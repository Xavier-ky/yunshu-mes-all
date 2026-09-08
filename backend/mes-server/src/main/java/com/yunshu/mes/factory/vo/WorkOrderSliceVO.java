package com.yunshu.mes.factory.vo;

public record WorkOrderSliceVO(
        Long workOrderId,
        String workOrderNo,
        String productName,
        double planQty,
        double completedQty,
        String status
) {
}
