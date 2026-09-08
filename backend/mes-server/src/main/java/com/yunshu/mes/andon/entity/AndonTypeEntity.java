package com.yunshu.mes.andon.entity;

import java.time.LocalDateTime;

/**
 * 安灯类型实体 — 对应 andon_type 表。
 */
public class AndonTypeEntity {

    private Long typeId;
    private String typeCode;
    private String typeName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AndonTypeEntity() {
    }

    public AndonTypeEntity(Long typeId, String typeCode, String typeName, String status,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.typeId = typeId;
        this.typeCode = typeCode;
        this.typeName = typeName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
