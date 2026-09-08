package com.yunshu.mes.production.entity;

import java.time.LocalDateTime;

/**
 * 产品序列号实体 — 对应 product_sn 表。
 */
public class ProductSnEntity {

    private Long snId;
    private String snCode;
    private Long productId;
    private Long workOrderId;
    private Long barcodeId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductSnEntity() {
    }

    public ProductSnEntity(Long snId, String snCode, Long productId, Long workOrderId,
                           Long barcodeId, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.snId = snId;
        this.snCode = snCode;
        this.productId = productId;
        this.workOrderId = workOrderId;
        this.barcodeId = barcodeId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getSnId() { return snId; }
    public void setSnId(Long snId) { this.snId = snId; }

    public String getSnCode() { return snCode; }
    public void setSnCode(String snCode) { this.snCode = snCode; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }

    public Long getBarcodeId() { return barcodeId; }
    public void setBarcodeId(Long barcodeId) { this.barcodeId = barcodeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
