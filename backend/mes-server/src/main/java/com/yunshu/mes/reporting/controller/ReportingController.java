package com.yunshu.mes.reporting.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.reporting.service.ReportingService;
import com.yunshu.mes.reporting.vo.ReportDefinitionVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/definitions")
    public ApiResponse<List<ReportDefinitionVO>> listDefinitions(HttpServletRequest request) {
        return ApiResponse.success(reportingService.listDefinitions(), request);
    }
}
