package com.yunshu.mes.production.vo;

public record ProductSnVO(
        Long snId,
        String snCode,
        String productName,
        String workOrderNo,
        String status
) {
}
