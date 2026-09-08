package com.yunshu.mes.reporting.vo;

public record ReportDefinitionVO(
        Long reportId,
        String reportCode,
        String reportName,
        String reportType,
        String status
) {
}
