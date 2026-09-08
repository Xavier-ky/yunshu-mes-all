package com.yunshu.mes.barcode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 条码类型创建/更新请求。
 */
public record BarcodeTypeRequest(
        @NotBlank @Size(max = 64) String typeCode,
        @NotBlank @Size(max = 100) String typeName,
        @Size(max = 32) String status
) {
    public BarcodeTypeRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
