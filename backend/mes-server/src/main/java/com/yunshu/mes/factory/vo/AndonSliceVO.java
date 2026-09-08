package com.yunshu.mes.factory.vo;

public record AndonSliceVO(
        Long andonId,
        String andonNo,
        String typeName,
        String stationName,
        String exceptionDesc,
        String status,
        String occurTime
) {
}
