package com.yunshu.mes.factory.vo;

import java.util.List;

public record LineMonitorSummaryVO(
        List<LineMonitorMetricVO> metrics,
        List<LineCardVO> lines,
        List<LineMonitorAlertVO> alerts,
        AlertSummaryVO alertSummary,
        List<EquipmentStatusSliceVO> equipmentStatus,
        EquipmentSummaryVO equipmentSummary,
        String currentShiftName,
        String refreshedAt
) {
}
