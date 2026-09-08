package com.yunshu.mes.planning.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.planning.dto.CustomerOrderRequest;
import com.yunshu.mes.planning.dto.DispatchTaskRequest;
import com.yunshu.mes.planning.dto.ProductionTaskRequest;
import com.yunshu.mes.planning.dto.WorkOrderRequest;
import com.yunshu.mes.planning.vo.CustomerOrderItemVO;
import com.yunshu.mes.planning.vo.CustomerOrderVO;
import com.yunshu.mes.planning.vo.DispatchTaskVO;
import com.yunshu.mes.planning.vo.ProductionTaskVO;
import com.yunshu.mes.planning.vo.WorkOrderVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * 计划管理域 Mock 实现，与 seed SQL 保持一致。写操作在无库时不可用。
 */
@Service
public class PlanningMockDataService implements PlanningService {

    private static final List<CustomerOrderVO> ORDERS = new ArrayList<>(List.of(
            new CustomerOrderVO(1L, "CO-20260701", "华东电器", 1L, "台扇 FS-001", 500L, "2026-08-15", "ENABLED"),
            new CustomerOrderVO(2L, "CO-20260702", "南方家电", 2L, "落地扇 FS-002", 300L, "2026-08-20", "ENABLED"),
            new CustomerOrderVO(3L, "CO-20260703", "北方商城", 1L, "台扇 FS-001", 200L, "2026-08-25", "ENABLED")
    ));

    private static final List<WorkOrderVO> WORK_ORDERS = new ArrayList<>(List.of(
            new WorkOrderVO(1L, "WO-20260701", 1L, "CO-20260701", 1L, "台扇 FS-001", 500L, 320L, "RUNNING", "SUFFICIENT"),
            new WorkOrderVO(2L, "WO-20260702", 2L, "CO-20260702", 2L, "落地扇 FS-002", 300L, 0L, "ENABLED", "INSUFFICIENT"),
            new WorkOrderVO(3L, "WO-20260703", 3L, "CO-20260703", 1L, "台扇 FS-001", 200L, 0L, "ENABLED", null)
    ));

    private static final List<ProductionTaskVO> TASKS = new ArrayList<>(List.of(
            new ProductionTaskVO(1L, "PT-20260701", 1L, "WO-20260701", "台扇组装一线", 500L, "ENABLED"),
            new ProductionTaskVO(2L, "PT-20260702", 2L, "WO-20260702", "落地扇组装一线", 300L, "ENABLED"),
            new ProductionTaskVO(3L, "PT-20260703", 1L, "WO-20260703", "台扇组装一线", 200L, "ENABLED")
    ));

    private static final List<DispatchTaskVO> DISPATCH_TASKS = new ArrayList<>(List.of(
            DispatchTaskVO.basic(1L, "DT-20260701", "PT-20260701", 1L, 1L, 1L, "底座装配", "底座装配工位", 6L, "张三", 500L, 0L, "ENABLED"),
            DispatchTaskVO.basic(2L, "DT-20260702", "PT-20260701", 1L, 2L, 2L, "电机安装", "电机安装工位", 6L, "李四", 500L, 0L, "ENABLED"),
            DispatchTaskVO.basic(3L, "DT-20260703", "PT-20260702", 2L, 1L, 1L, "底座装配", "底座装配工位", 7L, "王五", 300L, 0L, "ENABLED"),
            DispatchTaskVO.basic(4L, "DT-20260704", "PT-20260703", 1L, 3L, 3L, "扇叶装配", "扇叶装配工位", 7L, "赵六", 200L, 0L, "ENABLED")
    ));

    private final AtomicLong orderNextId = new AtomicLong(10);
    private final AtomicLong workOrderNextId = new AtomicLong(10);
    private final AtomicLong taskNextId = new AtomicLong(10);
    private final AtomicLong dispatchNextId = new AtomicLong(10);

    // ---- 客户订单 ----
    @Override
    public List<CustomerOrderVO> listOrders() {
        return new ArrayList<>(ORDERS);
    }

    @Override
    public Optional<CustomerOrderVO> getOrderById(Long id) {
        return ORDERS.stream().filter(o -> o.orderId().equals(id)).findFirst();
    }

    @Override
    public CustomerOrderVO createOrder(CustomerOrderRequest req) {
        throw mockWriteError();
    }

    @Override
    public CustomerOrderVO updateOrder(Long id, CustomerOrderRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteOrder(Long id) {
        throw mockWriteError();
    }

    // ---- 生产工单 ----
    @Override
    public List<WorkOrderVO> listWorkOrders() {
        return new ArrayList<>(WORK_ORDERS);
    }

    @Override
    public Optional<WorkOrderVO> getWorkOrderById(Long id) {
        return WORK_ORDERS.stream().filter(w -> w.workOrderId().equals(id)).findFirst();
    }

    @Override
    public WorkOrderVO createWorkOrder(WorkOrderRequest req) {
        throw mockWriteError();
    }

    @Override
    public WorkOrderVO updateWorkOrder(Long id, WorkOrderRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteWorkOrder(Long id) {
        throw mockWriteError();
    }

    // ---- 生产任务 ----
    @Override
    public List<ProductionTaskVO> listTasks() {
        return new ArrayList<>(TASKS);
    }

    @Override
    public Optional<ProductionTaskVO> getTaskById(Long id) {
        return TASKS.stream().filter(t -> t.taskId().equals(id)).findFirst();
    }

    @Override
    public ProductionTaskVO createTask(ProductionTaskRequest req) {
        throw mockWriteError();
    }

    @Override
    public ProductionTaskVO updateTask(Long id, ProductionTaskRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteTask(Long id) {
        throw mockWriteError();
    }

    // ---- 派工单 ----
    @Override
    public List<DispatchTaskVO> listDispatchTasks() {
        return new ArrayList<>(DISPATCH_TASKS);
    }

    @Override
    public Optional<DispatchTaskVO> getDispatchTaskById(Long id) {
        return DISPATCH_TASKS.stream().filter(d -> d.dispatchId().equals(id)).findFirst();
    }

    @Override
    public DispatchTaskVO createDispatchTask(DispatchTaskRequest req) {
        throw mockWriteError();
    }

    @Override
    public DispatchTaskVO updateDispatchTask(Long id, DispatchTaskRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteDispatchTask(Long id) {
        throw mockWriteError();
    }

    private BusinessException mockWriteError() {
        return new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }

    public List<CustomerOrderItemVO> listOrderItems(Long orderId) {
        return List.of(new CustomerOrderItemVO(
                orderId, orderId, 1L, "FAN-FS40-A", "台扇 FS-001", 500L, "标准款"));
    }
}
