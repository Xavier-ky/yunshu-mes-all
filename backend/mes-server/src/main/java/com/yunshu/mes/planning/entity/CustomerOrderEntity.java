package com.yunshu.mes.planning.entity;

import java.time.LocalDateTime;

/**
 * 客户订单实体 — 对应 customer_order 表。
 */
public class CustomerOrderEntity {

    private Long orderId;
    private String orderNo;
    private String customerName;
    private Long productId;
    private Long orderQty;
    private String deliveryDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CustomerOrderEntity() {
    }

    public CustomerOrderEntity(Long orderId, String orderNo, String customerName, Long productId,
                                Long orderQty, String deliveryDate, String status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.productId = productId;
        this.orderQty = orderQty;
        this.deliveryDate = deliveryDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getOrderQty() { return orderQty; }
    public void setOrderQty(Long orderQty) { this.orderQty = orderQty; }

    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
