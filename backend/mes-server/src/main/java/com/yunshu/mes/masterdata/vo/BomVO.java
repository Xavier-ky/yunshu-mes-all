package com.yunshu.mes.masterdata.vo;

/**
 * BOM视图对象。
 */
public record BomVO(
        Long bomId,
        String bomCode,
        String bomName,
        String productName,
        String bomVersion,
        String status
) {
}
