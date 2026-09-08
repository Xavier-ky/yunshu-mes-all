package com.yunshu.mes.production.entity;

import java.time.LocalDateTime;

/**
 * 生产完工实体 — 对应 production_completion 表。
 */
public class ProductionCompletionEntity {

    private Long completionId;
    private String completionNo;
    private Long workOrderId;
    private String completedQty;
    private String defectQty;
    private LocalDateTime completionTime;
    private String status;

    public ProductionCompletionEntity() {
    }

    public ProductionCompletionEntity(Long completionId, String completionNo, Long workOrderId,
                                       String completedQty, String defectQty,
                                       LocalDateTime completionTime, String status) {
        this.completionId = completionId;
        this.completionNo = completionNo;
        this.workOrderId = workOrderId;
        this.completedQty = completedQty;
        this.defectQty = defectQty;
        this.completionTime = completionTime;
        this.status = status;
    }

    public Long getCompletionId() { return completionId; }
    public void setCompletionId(Long completionId) { this.completionId = completionId; }

    public String getCompletionNo() { return completionNo; }
    public void setCompletionNo(String completionNo) { this.completionNo = completionNo; }

    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }

    public String getCompletedQty() { return completedQty; }
    public void setCompletedQty(String completedQty) { this.completedQty = completedQty; }

    public String getDefectQty() { return defectQty; }
    public void setDefectQty(String defectQty) { this.defectQty = defectQty; }

    public LocalDateTime getCompletionTime() { return completionTime; }
    public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
