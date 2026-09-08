package com.yunshu.mes.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 物料创建/更新请求。
 */
public record MaterialRequest(
        @NotBlank @Size(max = 64) String materialCode,
        @NotBlank @Size(max = 200) String materialName,
        @Size(max = 64) String materialType,
        Long unitId,
        Integer isCritical,
        @Size(max = 32) String status
) {
    public MaterialRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
        if (materialType == null) {
            materialType = "";
        }
        if (isCritical == null) {
            isCritical = 0;
        }
    }
}
