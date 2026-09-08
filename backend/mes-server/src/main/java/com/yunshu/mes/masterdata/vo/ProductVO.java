package com.yunshu.mes.masterdata.vo;

/**
 * 产品视图对象。
 */
public record ProductVO(
        Long productId,
        String productCode,
        String productName,
        String productModel,
        String category,
        String status
) {
}
