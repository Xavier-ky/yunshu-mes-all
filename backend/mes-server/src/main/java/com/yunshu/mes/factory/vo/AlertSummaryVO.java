package com.yunshu.mes.factory.vo;

import java.util.List;

public record AlertSummaryVO(
        long total,
        long highPriorityCount,
        long lineCount,
        long maxDurationMinutes,
        List<AlertTypeSliceVO> typeSlices
) {
}
