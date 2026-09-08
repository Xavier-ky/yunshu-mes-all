package com.yunshu.mes.process.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.process.dto.ProcessRouteStepRequest;
import com.yunshu.mes.process.dto.RouteRequest;
import com.yunshu.mes.process.dto.StepRequest;
import com.yunshu.mes.process.service.ProcessService;
import com.yunshu.mes.process.vo.ProcessRouteStepVO;
import com.yunshu.mes.process.vo.ProcessRouteVO;
import com.yunshu.mes.process.vo.ProcessStepVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工艺管理控制器 — 工序、工艺路线 完整 CRUD。
 */
@RestController
@RequestMapping("/api/process")
public class ProcessController {

    private final ProcessService processService;

    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    // ==================== 工序 ====================

    @GetMapping("/steps")
    public ApiResponse<List<ProcessStepVO>> listSteps(HttpServletRequest request) {
        return ApiResponse.success(processService.listSteps(), request);
    }

    @GetMapping("/steps/{id}")
    public ApiResponse<ProcessStepVO> getStep(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                processService.getStepById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "工序不存在")),
                request);
    }

    @PostMapping("/steps")
    public ApiResponse<ProcessStepVO> createStep(@Valid @RequestBody StepRequest req, HttpServletRequest request) {
        return ApiResponse.success(processService.createStep(req), request);
    }

    @PutMapping("/steps/{id}")
    public ApiResponse<ProcessStepVO> updateStep(@PathVariable Long id, @Valid @RequestBody StepRequest req,
                                                  HttpServletRequest request) {
        return ApiResponse.success(processService.updateStep(id, req), request);
    }

    @DeleteMapping("/steps/{id}")
    public ApiResponse<Void> deleteStep(@PathVariable Long id, HttpServletRequest request) {
        processService.deleteStep(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 工艺路线 ====================

    @GetMapping("/routes")
    public ApiResponse<List<ProcessRouteVO>> listRoutes(HttpServletRequest request) {
        return ApiResponse.success(processService.listRoutes(), request);
    }

    @GetMapping("/routes/{id}")
    public ApiResponse<ProcessRouteVO> getRoute(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                processService.getRouteById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "工艺路线不存在")),
                request);
    }

    @PostMapping("/routes")
    public ApiResponse<ProcessRouteVO> createRoute(@Valid @RequestBody RouteRequest req, HttpServletRequest request) {
        return ApiResponse.success(processService.createRoute(req), request);
    }

    @PutMapping("/routes/{id}")
    public ApiResponse<ProcessRouteVO> updateRoute(@PathVariable Long id, @Valid @RequestBody RouteRequest req,
                                                    HttpServletRequest request) {
        return ApiResponse.success(processService.updateRoute(id, req), request);
    }

    @DeleteMapping("/routes/{id}")
    public ApiResponse<Void> deleteRoute(@PathVariable Long id, HttpServletRequest request) {
        processService.deleteRoute(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 工艺路线工序 ====================

    @GetMapping("/routes/{routeId}/steps")
    public ApiResponse<List<ProcessRouteStepVO>> listRouteSteps(@PathVariable Long routeId, HttpServletRequest request) {
        return ApiResponse.success(processService.listRouteSteps(routeId), request);
    }

    @PostMapping("/routes/{routeId}/steps")
    public ApiResponse<ProcessRouteStepVO> createRouteStep(@PathVariable Long routeId,
                                                            @Valid @RequestBody ProcessRouteStepRequest req,
                                                            HttpServletRequest request) {
        return ApiResponse.success(processService.createRouteStep(req), request);
    }

    @PutMapping("/routes/{routeId}/steps/{stepId}")
    public ApiResponse<ProcessRouteStepVO> updateRouteStep(@PathVariable Long routeId, @PathVariable Long stepId,
                                                            @Valid @RequestBody ProcessRouteStepRequest req,
                                                            HttpServletRequest request) {
        return ApiResponse.success(processService.updateRouteStep(stepId, req), request);
    }

    @DeleteMapping("/routes/{routeId}/steps/{stepId}")
    public ApiResponse<Void> deleteRouteStep(@PathVariable Long routeId, @PathVariable Long stepId,
                                              HttpServletRequest request) {
        processService.deleteRouteStep(stepId);
        return ApiResponse.success(null, request);
    }
}
