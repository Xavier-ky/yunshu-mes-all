package com.yunshu.mes.inventory.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.inventory.dto.InventoryBatchRequest;
import com.yunshu.mes.inventory.dto.WarehouseRequest;
import com.yunshu.mes.inventory.vo.InventoryBatchVO;
import com.yunshu.mes.inventory.vo.InventoryTransactionVO;
import com.yunshu.mes.inventory.vo.WarehouseVO;
import com.yunshu.mes.inventory.vo.MaterialRequisitionVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * 库存资源域 Mock 实现，与 seed SQL 保持一致。写操作在无库时不可用。
 */
@Service
public class InventoryMockDataService implements InventoryService {

    private static final List<WarehouseVO> WAREHOUSES = new ArrayList<>(List.of(
            new WarehouseVO(1L, "WH-001", "原料仓", "RAW", "ENABLED"),
            new WarehouseVO(2L, "WH-002", "半成品仓", "SEMI", "ENABLED"),
            new WarehouseVO(3L, "WH-003", "成品仓", "FINISHED", "ENABLED")
    ));

    private static final List<InventoryBatchVO> BATCHES = new ArrayList<>(List.of(
            new InventoryBatchVO(1L, "B20240101-001", "电机-AC220V", "原料仓", 5000L, "ENABLED"),
            new InventoryBatchVO(2L, "B20240101-002", "塑胶扇叶-16寸", "原料仓", 8000L, "ENABLED"),
            new InventoryBatchVO(3L, "B20240102-001", "台扇整机", "成品仓", 300L, "ENABLED")
    ));

    private static final List<InventoryTransactionVO> TRANSACTIONS = new ArrayList<>(List.of(
            new InventoryTransactionVO(1L, "B20240101-001", "IN", "5000", "2024-01-01 10:00:00", "采购入库"),
            new InventoryTransactionVO(2L, "B20240101-002", "IN", "8000", "2024-01-01 10:00:00", "采购入库"),
            new InventoryTransactionVO(3L, "B20240102-001", "IN", "300", "2024-01-02 14:00:00", "生产完工入库")
    ));

    private final AtomicLong warehouseNextId = new AtomicLong(10);
    private final AtomicLong batchNextId = new AtomicLong(10);

    // ---- 仓库 ----
    @Override
    public List<WarehouseVO> listWarehouses() {
        return new ArrayList<>(WAREHOUSES);
    }

    @Override
    public Optional<WarehouseVO> getWarehouseById(Long id) {
        return WAREHOUSES.stream().filter(w -> w.warehouseId().equals(id)).findFirst();
    }

    @Override
    public WarehouseVO createWarehouse(WarehouseRequest req) {
        throw mockWriteError();
    }

    @Override
    public WarehouseVO updateWarehouse(Long id, WarehouseRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteWarehouse(Long id) {
        throw mockWriteError();
    }

    // ---- 库存批次 ----
    @Override
    public List<InventoryBatchVO> listBatches() {
        return new ArrayList<>(BATCHES);
    }

    @Override
    public Optional<InventoryBatchVO> getBatchById(Long id) {
        return BATCHES.stream().filter(b -> b.batchId().equals(id)).findFirst();
    }

    @Override
    public InventoryBatchVO createBatch(InventoryBatchRequest req) {
        throw mockWriteError();
    }

    @Override
    public InventoryBatchVO updateBatch(Long id, InventoryBatchRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteBatch(Long id) {
        throw mockWriteError();
    }

    // ---- 库存流水 ----
    @Override
    public List<InventoryTransactionVO> listTransactions() {
        return new ArrayList<>(TRANSACTIONS);
    }

    @Override public List<MaterialRequisitionVO> listRequisitions() { return List.of(); }
    @Override public MaterialRequisitionVO createRequisition(String a, Long b, Long c, String d, String e) { return null; }
    @Override public void addRequisitionItem(Long a, Long b, BigDecimal c) {}
    @Override public void deleteRequisitionItem(Long a) {}
    @Override public void updateRequisitionStatus(Long a, String b) {}
    @Override public void deleteRequisition(Long a) {}

    private BusinessException mockWriteError() {
        return new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }
}
