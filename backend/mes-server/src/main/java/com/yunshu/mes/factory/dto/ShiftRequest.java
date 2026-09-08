package com.yunshu.mes.factory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 班次创建/更新请求。
 */
public record ShiftRequest(
        @NotBlank @Size(max = 64) String shiftCode,
        @NotBlank @Size(max = 100) String shiftName,
        @NotBlank String startTime,
        @NotBlank String endTime,
        @Size(max = 32) String status
) {
    public ShiftRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
