package com.yunshu.mes.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 外部系统创建/更新请求。
 */
public record ExternalSystemRequest(
        @NotBlank @Size(max = 64) String systemCode,
        @NotBlank @Size(max = 100) String systemName,
        @Size(max = 32) String systemType,
        @Size(max = 32) String status
) {
    public ExternalSystemRequest {
        if (systemType == null || systemType.isBlank()) {
            systemType = "";
        }
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
