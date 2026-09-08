package com.yunshu.mes.planning.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.planning.service.PlanningSummaryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planning")
public class SchedulingOverviewController {

    private final PlanningSummaryService summaryService;

    public SchedulingOverviewController(PlanningSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/scheduling/overview")
    public ApiResponse<Map<String, Object>> overview(HttpServletRequest request) {
        return ApiResponse.success(summaryService.getSchedulingOverview(), request);
    }
}
