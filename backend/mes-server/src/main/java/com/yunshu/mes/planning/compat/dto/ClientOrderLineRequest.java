package com.yunshu.mes.planning.compat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ClientOrderLineRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Long orderQty
) {
}
