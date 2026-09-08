package com.yunshu.mes.inventory.service;

import com.yunshu.mes.inventory.dto.InventoryBatchRequest;
import com.yunshu.mes.inventory.dto.WarehouseRequest;
import com.yunshu.mes.inventory.vo.InventoryBatchVO;
import com.yunshu.mes.inventory.vo.InventoryTransactionVO;
import com.yunshu.mes.inventory.vo.MaterialRequisitionVO;
import com.yunshu.mes.inventory.vo.WarehouseVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 库存资源域服务接口：仓库、库存批次。
 */
public interface InventoryService {

    // ---- 仓库 ----
    List<WarehouseVO> listWarehouses();

    Optional<WarehouseVO> getWarehouseById(Long id);

    WarehouseVO createWarehouse(WarehouseRequest req);

    WarehouseVO updateWarehouse(Long id, WarehouseRequest req);

    void deleteWarehouse(Long id);

    // ---- 库存批次 ----
    List<InventoryBatchVO> listBatches();

    Optional<InventoryBatchVO> getBatchById(Long id);

    InventoryBatchVO createBatch(InventoryBatchRequest req);

    InventoryBatchVO updateBatch(Long id, InventoryBatchRequest req);

    void deleteBatch(Long id);

    // ---- 库存流水 ----
    List<InventoryTransactionVO> listTransactions();

    // ---- 领料单 ----
    List<MaterialRequisitionVO> listRequisitions();
    MaterialRequisitionVO createRequisition(String requisitionNo, Long workOrderId, Long requestUserId, String status, String remark);
    void addRequisitionItem(Long requisitionId, Long materialId, BigDecimal requestQty);
    void deleteRequisitionItem(Long itemId);
    void updateRequisitionStatus(Long id, String status);
    void deleteRequisition(Long id);
}
