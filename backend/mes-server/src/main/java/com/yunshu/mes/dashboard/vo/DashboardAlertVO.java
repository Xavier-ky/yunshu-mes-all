package com.yunshu.mes.dashboard.vo;

public record DashboardAlertVO(
        String alertType,
        String alertTitle,
        String level,
        String owner,
        String status
) {
}
