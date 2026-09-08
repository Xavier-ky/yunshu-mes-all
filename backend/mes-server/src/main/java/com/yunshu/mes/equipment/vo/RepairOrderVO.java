package com.yunshu.mes.equipment.vo;

public record RepairOrderVO(
        Long repairOrderId,
        String repairNo,
        String deviceName,
        String faultDesc,
        String status,
        String reportTime
) {
}
