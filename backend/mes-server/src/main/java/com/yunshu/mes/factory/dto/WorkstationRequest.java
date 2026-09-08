package com.yunshu.mes.factory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 工位创建/更新请求。
 */
public record WorkstationRequest(
        @NotNull Long lineId,
        @NotBlank @Size(max = 64) String stationCode,
        @NotBlank @Size(max = 100) String stationName,
        @Size(max = 32) String stationType,
        @Size(max = 32) String status
) {
    public WorkstationRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
