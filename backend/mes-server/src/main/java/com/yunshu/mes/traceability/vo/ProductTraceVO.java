package com.yunshu.mes.traceability.vo;

public record ProductTraceVO(
        Long snId,
        String snCode,
        String productName,
        String workOrderNo,
        String status
) {
}
