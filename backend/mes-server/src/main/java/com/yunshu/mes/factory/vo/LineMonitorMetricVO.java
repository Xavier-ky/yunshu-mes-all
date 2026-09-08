package com.yunshu.mes.factory.vo;

public record LineMonitorMetricVO(
        String code,
        String label,
        String value,
        String unit,
        String tone
) {
}
