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
public class PlanningSummaryController {

    private final PlanningSummaryService summaryService;

    public PlanningSummaryController(PlanningSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(HttpServletRequest request) {
        return ApiResponse.success(summaryService.getSummary(), request);
    }
}
