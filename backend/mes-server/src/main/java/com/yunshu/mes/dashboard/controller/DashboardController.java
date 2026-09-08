package com.yunshu.mes.dashboard.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.dashboard.service.DashboardService;
import com.yunshu.mes.dashboard.vo.DashboardSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryVO> getSummary(HttpServletRequest request) {
        return ApiResponse.success(dashboardService.getSummary(), request);
    }
}
