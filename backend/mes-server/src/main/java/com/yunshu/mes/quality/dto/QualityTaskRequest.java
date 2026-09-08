package com.yunshu.mes.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QualityTaskRequest(
        @NotBlank @Size(max = 64) String qualityTaskNo,
        @NotNull Long productId,
        @NotBlank @Size(max = 32) String inspectType,
        Long workOrderId,
        @Size(max = 32) String status
) {
    public QualityTaskRequest {
        if (status == null || status.isBlank()) status = "CREATED";
    }
}
