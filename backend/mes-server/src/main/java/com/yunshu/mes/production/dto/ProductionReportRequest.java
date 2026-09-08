package com.yunshu.mes.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductionReportRequest(
        @NotBlank @Size(max = 64) String reportNo,
        @NotNull Long workOrderId,
        @NotNull Long stepId,
        @NotNull Long operatorId,
        Long snId,
        @Size(max = 32) String reportType,
        @Size(max = 18) String goodQty,
        @Size(max = 18) String defectQty,
        @Size(max = 500) String remark
) {
    public ProductionReportRequest {
        if (reportType == null || reportType.isBlank()) {
            reportType = "NORMAL";
        }
        if (goodQty == null || goodQty.isBlank()) {
            goodQty = "0";
        }
        if (defectQty == null || defectQty.isBlank()) {
            defectQty = "0";
        }
    }
}
