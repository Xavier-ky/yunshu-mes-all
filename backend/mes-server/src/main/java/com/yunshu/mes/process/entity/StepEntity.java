package com.yunshu.mes.process.entity;

import java.time.LocalDateTime;

/**
 * 工序实体 — 对应 process_step 表。
 */
public class StepEntity {

    private Long stepId;
    private String stepCode;
    private String stepName;
    private String stepType;
    private String standardHours;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StepEntity() {
    }

    public StepEntity(Long stepId, String stepCode, String stepName, String stepType,
                       String standardHours, String status,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.stepId = stepId;
        this.stepCode = stepCode;
        this.stepName = stepName;
        this.stepType = stepType;
        this.standardHours = standardHours;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }

    public String getStepCode() { return stepCode; }
    public void setStepCode(String stepCode) { this.stepCode = stepCode; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }

    public String getStandardHours() { return standardHours; }
    public void setStandardHours(String standardHours) { this.standardHours = standardHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
