package com.yunshu.mes.masterdata.entity;

import java.time.LocalDateTime;

/**
 * 计量单位实体 — 对应 uom 表。
 */
public class UomEntity {

    private Long unitId;
    private String unitCode;
    private String unitName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UomEntity() {
    }

    public UomEntity(Long unitId, String unitCode, String unitName, String status,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
