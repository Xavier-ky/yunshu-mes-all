package com.yunshu.mes.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequisitionRequest(
        @NotBlank String requisitionNo,
        @NotNull Long workOrderId,
        @NotNull Long requestUserId,
        String status,
        String remark
) {
    public RequisitionRequest {
        if (status == null || status.isBlank()) status = "CREATED";
        if (remark == null) remark = "";
    }
}

record RequisitionItemRequest(
        @NotNull Long materialId,
        @NotNull java.math.BigDecimal requestQty
) {}
