package com.yunshu.mes.integration.entity;

import java.time.LocalDateTime;

/**
 * 外部系统实体 — 对应 external_system 表。
 */
public class ExternalSystemEntity {

    private Long systemId;
    private String systemCode;
    private String systemName;
    private String systemType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ExternalSystemEntity() {
    }

    public ExternalSystemEntity(Long systemId, String systemCode, String systemName, String systemType,
                                String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.systemId = systemId;
        this.systemCode = systemCode;
        this.systemName = systemName;
        this.systemType = systemType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getSystemId() { return systemId; }
    public void setSystemId(Long systemId) { this.systemId = systemId; }

    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public String getSystemType() { return systemType; }
    public void setSystemType(String systemType) { this.systemType = systemType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
