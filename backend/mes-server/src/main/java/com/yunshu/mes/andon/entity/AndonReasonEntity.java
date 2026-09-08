package com.yunshu.mes.andon.entity;

import java.time.LocalDateTime;

/**
 * 安灯原因实体 — 对应 andon_reason 表。
 */
public class AndonReasonEntity {

    private Long reasonId;
    private String reasonCode;
    private String reasonName;
    private Long typeId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AndonReasonEntity() {
    }

    public AndonReasonEntity(Long reasonId, String reasonCode, String reasonName, Long typeId, String status,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.reasonId = reasonId;
        this.reasonCode = reasonCode;
        this.reasonName = reasonName;
        this.typeId = typeId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getReasonId() { return reasonId; }
    public void setReasonId(Long reasonId) { this.reasonId = reasonId; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getReasonName() { return reasonName; }
    public void setReasonName(String reasonName) { this.reasonName = reasonName; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
