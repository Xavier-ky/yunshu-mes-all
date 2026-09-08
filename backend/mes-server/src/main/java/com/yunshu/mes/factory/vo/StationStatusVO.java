package com.yunshu.mes.factory.vo;

public record StationStatusVO(
        Long stationId,
        String stationCode,
        String stationName,
        String runStatus,
        String dispatchNo,
        String operatorName,
        double plannedQty,
        double completedQty,
        String deviceName,
        String deviceStatus
) {
}
