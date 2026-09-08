package com.yunshu.mes.factory.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.factory.service.LineMonitorService;
import com.yunshu.mes.factory.vo.LineMonitorDetailVO;
import com.yunshu.mes.factory.vo.LineMonitorSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产线监控聚合 API。
 */
@RestController
@RequestMapping("/api/factory/lines")
public class LineMonitorController {

    private final LineMonitorService lineMonitorService;

    public LineMonitorController(LineMonitorService lineMonitorService) {
        this.lineMonitorService = lineMonitorService;
    }

    @GetMapping("/monitor/summary")
    public ApiResponse<LineMonitorSummaryVO> summary(HttpServletRequest request) {
        return ApiResponse.success(lineMonitorService.getSummary(), request);
    }

    @GetMapping("/{lineId}/monitor")
    public ApiResponse<LineMonitorDetailVO> detail(@PathVariable Long lineId, HttpServletRequest request) {
        return ApiResponse.success(lineMonitorService.getDetail(lineId), request);
    }
}
