package com.yunshu.mes.planning.vo;

public record CustomerOrderItemVO(
        Long orderItemId,
        Long orderId,
        Long productId,
        String productCode,
        String productName,
        Long orderQty,
        String technicalRequirement
) {
}
