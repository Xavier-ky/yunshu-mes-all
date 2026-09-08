package com.yunshu.mes.masterdata.entity;

import java.time.LocalDateTime;

/**
 * BOM实体 — 对应 bom 表。
 */
public class BomEntity {

    private Long bomId;
    private String bomCode;
    private String bomName;
    private Long productId;
    private String bomVersion;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BomEntity() {
    }

    public BomEntity(Long bomId, String bomCode, String bomName, Long productId,
                     String bomVersion, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.bomId = bomId;
        this.bomCode = bomCode;
        this.bomName = bomName;
        this.productId = productId;
        this.bomVersion = bomVersion;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getBomId() { return bomId; }
    public void setBomId(Long bomId) { this.bomId = bomId; }

    public String getBomCode() { return bomCode; }
    public void setBomCode(String bomCode) { this.bomCode = bomCode; }

    public String getBomName() { return bomName; }
    public void setBomName(String bomName) { this.bomName = bomName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getBomVersion() { return bomVersion; }
    public void setBomVersion(String bomVersion) { this.bomVersion = bomVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
