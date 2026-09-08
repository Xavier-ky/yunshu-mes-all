package com.yunshu.mes.barcode.entity;

import java.time.LocalDateTime;

/**
 * 条码规则实体 — 对应 barcode_rule 表。
 */
public class BarcodeRuleEntity {

    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private Long typeId;
    private String codeMode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BarcodeRuleEntity() {
    }

    public BarcodeRuleEntity(Long ruleId, String ruleCode, String ruleName, Long typeId,
                             String codeMode, String status,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.ruleId = ruleId;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.typeId = typeId;
        this.codeMode = codeMode;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public String getCodeMode() { return codeMode; }
    public void setCodeMode(String codeMode) { this.codeMode = codeMode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
