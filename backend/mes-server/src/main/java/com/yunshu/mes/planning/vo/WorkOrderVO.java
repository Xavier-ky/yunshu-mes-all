package com.yunshu.mes.planning.vo;

public record WorkOrderVO(
        Long workOrderId,
        String workOrderNo,
        Long orderId,
        String orderNo,
        Long productId,
        String productName,
        Long planQty,
        Long completedQty,
        String status,
        String kittingStatus
) {
}
