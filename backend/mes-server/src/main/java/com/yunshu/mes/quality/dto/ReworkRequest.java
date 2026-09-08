package com.yunshu.mes.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReworkRequest(
        @NotBlank @Size(max = 64) String reworkNo,
        Long defectId,
        Long snId,
        Long workOrderId,
        @NotBlank @Size(max = 500) String reworkReason,
        @Size(max = 32) String status
) {
    public ReworkRequest {
        if (status == null || status.isBlank()) status = "CREATED";
    }
}
