package com.yunshu.mes.factory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 产线创建/更新请求。
 */
public record ProductionLineRequest(
        @NotNull Long workshopId,
        @NotBlank @Size(max = 64) String lineCode,
        @NotBlank @Size(max = 100) String lineName,
        String ratedCapacity,
        @Size(max = 32) String capacityUnit,
        @Size(max = 32) String status,
        Double modelPosX,
        Double modelPosY,
        Double modelPosZ
) {
    public ProductionLineRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
    }
}
