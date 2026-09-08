package com.yunshu.mes.dashboard.vo;

import java.util.List;

public record DashboardSummaryVO(
        List<DashboardMetricVO> metrics,
        List<DashboardWorkOrderVO> workOrders,
        List<DashboardAlertVO> alerts
) {
}
