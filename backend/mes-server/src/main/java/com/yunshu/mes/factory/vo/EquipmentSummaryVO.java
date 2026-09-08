package com.yunshu.mes.factory.vo;

public record EquipmentSummaryVO(
        long total,
        long runningCount,
        double runningRate,
        long faultCount,
        double avgOee
) {
}
