package com.yunshu.mes.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 设备台账创建/更新请求。
 */
public record DeviceRequest(
        @NotBlank @Size(max = 64) String deviceCode,
        @NotBlank @Size(max = 100) String deviceName,
        Long categoryId,
        Long lineId,
        Long stationId,
        @Size(max = 32) String status
) {
    public DeviceRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
