package com.yunshu.mes.factory.vo;

public record LineVO(
        Long lineId,
        String lineCode,
        String lineName,
        String workshopName,
        String ratedCapacity,
        String capacityUnit,
        String status,
        Double modelPosX,
        Double modelPosY,
        Double modelPosZ
) {
}
