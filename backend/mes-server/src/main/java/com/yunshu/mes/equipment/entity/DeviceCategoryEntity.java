package com.yunshu.mes.equipment.entity;

import java.time.LocalDateTime;

/**
 * 设备类别实体 — 对应 device_category 表。
 */
public class DeviceCategoryEntity {

    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DeviceCategoryEntity() {
    }

    public DeviceCategoryEntity(Long categoryId, String categoryCode, String categoryName, String status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.categoryId = categoryId;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
