package com.yunshu.mes.masterdata.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.masterdata.dto.BomItemRequest;
import com.yunshu.mes.masterdata.dto.BomRequest;
import com.yunshu.mes.masterdata.dto.MaterialRequest;
import com.yunshu.mes.masterdata.dto.ProductRequest;
import com.yunshu.mes.masterdata.dto.UomRequest;
import com.yunshu.mes.masterdata.service.MasterDataService;
import com.yunshu.mes.masterdata.vo.BomItemVO;
import com.yunshu.mes.masterdata.vo.BomVO;
import com.yunshu.mes.masterdata.vo.MaterialVO;
import com.yunshu.mes.masterdata.vo.ProductVO;
import com.yunshu.mes.masterdata.vo.UomVO;
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
 * 主数据控制器 — 产品、物料、BOM、计量单位 完整 CRUD。
 */
@RestController
@RequestMapping("/api/master-data")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    // ==================== 产品 ====================

    @GetMapping("/products")
    public ApiResponse<List<ProductVO>> listProducts(HttpServletRequest request) {
        return ApiResponse.success(masterDataService.listProducts(), request);
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductVO> getProduct(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                masterDataService.getProductById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "产品不存在")),
                request);
    }

    @PostMapping("/products")
    public ApiResponse<ProductVO> createProduct(@Valid @RequestBody ProductRequest req, HttpServletRequest request) {
        return ApiResponse.success(masterDataService.createProduct(req), request);
    }

    @PutMapping("/products/{id}")
    public ApiResponse<ProductVO> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest req,
                                                 HttpServletRequest request) {
        return ApiResponse.success(masterDataService.updateProduct(id, req), request);
    }

    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        masterDataService.deleteProduct(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 物料 ====================

    @GetMapping("/materials")
    public ApiResponse<List<MaterialVO>> listMaterials(HttpServletRequest request) {
        return ApiResponse.success(masterDataService.listMaterials(), request);
    }

    @GetMapping("/materials/{id}")
    public ApiResponse<MaterialVO> getMaterial(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                masterDataService.getMaterialById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "物料不存在")),
                request);
    }

    @PostMapping("/materials")
    public ApiResponse<MaterialVO> createMaterial(@Valid @RequestBody MaterialRequest req, HttpServletRequest request) {
        return ApiResponse.success(masterDataService.createMaterial(req), request);
    }

    @PutMapping("/materials/{id}")
    public ApiResponse<MaterialVO> updateMaterial(@PathVariable Long id, @Valid @RequestBody MaterialRequest req,
                                                   HttpServletRequest request) {
        return ApiResponse.success(masterDataService.updateMaterial(id, req), request);
    }

    @DeleteMapping("/materials/{id}")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long id, HttpServletRequest request) {
        masterDataService.deleteMaterial(id);
        return ApiResponse.success(null, request);
    }

    // ==================== BOM ====================

    @GetMapping("/boms")
    public ApiResponse<List<BomVO>> listBoms(HttpServletRequest request) {
        return ApiResponse.success(masterDataService.listBoms(), request);
    }

    @GetMapping("/boms/{id}")
    public ApiResponse<BomVO> getBom(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                masterDataService.getBomById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "BOM不存在")),
                request);
    }

    @PostMapping("/boms")
    public ApiResponse<BomVO> createBom(@Valid @RequestBody BomRequest req, HttpServletRequest request) {
        return ApiResponse.success(masterDataService.createBom(req), request);
    }

    @PutMapping("/boms/{id}")
    public ApiResponse<BomVO> updateBom(@PathVariable Long id, @Valid @RequestBody BomRequest req,
                                         HttpServletRequest request) {
        return ApiResponse.success(masterDataService.updateBom(id, req), request);
    }

    @DeleteMapping("/boms/{id}")
    public ApiResponse<Void> deleteBom(@PathVariable Long id, HttpServletRequest request) {
        masterDataService.deleteBom(id);
        return ApiResponse.success(null, request);
    }

    // ==================== BOM 物料明细 ====================

    @GetMapping("/boms/{bomId}/items")
    public ApiResponse<List<BomItemVO>> listBomItems(@PathVariable Long bomId, HttpServletRequest request) {
        return ApiResponse.success(masterDataService.listBomItems(bomId), request);
    }

    @PostMapping("/boms/{bomId}/items")
    public ApiResponse<BomItemVO> createBomItem(@PathVariable Long bomId, @Valid @RequestBody BomItemRequest req,
                                                 HttpServletRequest request) {
        return ApiResponse.success(masterDataService.createBomItem(bomId, req), request);
    }

    @PutMapping("/boms/{bomId}/items/{itemId}")
    public ApiResponse<BomItemVO> updateBomItem(@PathVariable Long bomId, @PathVariable Long itemId,
                                                 @Valid @RequestBody BomItemRequest req, HttpServletRequest request) {
        return ApiResponse.success(masterDataService.updateBomItem(itemId, req), request);
    }

    @DeleteMapping("/boms/{bomId}/items/{itemId}")
    public ApiResponse<Void> deleteBomItem(@PathVariable Long bomId, @PathVariable Long itemId, HttpServletRequest request) {
        masterDataService.deleteBomItem(itemId);
        return ApiResponse.success(null, request);
    }

    // ==================== 产品-工艺路线关联 ====================

    @GetMapping("/products/{productId}/route")
    public ApiResponse<Long> getProductRoute(@PathVariable Long productId, HttpServletRequest request) {
        Long routeId = masterDataService.getProductRoute(productId);
        return ApiResponse.success(routeId, request);
    }

    @PostMapping("/products/{productId}/route")
    public ApiResponse<Void> bindProductRoute(@PathVariable Long productId, @RequestBody java.util.Map<String, Long> body,
                                               HttpServletRequest request) {
        masterDataService.bindProductRoute(productId, body.get("routeId"));
        return ApiResponse.success(null, request);
    }

    // ==================== 计量单位 ====================

    @GetMapping("/uoms")
    public ApiResponse<List<UomVO>> listUoms(HttpServletRequest request) {
        return ApiResponse.success(masterDataService.listUoms(), request);
    }

    @GetMapping("/uoms/{id}")
    public ApiResponse<UomVO> getUom(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                masterDataService.getUomById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "计量单位不存在")),
                request);
    }

    @PostMapping("/uoms")
    public ApiResponse<UomVO> createUom(@Valid @RequestBody UomRequest req, HttpServletRequest request) {
        return ApiResponse.success(masterDataService.createUom(req), request);
    }

    @PutMapping("/uoms/{id}")
    public ApiResponse<UomVO> updateUom(@PathVariable Long id, @Valid @RequestBody UomRequest req,
                                         HttpServletRequest request) {
        return ApiResponse.success(masterDataService.updateUom(id, req), request);
    }

    @DeleteMapping("/uoms/{id}")
    public ApiResponse<Void> deleteUom(@PathVariable Long id, HttpServletRequest request) {
        masterDataService.deleteUom(id);
        return ApiResponse.success(null, request);
    }
}
