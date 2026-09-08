package com.yunshu.mes.factory.vo;

public record WorkstationVO(
        Long stationId,
        String stationCode,
        String stationName,
        String stationType,
        Long lineId,
        String lineName,
        String status
) {
}
