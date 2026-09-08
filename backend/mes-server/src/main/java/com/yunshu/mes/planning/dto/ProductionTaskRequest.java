package com.yunshu.mes.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 生产任务创建/更新请求。
 */
public record ProductionTaskRequest(
        @NotBlank @Size(max = 64) String taskNo,
        @NotNull Long workOrderId,
        Long lineId,
        @NotNull Long planQty,
        @Size(max = 32) String status
) {
    public ProductionTaskRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
