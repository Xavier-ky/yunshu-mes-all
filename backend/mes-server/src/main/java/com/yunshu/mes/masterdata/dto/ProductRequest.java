package com.yunshu.mes.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 产品创建/更新请求。
 */
public record ProductRequest(
        @NotBlank @Size(max = 64) String productCode,
        @NotBlank @Size(max = 200) String productName,
        @Size(max = 100) String productModel,
        @Size(max = 64) String category,
        @Size(max = 32) String status
) {
    public ProductRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
        if (productModel == null) {
            productModel = "";
        }
        if (category == null) {
            category = "";
        }
    }
}
