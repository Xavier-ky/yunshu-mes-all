package com.yunshu.mes.masterdata.vo;

/**
 * 物料视图对象。
 */
public record MaterialVO(
        Long materialId,
        String materialCode,
        String materialName,
        String materialType,
        String unitName,
        String isCritical,
        String status
) {
}
