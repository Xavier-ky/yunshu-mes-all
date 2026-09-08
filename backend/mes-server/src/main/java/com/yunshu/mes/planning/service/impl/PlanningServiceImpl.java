package com.yunshu.mes.planning.service.impl;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.planning.dto.CustomerOrderRequest;
import com.yunshu.mes.planning.dto.DispatchTaskRequest;
import com.yunshu.mes.planning.dto.ProductionTaskRequest;
import com.yunshu.mes.planning.dto.WorkOrderRequest;
import com.yunshu.mes.planning.repository.CustomerOrderRepository;
import com.yunshu.mes.planning.repository.DispatchTaskRepository;
import com.yunshu.mes.planning.repository.ProductionTaskRepository;
import com.yunshu.mes.planning.repository.WorkOrderRepository;
import com.yunshu.mes.planning.service.PlanningMockDataService;
import com.yunshu.mes.planning.service.PlanningService;
import com.yunshu.mes.planning.vo.CustomerOrderVO;
import com.yunshu.mes.planning.vo.DispatchTaskVO;
import com.yunshu.mes.planning.vo.ProductionTaskVO;
import com.yunshu.mes.planning.vo.WorkOrderVO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 计划管理域服务门面：优先 JDBC 仓储，异常时回退 Mock。
 */
@Service
@Primary
public class PlanningServiceImpl implements PlanningService {

    private static final Logger log = LoggerFactory.getLogger(PlanningServiceImpl.class);

    private final CustomerOrderRepository orderRepo;
    private final WorkOrderRepository workOrderRepo;
    private final ProductionTaskRepository taskRepo;
    private final DispatchTaskRepository dispatchRepo;
    private final PlanningMockDataService mock;

    public PlanningServiceImpl(CustomerOrderRepository orderRepo, WorkOrderRepository workOrderRepo,
                                ProductionTaskRepository taskRepo, DispatchTaskRepository dispatchRepo,
                                PlanningMockDataService mock) {
        this.orderRepo = orderRepo;
        this.workOrderRepo = workOrderRepo;
        this.taskRepo = taskRepo;
        this.dispatchRepo = dispatchRepo;
        this.mock = mock;
    }

    // ==================== 客户订单 ====================

    @Override
    public List<CustomerOrderVO> listOrders() {
        try { return orderRepo.findAll(); }
        catch (DataAccessException e) { log.warn("客户订单列表回退 Mock：{}", e.getMessage()); return mock.listOrders(); }
    }

    @Override
    public Optional<CustomerOrderVO> getOrderById(Long id) {
        try { return orderRepo.findById(id).or(() -> mock.getOrderById(id)); }
        catch (DataAccessException e) { log.warn("客户订单详情回退 Mock：{}", e.getMessage()); return mock.getOrderById(id); }
    }

    @Override
    public CustomerOrderVO createOrder(CustomerOrderRequest req) {
        try {
            Long id = orderRepo.insert(req.orderNo(), req.customerName(), req.productId(), req.orderQty(), req.deliveryDate(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建客户订单失败");
            return orderRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建客户订单后回查失败"));
        } catch (DataAccessException e) { log.warn("创建客户订单回退 Mock：{}", e.getMessage()); return mock.createOrder(req); }
    }

    @Override
    public CustomerOrderVO updateOrder(Long id, CustomerOrderRequest req) {
        try {
            orderRepo.update(id, req.orderNo(), req.customerName(), req.productId(), req.orderQty(), req.deliveryDate(), req.status());
            return orderRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "客户订单不存在"));
        } catch (DataAccessException e) { log.warn("更新客户订单回退 Mock：{}", e.getMessage()); return mock.updateOrder(id, req); }
    }

    @Override
    public void deleteOrder(Long id) {
        try { orderRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除客户订单回退 Mock：{}", e.getMessage()); mock.deleteOrder(id); }
    }

    // ==================== 生产工单 ====================

    @Override
    public List<WorkOrderVO> listWorkOrders() {
        try { return workOrderRepo.findAll(); }
        catch (DataAccessException e) { log.warn("生产工单列表回退 Mock：{}", e.getMessage()); return mock.listWorkOrders(); }
    }

    @Override
    public Optional<WorkOrderVO> getWorkOrderById(Long id) {
        try { return workOrderRepo.findById(id).or(() -> mock.getWorkOrderById(id)); }
        catch (DataAccessException e) { log.warn("生产工单详情回退 Mock：{}", e.getMessage()); return mock.getWorkOrderById(id); }
    }

    @Override
    public WorkOrderVO createWorkOrder(WorkOrderRequest req) {
        try {
            Long id = workOrderRepo.insert(req.workOrderNo(), req.productId(), req.orderId(), req.orderItemId(), req.planQty(), req.status());
            if (id != null && req.orderId() != null) {
                try {
                    workOrderRepo.enrichFromOrder(id, req.orderId());
                } catch (DataAccessException ex) {
                    log.warn("工单来源字段回填跳过（需 V5 迁移）：{}", ex.getMessage());
                }
            }
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建生产工单失败");
            return workOrderRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建生产工单后回查失败"));
        } catch (DataAccessException e) { log.warn("创建生产工单回退 Mock：{}", e.getMessage()); return mock.createWorkOrder(req); }
    }

    @Override
    public WorkOrderVO updateWorkOrder(Long id, WorkOrderRequest req) {
        try {
            workOrderRepo.update(id, req.workOrderNo(), req.productId(), req.orderId(), req.planQty(), req.status());
            return workOrderRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "生产工单不存在"));
        } catch (DataAccessException e) { log.warn("更新生产工单回退 Mock：{}", e.getMessage()); return mock.updateWorkOrder(id, req); }
    }

    @Override
    public void deleteWorkOrder(Long id) {
        try { workOrderRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除生产工单回退 Mock：{}", e.getMessage()); mock.deleteWorkOrder(id); }
    }

    // ==================== 生产任务 ====================

    @Override
    public List<ProductionTaskVO> listTasks() {
        try { return taskRepo.findAll(); }
        catch (DataAccessException e) { log.warn("生产任务列表回退 Mock：{}", e.getMessage()); return mock.listTasks(); }
    }

    @Override
    public Optional<ProductionTaskVO> getTaskById(Long id) {
        try { return taskRepo.findById(id).or(() -> mock.getTaskById(id)); }
        catch (DataAccessException e) { log.warn("生产任务详情回退 Mock：{}", e.getMessage()); return mock.getTaskById(id); }
    }

    @Override
    public ProductionTaskVO createTask(ProductionTaskRequest req) {
        try {
            Long id = taskRepo.insert(req.taskNo(), req.workOrderId(), req.lineId(), req.planQty(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建生产任务失败");
            return taskRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建生产任务后回查失败"));
        } catch (DataAccessException e) { log.warn("创建生产任务回退 Mock：{}", e.getMessage()); return mock.createTask(req); }
    }

    @Override
    public ProductionTaskVO updateTask(Long id, ProductionTaskRequest req) {
        try {
            taskRepo.update(id, req.taskNo(), req.workOrderId(), req.lineId(), req.planQty(), req.status());
            return taskRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "生产任务不存在"));
        } catch (DataAccessException e) { log.warn("更新生产任务回退 Mock：{}", e.getMessage()); return mock.updateTask(id, req); }
    }

    @Override
    public void deleteTask(Long id) {
        try { taskRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除生产任务回退 Mock：{}", e.getMessage()); mock.deleteTask(id); }
    }

    // ==================== 派工单 ====================

    @Override
    public List<DispatchTaskVO> listDispatchTasks() {
        try { return dispatchRepo.findAll(); }
        catch (DataAccessException e) { log.warn("派工单列表回退 Mock：{}", e.getMessage()); return mock.listDispatchTasks(); }
    }

    @Override
    public Optional<DispatchTaskVO> getDispatchTaskById(Long id) {
        try { return dispatchRepo.findById(id).or(() -> mock.getDispatchTaskById(id)); }
        catch (DataAccessException e) { log.warn("派工单详情回退 Mock：{}", e.getMessage()); return mock.getDispatchTaskById(id); }
    }

    @Override
    public DispatchTaskVO createDispatchTask(DispatchTaskRequest req) {
        try {
            var task = taskRepo.findById(req.taskId()).orElseThrow(() ->
                    new BusinessException(ErrorCode.NOT_FOUND, "生产任务不存在"));
            java.math.BigDecimal plannedQty = task.planQty() != null
                    ? java.math.BigDecimal.valueOf(task.planQty()) : java.math.BigDecimal.ZERO;
            Long id = dispatchRepo.insert(req.dispatchNo(), req.taskId(), task.workOrderId(), plannedQty, req.stepId(), req.stationId(), req.assigneeId(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建派工单失败");
            return dispatchRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建派工单后回查失败"));
        } catch (DataAccessException e) { log.warn("创建派工单回退 Mock：{}", e.getMessage()); return mock.createDispatchTask(req); }
    }

    @Override
    public DispatchTaskVO updateDispatchTask(Long id, DispatchTaskRequest req) {
        try {
            var task = taskRepo.findById(req.taskId()).orElseThrow(() ->
                    new BusinessException(ErrorCode.NOT_FOUND, "生产任务不存在"));
            java.math.BigDecimal plannedQty = task.planQty() != null
                    ? java.math.BigDecimal.valueOf(task.planQty()) : java.math.BigDecimal.ZERO;
            dispatchRepo.update(id, req.dispatchNo(), req.taskId(), task.workOrderId(), plannedQty, req.stepId(), req.stationId(), req.assigneeId(), req.status());
            return dispatchRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "派工单不存在"));
        } catch (DataAccessException e) { log.warn("更新派工单回退 Mock：{}", e.getMessage()); return mock.updateDispatchTask(id, req); }
    }

    @Override
    public void deleteDispatchTask(Long id) {
        try { dispatchRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除派工单回退 Mock：{}", e.getMessage()); mock.deleteDispatchTask(id); }
    }
}
