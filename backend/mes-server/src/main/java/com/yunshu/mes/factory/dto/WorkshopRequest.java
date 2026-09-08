package com.yunshu.mes.factory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 车间创建/更新请求。
 */
public record WorkshopRequest(
        @NotBlank @Size(max = 64) String workshopCode,
        @NotBlank @Size(max = 100) String workshopName,
        @Size(max = 32) String status
) {
    public WorkshopRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
