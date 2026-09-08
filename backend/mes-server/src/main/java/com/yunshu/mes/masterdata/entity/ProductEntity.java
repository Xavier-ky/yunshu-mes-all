package com.yunshu.mes.masterdata.entity;

import java.time.LocalDateTime;

/**
 * 产品实体 — 对应 product 表。
 */
public class ProductEntity {

    private Long productId;
    private String productCode;
    private String productName;
    private String productModel;
    private String category;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductEntity() {
    }

    public ProductEntity(Long productId, String productCode, String productName, String productModel,
                         String category, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.productModel = productModel;
        this.category = category;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductModel() { return productModel; }
    public void setProductModel(String productModel) { this.productModel = productModel; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
