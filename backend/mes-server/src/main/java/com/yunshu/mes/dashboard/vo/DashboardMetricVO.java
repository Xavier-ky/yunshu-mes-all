package com.yunshu.mes.dashboard.vo;

public record DashboardMetricVO(
        String name,
        String value,
        String unit,
        String trend,
        String status
) {
}
