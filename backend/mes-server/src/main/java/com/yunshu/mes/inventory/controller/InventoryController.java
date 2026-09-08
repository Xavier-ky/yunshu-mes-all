package com.yunshu.mes.inventory.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.inventory.dto.InventoryBatchRequest;
import com.yunshu.mes.inventory.dto.WarehouseRequest;
import com.yunshu.mes.inventory.service.InventoryService;
import com.yunshu.mes.inventory.vo.InventoryBatchVO;
import com.yunshu.mes.inventory.vo.InventoryTransactionVO;
import com.yunshu.mes.inventory.vo.WarehouseVO;
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
 * 库存资源控制器 — 仓库、库存批次、库存流水 完整 CRUD。
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // ==================== 仓库 ====================

    @GetMapping("/warehouses")
    public ApiResponse<List<WarehouseVO>> listWarehouses(HttpServletRequest request) {
        return ApiResponse.success(inventoryService.listWarehouses(), request);
    }

    @GetMapping("/warehouses/{id}")
    public ApiResponse<WarehouseVO> getWarehouse(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                inventoryService.getWarehouseById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "仓库不存在")),
                request);
    }

    @PostMapping("/warehouses")
    public ApiResponse<WarehouseVO> createWarehouse(@Valid @RequestBody WarehouseRequest req, HttpServletRequest request) {
        return ApiResponse.success(inventoryService.createWarehouse(req), request);
    }

    @PutMapping("/warehouses/{id}")
    public ApiResponse<WarehouseVO> updateWarehouse(@PathVariable Long id, @Valid @RequestBody WarehouseRequest req,
                                                     HttpServletRequest request) {
        return ApiResponse.success(inventoryService.updateWarehouse(id, req), request);
    }

    @DeleteMapping("/warehouses/{id}")
    public ApiResponse<Void> deleteWarehouse(@PathVariable Long id, HttpServletRequest request) {
        inventoryService.deleteWarehouse(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 库存批次 ====================

    @GetMapping("/batches")
    public ApiResponse<List<InventoryBatchVO>> listBatches(HttpServletRequest request) {
        return ApiResponse.success(inventoryService.listBatches(), request);
    }

    @GetMapping("/batches/{id}")
    public ApiResponse<InventoryBatchVO> getBatch(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                inventoryService.getBatchById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "库存批次不存在")),
                request);
    }

    @PostMapping("/batches")
    public ApiResponse<InventoryBatchVO> createBatch(@Valid @RequestBody InventoryBatchRequest req,
                                                      HttpServletRequest request) {
        return ApiResponse.success(inventoryService.createBatch(req), request);
    }

    @PutMapping("/batches/{id}")
    public ApiResponse<InventoryBatchVO> updateBatch(@PathVariable Long id, @Valid @RequestBody InventoryBatchRequest req,
                                                      HttpServletRequest request) {
        return ApiResponse.success(inventoryService.updateBatch(id, req), request);
    }

    @DeleteMapping("/batches/{id}")
    public ApiResponse<Void> deleteBatch(@PathVariable Long id, HttpServletRequest request) {
        inventoryService.deleteBatch(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 库存流水 ====================

    @GetMapping("/transactions")
    public ApiResponse<List<InventoryTransactionVO>> listTransactions(HttpServletRequest request) {
        return ApiResponse.success(inventoryService.listTransactions(), request);
    }
}
