package com.yunshu.mes.andon.controller;

import com.yunshu.mes.andon.dto.AndonReasonRequest;
import com.yunshu.mes.andon.dto.AndonTypeRequest;
import com.yunshu.mes.andon.service.AndonService;
import com.yunshu.mes.andon.vo.AndonEventVO;
import com.yunshu.mes.andon.vo.AndonReasonVO;
import com.yunshu.mes.andon.vo.AndonTypeVO;
import com.yunshu.mes.common.response.ApiResponse;
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
 * 安灯模块控制器 — 安灯类型、异常原因、安灯事件 完整 CRUD。
 */
@RestController
@RequestMapping("/api/andon")
public class AndonController {

    private final AndonService andonService;

    public AndonController(AndonService andonService) {
        this.andonService = andonService;
    }

    // ==================== 安灯类型 ====================

    @GetMapping("/types")
    public ApiResponse<List<AndonTypeVO>> listTypes(HttpServletRequest request) {
        return ApiResponse.success(andonService.listTypes(), request);
    }

    @GetMapping("/types/{id}")
    public ApiResponse<AndonTypeVO> getType(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                andonService.getTypeById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "安灯类型不存在")),
                request);
    }

    @PostMapping("/types")
    public ApiResponse<AndonTypeVO> createType(@Valid @RequestBody AndonTypeRequest req, HttpServletRequest request) {
        return ApiResponse.success(andonService.createType(req), request);
    }

    @PutMapping("/types/{id}")
    public ApiResponse<AndonTypeVO> updateType(@PathVariable Long id, @Valid @RequestBody AndonTypeRequest req,
                                                HttpServletRequest request) {
        return ApiResponse.success(andonService.updateType(id, req), request);
    }

    @DeleteMapping("/types/{id}")
    public ApiResponse<Void> deleteType(@PathVariable Long id, HttpServletRequest request) {
        andonService.deleteType(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 安灯原因 ====================

    @GetMapping("/reasons")
    public ApiResponse<List<AndonReasonVO>> listReasons(HttpServletRequest request) {
        return ApiResponse.success(andonService.listReasons(), request);
    }

    @GetMapping("/reasons/{id}")
    public ApiResponse<AndonReasonVO> getReason(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                andonService.getReasonById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "安灯原因不存在")),
                request);
    }

    @PostMapping("/reasons")
    public ApiResponse<AndonReasonVO> createReason(@Valid @RequestBody AndonReasonRequest req, HttpServletRequest request) {
        return ApiResponse.success(andonService.createReason(req), request);
    }

    @PutMapping("/reasons/{id}")
    public ApiResponse<AndonReasonVO> updateReason(@PathVariable Long id, @Valid @RequestBody AndonReasonRequest req,
                                                    HttpServletRequest request) {
        return ApiResponse.success(andonService.updateReason(id, req), request);
    }

    @DeleteMapping("/reasons/{id}")
    public ApiResponse<Void> deleteReason(@PathVariable Long id, HttpServletRequest request) {
        andonService.deleteReason(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 安灯事件 ====================

    @GetMapping("/events")
    public ApiResponse<List<AndonEventVO>> listEvents(HttpServletRequest request) {
        return ApiResponse.success(andonService.listEvents(), request);
    }
}
