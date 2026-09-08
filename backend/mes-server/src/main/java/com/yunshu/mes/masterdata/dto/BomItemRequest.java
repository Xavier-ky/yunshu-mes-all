package com.yunshu.mes.masterdata.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BomItemRequest(
        @NotNull Long bomId,
        @NotNull Long materialId,
        @NotNull BigDecimal qtyPer,
        BigDecimal lossRate,
        Boolean isKeyMaterial,
        String remark
) {
    public BomItemRequest {
        if (lossRate == null) lossRate = BigDecimal.ZERO;
        if (isKeyMaterial == null) isKeyMaterial = false;
        if (remark == null) remark = "";
    }
}
