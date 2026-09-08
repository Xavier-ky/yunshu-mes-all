package com.yunshu.mes.production.vo;

public record ProductionReportVO(
        Long reportId,
        String reportNo,
        String workOrderNo,
        String stepName,
        String reportType,
        String goodQty,
        String defectQty,
        String reportTime
) {
}
