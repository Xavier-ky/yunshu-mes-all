package com.yunshu.mes.process.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 工艺路线创建/更新请求。
 */
public record RouteRequest(
        @NotBlank @Size(max = 64) String routeCode,
        @NotBlank @Size(max = 100) String routeName,
        @Size(max = 32) String routeVersion,
        @Size(max = 32) String status
) {
    public RouteRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
