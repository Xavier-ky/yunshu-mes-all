package com.yunshu.mes.factory.vo;

public record DispatchSliceVO(
        Long dispatchId,
        String dispatchNo,
        String stationName,
        String stepName,
        String operatorName,
        double plannedQty,
        double completedQty,
        String status
) {
}
