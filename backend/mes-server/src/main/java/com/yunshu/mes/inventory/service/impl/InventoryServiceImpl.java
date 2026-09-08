package com.yunshu.mes.inventory.service.impl;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.inventory.dto.InventoryBatchRequest;
import com.yunshu.mes.inventory.dto.WarehouseRequest;
import com.yunshu.mes.inventory.repository.InventoryBatchRepository;
import com.yunshu.mes.inventory.repository.WarehouseRepository;
import com.yunshu.mes.inventory.service.InventoryMockDataService;
import com.yunshu.mes.inventory.service.InventoryService;
import com.yunshu.mes.inventory.vo.InventoryBatchVO;
import com.yunshu.mes.inventory.vo.InventoryTransactionVO;
import com.yunshu.mes.inventory.vo.WarehouseVO;
import com.yunshu.mes.inventory.vo.MaterialRequisitionVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 库存资源域服务门面：优先 JDBC 仓储，异常时回退 Mock。
 */
@Service
@Primary
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final WarehouseRepository warehouseRepo;
    private final InventoryBatchRepository batchRepo;
    private final InventoryMockDataService mock;

    public InventoryServiceImpl(WarehouseRepository warehouseRepo, InventoryBatchRepository batchRepo,
                                InventoryMockDataService mock) {
        this.warehouseRepo = warehouseRepo;
        this.batchRepo = batchRepo;
        this.mock = mock;
    }

    // ==================== 仓库 ====================

    @Override
    public List<WarehouseVO> listWarehouses() {
        try { return warehouseRepo.findAll(); }
        catch (DataAccessException e) { log.warn("仓库列表回退 Mock：{}", e.getMessage()); return mock.listWarehouses(); }
    }

    @Override
    public Optional<WarehouseVO> getWarehouseById(Long id) {
        try { return warehouseRepo.findById(id).or(() -> mock.getWarehouseById(id)); }
        catch (DataAccessException e) { log.warn("仓库详情回退 Mock：{}", e.getMessage()); return mock.getWarehouseById(id); }
    }

    @Override
    public WarehouseVO createWarehouse(WarehouseRequest req) {
        try {
            Long id = warehouseRepo.insert(req.warehouseCode(), req.warehouseName(), req.warehouseType(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建仓库失败");
            return warehouseRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建仓库后回查失败"));
        } catch (DataAccessException e) { log.warn("创建仓库回退 Mock：{}", e.getMessage()); return mock.createWarehouse(req); }
    }

    @Override
    public WarehouseVO updateWarehouse(Long id, WarehouseRequest req) {
        try {
            warehouseRepo.update(id, req.warehouseCode(), req.warehouseName(), req.warehouseType(), req.status());
            return warehouseRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "仓库不存在"));
        } catch (DataAccessException e) { log.warn("更新仓库回退 Mock：{}", e.getMessage()); return mock.updateWarehouse(id, req); }
    }

    @Override
    public void deleteWarehouse(Long id) {
        try { warehouseRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除仓库回退 Mock：{}", e.getMessage()); mock.deleteWarehouse(id); }
    }

    // ==================== 库存批次 ====================

    @Override
    public List<InventoryBatchVO> listBatches() {
        try { return batchRepo.findAll(); }
        catch (DataAccessException e) { log.warn("库存批次列表回退 Mock：{}", e.getMessage()); return mock.listBatches(); }
    }

    @Override
    public Optional<InventoryBatchVO> getBatchById(Long id) {
        try { return batchRepo.findById(id).or(() -> mock.getBatchById(id)); }
        catch (DataAccessException e) { log.warn("库存批次详情回退 Mock：{}", e.getMessage()); return mock.getBatchById(id); }
    }

    @Override
    public InventoryBatchVO createBatch(InventoryBatchRequest req) {
        try {
            Long id = batchRepo.insert(req.batchNo(), req.materialId(), req.warehouseId(),
                    req.locationId(), req.availableQty(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建库存批次失败");
            return batchRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建库存批次后回查失败"));
        } catch (DataAccessException e) { log.warn("创建库存批次回退 Mock：{}", e.getMessage()); return mock.createBatch(req); }
    }

    @Override
    public InventoryBatchVO updateBatch(Long id, InventoryBatchRequest req) {
        try {
            batchRepo.update(id, req.batchNo(), req.materialId(), req.warehouseId(),
                    req.locationId(), req.availableQty(), req.status());
            return batchRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "库存批次不存在"));
        } catch (DataAccessException e) { log.warn("更新库存批次回退 Mock：{}", e.getMessage()); return mock.updateBatch(id, req); }
    }

    @Override
    public void deleteBatch(Long id) {
        try { batchRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除库存批次回退 Mock：{}", e.getMessage()); mock.deleteBatch(id); }
    }

    // ==================== 库存流水 ====================

    @Override
    public List<InventoryTransactionVO> listTransactions() {
        return List.of();
    }

    @Override public List<MaterialRequisitionVO> listRequisitions() { return List.of(); }
    @Override public MaterialRequisitionVO createRequisition(String a, Long b, Long c, String d, String e) { return null; }
    @Override public void addRequisitionItem(Long a, Long b, BigDecimal c) {}
    @Override public void deleteRequisitionItem(Long a) {}
    @Override public void updateRequisitionStatus(Long a, String b) {}
    @Override public void deleteRequisition(Long a) {}
}
