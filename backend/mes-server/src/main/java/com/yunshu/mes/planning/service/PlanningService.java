package com.yunshu.mes.planning.service;

import com.yunshu.mes.planning.dto.CustomerOrderRequest;
import com.yunshu.mes.planning.dto.DispatchTaskRequest;
import com.yunshu.mes.planning.dto.ProductionTaskRequest;
import com.yunshu.mes.planning.dto.WorkOrderRequest;
import com.yunshu.mes.planning.vo.CustomerOrderVO;
import com.yunshu.mes.planning.vo.DispatchTaskVO;
import com.yunshu.mes.planning.vo.ProductionTaskVO;
import com.yunshu.mes.planning.vo.WorkOrderVO;
import java.util.List;
import java.util.Optional;

/**
 * 计划管理域服务接口：客户订单、生产工单、生产任务、派工单。
 */
public interface PlanningService {

    // ---- 客户订单 ----
    List<CustomerOrderVO> listOrders();

    Optional<CustomerOrderVO> getOrderById(Long id);

    CustomerOrderVO createOrder(CustomerOrderRequest req);

    CustomerOrderVO updateOrder(Long id, CustomerOrderRequest req);

    void deleteOrder(Long id);

    // ---- 生产工单 ----
    List<WorkOrderVO> listWorkOrders();

    Optional<WorkOrderVO> getWorkOrderById(Long id);

    WorkOrderVO createWorkOrder(WorkOrderRequest req);

    WorkOrderVO updateWorkOrder(Long id, WorkOrderRequest req);

    void deleteWorkOrder(Long id);

    // ---- 生产任务 ----
    List<ProductionTaskVO> listTasks();

    Optional<ProductionTaskVO> getTaskById(Long id);

    ProductionTaskVO createTask(ProductionTaskRequest req);

    ProductionTaskVO updateTask(Long id, ProductionTaskRequest req);

    void deleteTask(Long id);

    // ---- 派工单 ----
    List<DispatchTaskVO> listDispatchTasks();

    Optional<DispatchTaskVO> getDispatchTaskById(Long id);

    DispatchTaskVO createDispatchTask(DispatchTaskRequest req);

    DispatchTaskVO updateDispatchTask(Long id, DispatchTaskRequest req);

    void deleteDispatchTask(Long id);
}
