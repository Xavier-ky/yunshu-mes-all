package com.yunshu.mes.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 库存批次创建/更新请求。
 */
public record InventoryBatchRequest(
        @NotBlank @Size(max = 64) String batchNo,
        @NotNull Long materialId,
        @NotNull Long warehouseId,
        Long locationId,
        @NotNull Long availableQty,
        @Size(max = 32) String status
) {
    public InventoryBatchRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
