package com.yunshu.mes.planning.entity;

import java.time.LocalDateTime;

/**
 * 生产任务实体 — 对应 production_task 表。
 */
public class ProductionTaskEntity {

    private Long taskId;
    private String taskNo;
    private Long workOrderId;
    private Long lineId;
    private Long planQty;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductionTaskEntity() {
    }

    public ProductionTaskEntity(Long taskId, String taskNo, Long workOrderId, Long lineId,
                                 Long planQty, String status,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.workOrderId = workOrderId;
        this.lineId = lineId;
        this.planQty = planQty;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }

    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public Long getPlanQty() { return planQty; }
    public void setPlanQty(Long planQty) { this.planQty = planQty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
