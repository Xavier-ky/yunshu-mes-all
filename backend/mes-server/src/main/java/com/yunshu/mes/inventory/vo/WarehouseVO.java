package com.yunshu.mes.inventory.vo;

public record WarehouseVO(
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String warehouseType,
        String status
) {
}
