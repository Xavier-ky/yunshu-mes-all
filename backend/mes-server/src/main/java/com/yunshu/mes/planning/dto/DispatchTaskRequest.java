package com.yunshu.mes.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 派工单创建/更新请求。
 */
public record DispatchTaskRequest(
        @NotBlank @Size(max = 64) String dispatchNo,
        @NotNull Long taskId,
        Long stepId,
        Long stationId,
        Long assigneeId,
        @Size(max = 32) String status
) {
    public DispatchTaskRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
