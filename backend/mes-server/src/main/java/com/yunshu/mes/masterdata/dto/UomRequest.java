package com.yunshu.mes.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 计量单位创建/更新请求。
 */
public record UomRequest(
        @NotBlank @Size(max = 32) String unitCode,
        @NotBlank @Size(max = 64) String unitName,
        @Size(max = 32) String status
) {
    public UomRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
