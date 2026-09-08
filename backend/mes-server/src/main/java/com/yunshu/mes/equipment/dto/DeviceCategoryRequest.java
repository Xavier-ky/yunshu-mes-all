package com.yunshu.mes.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 设备类别创建/更新请求。
 */
public record DeviceCategoryRequest(
        @NotBlank @Size(max = 64) String categoryCode,
        @NotBlank @Size(max = 100) String categoryName,
        @Size(max = 32) String status
) {
    public DeviceCategoryRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
