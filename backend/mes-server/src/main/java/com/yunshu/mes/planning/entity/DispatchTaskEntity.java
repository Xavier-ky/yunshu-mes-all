package com.yunshu.mes.planning.entity;

import java.time.LocalDateTime;

/**
 * 派工单实体 — 对应 dispatch_task 表。
 */
public class DispatchTaskEntity {

    private Long dispatchId;
    private String dispatchNo;
    private Long taskId;
    private Long stepId;
    private Long stationId;
    private Long assigneeId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DispatchTaskEntity() {
    }

    public DispatchTaskEntity(Long dispatchId, String dispatchNo, Long taskId, Long stepId,
                               Long stationId, Long assigneeId, String status,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.dispatchId = dispatchId;
        this.dispatchNo = dispatchNo;
        this.taskId = taskId;
        this.stepId = stepId;
        this.stationId = stationId;
        this.assigneeId = assigneeId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getDispatchId() { return dispatchId; }
    public void setDispatchId(Long dispatchId) { this.dispatchId = dispatchId; }

    public String getDispatchNo() { return dispatchNo; }
    public void setDispatchNo(String dispatchNo) { this.dispatchNo = dispatchNo; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
