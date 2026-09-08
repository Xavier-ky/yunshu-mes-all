package com.yunshu.mes.dashboard.vo;

public record DashboardWorkOrderVO(
        String workOrderNo,
        String productName,
        String lineName,
        String planQty,
        String completedQty,
        String status
) {
}
