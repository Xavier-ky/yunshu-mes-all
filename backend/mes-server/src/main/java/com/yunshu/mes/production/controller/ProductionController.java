package com.yunshu.mes.production.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.production.dto.ProductSnRequest;
import com.yunshu.mes.production.dto.ProductionCompletionRequest;
import com.yunshu.mes.production.dto.ProductionReportRequest;
import com.yunshu.mes.production.service.ProductionService;
import com.yunshu.mes.production.vo.ProductSnVO;
import com.yunshu.mes.production.vo.ProductionCompletionVO;
import com.yunshu.mes.production.vo.ProductionReportVO;
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
 * 生产执行控制器 — 产品 SN、生产报工、完工单 完整 CRUD。
 */
@RestController
@RequestMapping("/api/production")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    // ==================== 产品 SN ====================

    @GetMapping("/product-sns")
    public ApiResponse<List<ProductSnVO>> listProductSns(HttpServletRequest request) {
        return ApiResponse.success(productionService.listProductSns(), request);
    }

    @GetMapping("/product-sns/{id}")
    public ApiResponse<ProductSnVO> getProductSn(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                productionService.getProductSnById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "产品SN不存在")),
                request);
    }

    @PostMapping("/product-sns")
    public ApiResponse<ProductSnVO> createProductSn(@Valid @RequestBody ProductSnRequest req, HttpServletRequest request) {
        return ApiResponse.success(productionService.createProductSn(req), request);
    }

    @PutMapping("/product-sns/{id}")
    public ApiResponse<ProductSnVO> updateProductSn(@PathVariable Long id, @Valid @RequestBody ProductSnRequest req,
                                                     HttpServletRequest request) {
        return ApiResponse.success(productionService.updateProductSn(id, req), request);
    }

    @DeleteMapping("/product-sns/{id}")
    public ApiResponse<Void> deleteProductSn(@PathVariable Long id, HttpServletRequest request) {
        productionService.deleteProductSn(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 生产报工 ====================

    @GetMapping("/reports")
    public ApiResponse<List<ProductionReportVO>> listReports(HttpServletRequest request) {
        return ApiResponse.success(productionService.listReports(), request);
    }

    @GetMapping("/reports/{id}")
    public ApiResponse<ProductionReportVO> getReport(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                productionService.getReportById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "报工记录不存在")),
                request);
    }

    @PostMapping("/reports")
    public ApiResponse<ProductionReportVO> createReport(@Valid @RequestBody ProductionReportRequest req, HttpServletRequest request) {
        return ApiResponse.success(productionService.createReport(req), request);
    }

    @PutMapping("/reports/{id}")
    public ApiResponse<ProductionReportVO> updateReport(@PathVariable Long id, @Valid @RequestBody ProductionReportRequest req,
                                                         HttpServletRequest request) {
        return ApiResponse.success(productionService.updateReport(id, req), request);
    }

    @DeleteMapping("/reports/{id}")
    public ApiResponse<Void> deleteReport(@PathVariable Long id, HttpServletRequest request) {
        productionService.deleteReport(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 完工单 ====================

    @GetMapping("/completions")
    public ApiResponse<List<ProductionCompletionVO>> listCompletions(HttpServletRequest request) {
        return ApiResponse.success(productionService.listCompletions(), request);
    }

    @GetMapping("/completions/{id}")
    public ApiResponse<ProductionCompletionVO> getCompletion(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                productionService.getCompletionById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "完工单不存在")),
                request);
    }

    @PostMapping("/completions")
    public ApiResponse<ProductionCompletionVO> createCompletion(@Valid @RequestBody ProductionCompletionRequest req,
                                                                 HttpServletRequest request) {
        return ApiResponse.success(productionService.createCompletion(req), request);
    }

    @PutMapping("/completions/{id}")
    public ApiResponse<ProductionCompletionVO> updateCompletion(@PathVariable Long id, @Valid @RequestBody ProductionCompletionRequest req,
                                                                 HttpServletRequest request) {
        return ApiResponse.success(productionService.updateCompletion(id, req), request);
    }

    @DeleteMapping("/completions/{id}")
    public ApiResponse<Void> deleteCompletion(@PathVariable Long id, HttpServletRequest request) {
        productionService.deleteCompletion(id);
        return ApiResponse.success(null, request);
    }
}
