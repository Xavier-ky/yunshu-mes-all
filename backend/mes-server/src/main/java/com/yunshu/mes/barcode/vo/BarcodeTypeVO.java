package com.yunshu.mes.barcode.vo;

public record BarcodeTypeVO(
        Long typeId,
        String typeCode,
        String typeName,
        String status
) {
}
