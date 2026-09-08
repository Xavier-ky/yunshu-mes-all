package com.yunshu.mes.barcode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 条码规则创建/更新请求。
 */
public record BarcodeRuleRequest(
        @NotBlank @Size(max = 64) String ruleCode,
        @NotBlank @Size(max = 100) String ruleName,
        @NotNull Long typeId,
        @NotBlank @Size(max = 32) String codeMode,
        @Size(max = 32) String status
) {
    public BarcodeRuleRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
