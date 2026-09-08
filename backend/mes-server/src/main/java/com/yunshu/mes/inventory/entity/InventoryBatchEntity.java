package com.yunshu.mes.inventory.entity;

import java.time.LocalDateTime;

/**
 * 库存批次实体 — 对应 inventory_batch 表。
 */
public class InventoryBatchEntity {

    private Long batchId;
    private String batchNo;
    private Long materialId;
    private Long warehouseId;
    private Long locationId;
    private Long availableQty;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InventoryBatchEntity() {
    }

    public InventoryBatchEntity(Long batchId, String batchNo, Long materialId, Long warehouseId,
                                Long locationId, Long availableQty, String status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.batchId = batchId;
        this.batchNo = batchNo;
        this.materialId = materialId;
        this.warehouseId = warehouseId;
        this.locationId = locationId;
        this.availableQty = availableQty;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Long getAvailableQty() { return availableQty; }
    public void setAvailableQty(Long availableQty) { this.availableQty = availableQty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
