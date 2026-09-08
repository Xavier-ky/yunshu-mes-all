package com.yunshu.mes.factory.vo;

import java.util.List;

public record LineMonitorDetailVO(
        LineVO line,
        List<StationStatusVO> stations,
        List<WorkOrderSliceVO> workOrders,
        List<DispatchSliceVO> dispatches,
        List<AndonSliceVO> andons,
        List<HourPointVO> hourlyOutput,
        OeeSliceVO oee,
        List<EventLogVO> events,
        String currentShiftName,
        String refreshedAt
) {
}
