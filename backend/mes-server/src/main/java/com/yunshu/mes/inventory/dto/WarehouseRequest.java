package com.yunshu.mes.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 仓库创建/更新请求。
 */
public record WarehouseRequest(
        @NotBlank @Size(max = 64) String warehouseCode,
        @NotBlank @Size(max = 100) String warehouseName,
        @NotBlank @Size(max = 32) String warehouseType,
        @Size(max = 32) String status
) {
    public WarehouseRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
