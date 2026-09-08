package com.yunshu.mes.masterdata.entity;

import java.time.LocalDateTime;

/**
 * 物料实体 — 对应 material 表。
 */
public class MaterialEntity {

    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialType;
    private Long unitId;
    private Integer isCritical;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MaterialEntity() {
    }

    public MaterialEntity(Long materialId, String materialCode, String materialName, String materialType,
                          Long unitId, Integer isCritical, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.materialId = materialId;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.materialType = materialType;
        this.unitId = unitId;
        this.isCritical = isCritical;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Integer getIsCritical() { return isCritical; }
    public void setIsCritical(Integer isCritical) { this.isCritical = isCritical; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
