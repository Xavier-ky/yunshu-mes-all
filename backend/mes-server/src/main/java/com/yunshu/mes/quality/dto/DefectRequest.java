package com.yunshu.mes.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DefectRequest(
        @NotBlank @Size(max = 64) String defectNo,
        Long snId,
        Long workOrderId,
        Long stepId,
        @NotBlank @Size(max = 500) String defectDesc,
        @Size(max = 32) String severity,
        @Size(max = 32) String status
) {
    public DefectRequest {
        if (severity == null || severity.isBlank()) severity = "NORMAL";
        if (status == null || status.isBlank()) status = "OPEN";
    }
}
