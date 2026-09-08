package com.yunshu.mes.barcode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 条码模板创建/更新请求。
 */
public record BarcodeTemplateRequest(
        @NotBlank @Size(max = 64) String templateCode,
        @NotBlank @Size(max = 100) String templateName,
        @Size(max = 32) String status
) {
    public BarcodeTemplateRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
