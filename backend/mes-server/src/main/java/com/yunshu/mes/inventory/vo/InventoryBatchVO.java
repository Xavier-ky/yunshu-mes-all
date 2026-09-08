package com.yunshu.mes.inventory.vo;

public record InventoryBatchVO(
        Long batchId,
        String batchNo,
        String materialName,
        String warehouseName,
        Long availableQty,
        String status
) {
}
