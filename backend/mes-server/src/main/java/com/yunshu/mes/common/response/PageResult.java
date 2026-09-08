package com.yunshu.mes.common.response;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        long pageNo,
        long pageSize,
        long total
) {
    public static <T> PageResult<T> of(List<T> records, long pageNo, long pageSize, long total) {
        return new PageResult<>(records, pageNo, pageSize, total);
    }
}
