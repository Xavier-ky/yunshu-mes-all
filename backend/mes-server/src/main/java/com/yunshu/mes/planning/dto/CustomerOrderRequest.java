package com.yunshu.mes.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 客户订单创建/更新请求。
 */
public record CustomerOrderRequest(
        @NotBlank @Size(max = 64) String orderNo,
        @NotBlank @Size(max = 100) String customerName,
        @NotNull Long productId,
        @NotNull Long orderQty,
        @NotBlank @Size(max = 32) String deliveryDate,
        @Size(max = 32) String status
) {
    public CustomerOrderRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
