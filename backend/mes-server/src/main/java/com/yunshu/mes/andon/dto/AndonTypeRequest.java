package com.yunshu.mes.andon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 安灯类型创建/更新请求。
 */
public record AndonTypeRequest(
        @NotBlank @Size(max = 64) String typeCode,
        @NotBlank @Size(max = 100) String typeName,
        @Size(max = 32) String status
) {
    public AndonTypeRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
