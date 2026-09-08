package com.yunshu.mes.traceability.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.traceability.service.TraceabilityService;
import com.yunshu.mes.traceability.vo.ProductTraceVO;
import com.yunshu.mes.traceability.vo.TraceQueryLogVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traceability")
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    public TraceabilityController(TraceabilityService traceabilityService) {
        this.traceabilityService = traceabilityService;
    }

    @GetMapping("/query-logs")
    public ApiResponse<List<TraceQueryLogVO>> listQueryLogs(HttpServletRequest request) {
        return ApiResponse.success(traceabilityService.listQueryLogs(), request);
    }

    @GetMapping("/product")
    public ApiResponse<ProductTraceVO> traceProduct(@RequestParam("code") String code, HttpServletRequest request) {
        return ApiResponse.success(
                traceabilityService.traceProduct(code).orElse(null), request);
    }
}
