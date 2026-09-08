package com.yunshu.mes.barcode.vo;

public record BarcodeTemplateVO(
        Long templateId,
        String templateCode,
        String templateName,
        String status
) {
}
