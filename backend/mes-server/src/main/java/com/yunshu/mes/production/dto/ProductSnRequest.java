package com.yunshu.mes.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductSnRequest(
        @NotBlank @Size(max = 128) String snCode,
        @NotNull Long productId,
        @NotNull Long workOrderId,
        Long barcodeId,
        @Size(max = 32) String status
) {
    public ProductSnRequest {
        if (status == null || status.isBlank()) {
            status = "CREATED";
        }
    }
}
