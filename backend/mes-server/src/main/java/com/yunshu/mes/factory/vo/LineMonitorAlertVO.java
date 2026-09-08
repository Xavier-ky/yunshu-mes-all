package com.yunshu.mes.factory.vo;

public record LineMonitorAlertVO(
        Long andonId,
        String andonNo,
        Long lineId,
        String lineName,
        String message,
        String status,
        String occurTime,
        String typeName,
        String typeCode,
        String priority,
        String exceptionDesc,
        long durationMinutes
) {
}
