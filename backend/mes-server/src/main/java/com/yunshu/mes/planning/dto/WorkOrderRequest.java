package com.yunshu.mes.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 生产工单创建/更新请求。
 */
public record WorkOrderRequest(
        @NotBlank @Size(max = 64) String workOrderNo,
        @NotNull Long productId,
        Long orderId,
        Long orderItemId,
        @NotNull Long planQty,
        @Size(max = 32) String status
) {
    public WorkOrderRequest {
        if (status == null || status.isBlank()) {
            status = "CREATED";
        }
    }
}
