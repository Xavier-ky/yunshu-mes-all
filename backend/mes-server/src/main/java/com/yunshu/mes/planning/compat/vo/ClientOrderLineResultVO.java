package com.yunshu.mes.planning.compat.vo;

public record ClientOrderLineResultVO(
        Long productId,
        String productName,
        Long orderQty
) {
}
