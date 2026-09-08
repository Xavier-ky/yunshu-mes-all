package com.yunshu.mes.barcode.entity;

import java.time.LocalDateTime;

/**
 * 条码模板实体 — 对应 barcode_template 表。
 */
public class BarcodeTemplateEntity {

    private Long templateId;
    private String templateCode;
    private String templateName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BarcodeTemplateEntity() {
    }

    public BarcodeTemplateEntity(Long templateId, String templateCode, String templateName, String status,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.templateId = templateId;
        this.templateCode = templateCode;
        this.templateName = templateName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
