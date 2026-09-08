package com.yunshu.mes.planning.compat.vo;

import java.util.List;

public record ClientOrderResultVO(
        Long orderId,
        String orderNo,
        String customerName,
        String deliveryDate,
        String status,
        List<ClientOrderLineResultVO> items
) {
}
