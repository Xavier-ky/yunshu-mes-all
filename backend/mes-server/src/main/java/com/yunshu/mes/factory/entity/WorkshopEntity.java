package com.yunshu.mes.factory.entity;

import java.time.LocalDateTime;

/**
 * 车间实体 — 对应 workshop 表。
 */
public class WorkshopEntity {

    private Long workshopId;
    private String workshopCode;
    private String workshopName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WorkshopEntity() {
    }

    public WorkshopEntity(Long workshopId, String workshopCode, String workshopName, String status,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.workshopId = workshopId;
        this.workshopCode = workshopCode;
        this.workshopName = workshopName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }

    public String getWorkshopCode() { return workshopCode; }
    public void setWorkshopCode(String workshopCode) { this.workshopCode = workshopCode; }

    public String getWorkshopName() { return workshopName; }
    public void setWorkshopName(String workshopName) { this.workshopName = workshopName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
