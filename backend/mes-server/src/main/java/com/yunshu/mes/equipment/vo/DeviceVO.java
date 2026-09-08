package com.yunshu.mes.equipment.vo;

public record DeviceVO(
        Long deviceId,
        String deviceCode,
        String deviceName,
        String categoryName,
        String lineName,
        String stationName,
        String status
) {
}
