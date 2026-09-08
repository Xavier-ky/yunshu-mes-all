package com.yunshu.mes.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * BOM创建/更新请求。
 */
public record BomRequest(
        @NotBlank @Size(max = 64) String bomCode,
        @NotBlank @Size(max = 200) String bomName,
        @NotNull Long productId,
        @NotBlank @Size(max = 32) String bomVersion,
        @Size(max = 32) String status
) {
    public BomRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
