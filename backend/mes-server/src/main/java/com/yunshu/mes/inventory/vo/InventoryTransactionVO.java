package com.yunshu.mes.inventory.vo;

public record InventoryTransactionVO(
        Long transactionId,
        String batchNo,
        String transactionType,
        String changeQty,
        String transactionTime,
        String remark
) {
}
