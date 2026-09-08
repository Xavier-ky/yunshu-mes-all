package com.yunshu.mes.factory.vo;

public record LineCardVO(
        Long lineId,
        String lineCode,
        String lineName,
        String workshopName,
        String lineStatus,
        String currentWorkOrderNo,
        String productName,
        double planQty,
        double completedQty,
        double progressPct,
        double oee,
        boolean oeeEstimated,
        StationStatusRatioVO stationRatio,
        int openAndonCount,
        Double modelPosX,
        Double modelPosY,
        Double modelPosZ
) {
}
