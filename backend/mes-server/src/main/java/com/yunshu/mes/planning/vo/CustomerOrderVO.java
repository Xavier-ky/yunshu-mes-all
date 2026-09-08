package com.yunshu.mes.planning.vo;

public record CustomerOrderVO(
        Long orderId,
        String orderNo,
        String customerName,
        Long productId,
        String productName,
        Long orderQty,
        String deliveryDate,
        String status
) {
}
