package com.yunshu.mes.planning.entity;

import java.time.LocalDateTime;

/**
 * 生产工单实体 — 对应 work_order 表。
 */
public class WorkOrderEntity {

    private Long workOrderId;
    private String workOrderNo;
    private Long productId;
    private Long orderId;
    private Long planQty;
    private Long completedQty;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WorkOrderEntity() {
    }

    public WorkOrderEntity(Long workOrderId, String workOrderNo, Long productId, Long orderId,
                            Long planQty, Long completedQty, String status,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.workOrderId = workOrderId;
        this.workOrderNo = workOrderNo;
        this.productId = productId;
        this.orderId = orderId;
        this.planQty = planQty;
        this.completedQty = completedQty;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }

    public String getWorkOrderNo() { return workOrderNo; }
    public void setWorkOrderNo(String workOrderNo) { this.workOrderNo = workOrderNo; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getPlanQty() { return planQty; }
    public void setPlanQty(Long planQty) { this.planQty = planQty; }

    public Long getCompletedQty() { return completedQty; }
    public void setCompletedQty(Long completedQty) { this.completedQty = completedQty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
