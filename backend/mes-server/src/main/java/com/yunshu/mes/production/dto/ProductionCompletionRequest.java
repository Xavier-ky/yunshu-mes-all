package com.yunshu.mes.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductionCompletionRequest(
        @NotBlank @Size(max = 64) String completionNo,
        @NotNull Long workOrderId,
        @Size(max = 18) String completedQty,
        @Size(max = 18) String defectQty,
        @Size(max = 32) String status
) {
    public ProductionCompletionRequest {
        if (completedQty == null || completedQty.isBlank()) {
            completedQty = "0";
        }
        if (defectQty == null || defectQty.isBlank()) {
            defectQty = "0";
        }
        if (status == null || status.isBlank()) {
            status = "CREATED";
        }
    }
}
