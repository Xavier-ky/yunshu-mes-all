package com.yunshu.mes.masterdata.vo;

/**
 * 计量单位视图对象。
 */
public record UomVO(
        Long unitId,
        String unitCode,
        String unitName,
        String status
) {
}
