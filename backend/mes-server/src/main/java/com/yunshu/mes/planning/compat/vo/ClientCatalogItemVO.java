package com.yunshu.mes.planning.compat.vo;

import java.util.List;

public record ClientCatalogItemVO(
        Long productId,
        String productCode,
        String productName,
        String productModel,
        String category,
        String categoryLabel,
        List<String> specs,
        String imageUrl,
        String displayPrice,
        String tagline
) {
}
