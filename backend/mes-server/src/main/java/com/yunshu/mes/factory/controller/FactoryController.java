package com.yunshu.mes.factory.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.factory.dto.ProductionLineRequest;
import com.yunshu.mes.factory.dto.ShiftRequest;
import com.yunshu.mes.factory.dto.WorkshopRequest;
import com.yunshu.mes.factory.dto.WorkstationRequest;
import com.yunshu.mes.factory.service.FactoryService;
import com.yunshu.mes.factory.vo.LineVO;
import com.yunshu.mes.factory.vo.ShiftVO;
import com.yunshu.mes.factory.vo.WorkshopVO;
import com.yunshu.mes.factory.vo.WorkstationVO;
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
 * 工厂资源控制器 — 车间、产线、工位、班次 完整 CRUD。
 */
@RestController
@RequestMapping("/api/factory")
public class FactoryController {

    private final FactoryService factoryService;

    public FactoryController(FactoryService factoryService) {
        this.factoryService = factoryService;
    }

    // ==================== 车间 ====================

    @GetMapping("/workshops")
    public ApiResponse<List<WorkshopVO>> listWorkshops(HttpServletRequest request) {
        return ApiResponse.success(factoryService.listWorkshops(), request);
    }

    @GetMapping("/workshops/{id}")
    public ApiResponse<WorkshopVO> getWorkshop(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                factoryService.getWorkshopById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "车间不存在")),
                request);
    }

    @PostMapping("/workshops")
    public ApiResponse<WorkshopVO> createWorkshop(@Valid @RequestBody WorkshopRequest req, HttpServletRequest request) {
        return ApiResponse.success(factoryService.createWorkshop(req), request);
    }

    @PutMapping("/workshops/{id}")
    public ApiResponse<WorkshopVO> updateWorkshop(@PathVariable Long id, @Valid @RequestBody WorkshopRequest req,
                                                   HttpServletRequest request) {
        return ApiResponse.success(factoryService.updateWorkshop(id, req), request);
    }

    @DeleteMapping("/workshops/{id}")
    public ApiResponse<Void> deleteWorkshop(@PathVariable Long id, HttpServletRequest request) {
        factoryService.deleteWorkshop(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 产线 ====================

    @GetMapping("/lines")
    public ApiResponse<List<LineVO>> listLines(HttpServletRequest request) {
        return ApiResponse.success(factoryService.listLines(), request);
    }

    @GetMapping("/lines/{id}")
    public ApiResponse<LineVO> getLine(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                factoryService.getLineById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "产线不存在")),
                request);
    }

    @PostMapping("/lines")
    public ApiResponse<LineVO> createLine(@Valid @RequestBody ProductionLineRequest req, HttpServletRequest request) {
        return ApiResponse.success(factoryService.createLine(req), request);
    }

    @PutMapping("/lines/{id}")
    public ApiResponse<LineVO> updateLine(@PathVariable Long id, @Valid @RequestBody ProductionLineRequest req,
                                           HttpServletRequest request) {
        return ApiResponse.success(factoryService.updateLine(id, req), request);
    }

    @DeleteMapping("/lines/{id}")
    public ApiResponse<Void> deleteLine(@PathVariable Long id, HttpServletRequest request) {
        factoryService.deleteLine(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 工位 ====================

    @GetMapping("/workstations")
    public ApiResponse<List<WorkstationVO>> listWorkstations(HttpServletRequest request) {
        return ApiResponse.success(factoryService.listWorkstations(), request);
    }

    @GetMapping("/workstations/{id}")
    public ApiResponse<WorkstationVO> getWorkstation(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                factoryService.getWorkstationById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "工位不存在")),
                request);
    }

    @PostMapping("/workstations")
    public ApiResponse<WorkstationVO> createWorkstation(@Valid @RequestBody WorkstationRequest req,
                                                         HttpServletRequest request) {
        return ApiResponse.success(factoryService.createWorkstation(req), request);
    }

    @PutMapping("/workstations/{id}")
    public ApiResponse<WorkstationVO> updateWorkstation(@PathVariable Long id, @Valid @RequestBody WorkstationRequest req,
                                                         HttpServletRequest request) {
        return ApiResponse.success(factoryService.updateWorkstation(id, req), request);
    }

    @DeleteMapping("/workstations/{id}")
    public ApiResponse<Void> deleteWorkstation(@PathVariable Long id, HttpServletRequest request) {
        factoryService.deleteWorkstation(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 班次 ====================

    @GetMapping("/shifts")
    public ApiResponse<List<ShiftVO>> listShifts(HttpServletRequest request) {
        return ApiResponse.success(factoryService.listShifts(), request);
    }

    @GetMapping("/shifts/{id}")
    public ApiResponse<ShiftVO> getShift(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                factoryService.getShiftById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "班次不存在")),
                request);
    }

    @PostMapping("/shifts")
    public ApiResponse<ShiftVO> createShift(@Valid @RequestBody ShiftRequest req, HttpServletRequest request) {
        return ApiResponse.success(factoryService.createShift(req), request);
    }

    @PutMapping("/shifts/{id}")
    public ApiResponse<ShiftVO> updateShift(@PathVariable Long id, @Valid @RequestBody ShiftRequest req,
                                             HttpServletRequest request) {
        return ApiResponse.success(factoryService.updateShift(id, req), request);
    }

    @DeleteMapping("/shifts/{id}")
    public ApiResponse<Void> deleteShift(@PathVariable Long id, HttpServletRequest request) {
        factoryService.deleteShift(id);
        return ApiResponse.success(null, request);
    }
}
