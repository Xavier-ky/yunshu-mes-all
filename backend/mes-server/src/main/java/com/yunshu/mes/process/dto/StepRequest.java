package com.yunshu.mes.process.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 工序创建/更新请求。
 */
public record StepRequest(
        @NotBlank @Size(max = 64) String stepCode,
        @NotBlank @Size(max = 100) String stepName,
        @Size(max = 32) String stepType,
        @Size(max = 32) String standardHours,
        @Size(max = 32) String status
) {
    public StepRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
