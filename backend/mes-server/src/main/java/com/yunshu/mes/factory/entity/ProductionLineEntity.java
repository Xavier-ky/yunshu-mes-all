package com.yunshu.mes.factory.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产线实体 — 对应 production_line 表。
 */
public class ProductionLineEntity {

    private Long lineId;
    private Long workshopId;
    private String lineCode;
    private String lineName;
    private BigDecimal ratedCapacity;
    private String capacityUnit;
    private String status;
    private java.math.BigDecimal modelPosX;
    private java.math.BigDecimal modelPosY;
    private java.math.BigDecimal modelPosZ;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductionLineEntity() {
    }

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }

    public String getLineCode() { return lineCode; }
    public void setLineCode(String lineCode) { this.lineCode = lineCode; }

    public String getLineName() { return lineName; }
    public void setLineName(String lineName) { this.lineName = lineName; }

    public BigDecimal getRatedCapacity() { return ratedCapacity; }
    public void setRatedCapacity(BigDecimal ratedCapacity) { this.ratedCapacity = ratedCapacity; }

    public String getCapacityUnit() { return capacityUnit; }
    public void setCapacityUnit(String capacityUnit) { this.capacityUnit = capacityUnit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.math.BigDecimal getModelPosX() { return modelPosX; }
    public void setModelPosX(java.math.BigDecimal modelPosX) { this.modelPosX = modelPosX; }

    public java.math.BigDecimal getModelPosY() { return modelPosY; }
    public void setModelPosY(java.math.BigDecimal modelPosY) { this.modelPosY = modelPosY; }

    public java.math.BigDecimal getModelPosZ() { return modelPosZ; }
    public void setModelPosZ(java.math.BigDecimal modelPosZ) { this.modelPosZ = modelPosZ; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
