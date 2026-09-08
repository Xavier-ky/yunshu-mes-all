package com.yunshu.mes.barcode.vo;

public record BarcodeRuleVO(
        Long ruleId,
        String ruleCode,
        String ruleName,
        String typeName,
        String codeMode,
        String status
) {
}
