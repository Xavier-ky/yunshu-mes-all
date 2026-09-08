package com.yunshu.mes.planning.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.planning.dto.CustomerOrderRequest;
import com.yunshu.mes.planning.dto.DispatchTaskRequest;
import com.yunshu.mes.planning.dto.ProductionTaskRequest;
import com.yunshu.mes.planning.dto.WorkOrderRequest;
import com.yunshu.mes.planning.service.PlanningService;
import com.yunshu.mes.planning.service.PlanningSummaryService;
import com.yunshu.mes.planning.vo.CustomerOrderItemVO;
import com.yunshu.mes.planning.vo.CustomerOrderVO;
import com.yunshu.mes.planning.vo.DispatchTaskVO;
import com.yunshu.mes.planning.vo.ProductionTaskVO;
import com.yunshu.mes.planning.vo.WorkOrderVO;
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
 * 计划管理控制器 — 客户订单、生产工单、生产任务、派工单 完整 CRUD。
 */
@RestController
@RequestMapping("/api/planning")
public class PlanningController {

    private final PlanningService planningService;
    private final PlanningSummaryService summaryService;

    public PlanningController(PlanningService planningService, PlanningSummaryService summaryService) {
        this.planningService = planningService;
        this.summaryService = summaryService;
    }

    // ==================== 客户订单 ====================

    @GetMapping("/orders")
    public ApiResponse<List<CustomerOrderVO>> listOrders(HttpServletRequest request) {
        return ApiResponse.success(planningService.listOrders(), request);
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<CustomerOrderVO> getOrder(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                planningService.getOrderById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "客户订单不存在")),
                request);
    }

    @PostMapping("/orders")
    public ApiResponse<CustomerOrderVO> createOrder(@Valid @RequestBody CustomerOrderRequest req, HttpServletRequest request) {
        return ApiResponse.success(planningService.createOrder(req), request);
    }

    @PutMapping("/orders/{id}")
    public ApiResponse<CustomerOrderVO> updateOrder(@PathVariable Long id, @Valid @RequestBody CustomerOrderRequest req,
                                                     HttpServletRequest request) {
        return ApiResponse.success(planningService.updateOrder(id, req), request);
    }

    @DeleteMapping("/orders/{id}")
    public ApiResponse<Void> deleteOrder(@PathVariable Long id, HttpServletRequest request) {
        planningService.deleteOrder(id);
        return ApiResponse.success(null, request);
    }

    @GetMapping("/orders/{id}/items")
    public ApiResponse<List<CustomerOrderItemVO>> listOrderItems(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(summaryService.listOrderItems(id), request);
    }

    // ==================== 生产工单 ====================

    @GetMapping("/work-orders")
    public ApiResponse<List<WorkOrderVO>> listWorkOrders(HttpServletRequest request) {
        return ApiResponse.success(planningService.listWorkOrders(), request);
    }

    @GetMapping("/work-orders/{id}")
    public ApiResponse<WorkOrderVO> getWorkOrder(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                planningService.getWorkOrderById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "生产工单不存在")),
                request);
    }

    @PostMapping("/work-orders")
    public ApiResponse<WorkOrderVO> createWorkOrder(@Valid @RequestBody WorkOrderRequest req, HttpServletRequest request) {
        return ApiResponse.success(planningService.createWorkOrder(req), request);
    }

    @PutMapping("/work-orders/{id}")
    public ApiResponse<WorkOrderVO> updateWorkOrder(@PathVariable Long id, @Valid @RequestBody WorkOrderRequest req,
                                                     HttpServletRequest request) {
        return ApiResponse.success(planningService.updateWorkOrder(id, req), request);
    }

    @DeleteMapping("/work-orders/{id}")
    public ApiResponse<Void> deleteWorkOrder(@PathVariable Long id, HttpServletRequest request) {
        planningService.deleteWorkOrder(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 生产任务 ====================

    @GetMapping("/production-tasks")
    public ApiResponse<List<ProductionTaskVO>> listTasks(HttpServletRequest request) {
        return ApiResponse.success(planningService.listTasks(), request);
    }

    @GetMapping("/production-tasks/{id}")
    public ApiResponse<ProductionTaskVO> getTask(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                planningService.getTaskById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "生产任务不存在")),
                request);
    }

    @PostMapping("/production-tasks")
    public ApiResponse<ProductionTaskVO> createTask(@Valid @RequestBody ProductionTaskRequest req, HttpServletRequest request) {
        return ApiResponse.success(planningService.createTask(req), request);
    }

    @PutMapping("/production-tasks/{id}")
    public ApiResponse<ProductionTaskVO> updateTask(@PathVariable Long id, @Valid @RequestBody ProductionTaskRequest req,
                                                     HttpServletRequest request) {
        return ApiResponse.success(planningService.updateTask(id, req), request);
    }

    @DeleteMapping("/production-tasks/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id, HttpServletRequest request) {
        planningService.deleteTask(id);
        return ApiResponse.success(null, request);
    }

    // ==================== 派工单 ====================

    @GetMapping("/dispatch-tasks")
    public ApiResponse<List<DispatchTaskVO>> listDispatchTasks(HttpServletRequest request) {
        return ApiResponse.success(planningService.listDispatchTasks(), request);
    }

    @GetMapping("/dispatch-tasks/{id}")
    public ApiResponse<DispatchTaskVO> getDispatchTask(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(
                planningService.getDispatchTaskById(id)
                        .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                                com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "派工单不存在")),
                request);
    }

    @PostMapping("/dispatch-tasks")
    public ApiResponse<DispatchTaskVO> createDispatchTask(@Valid @RequestBody DispatchTaskRequest req, HttpServletRequest request) {
        return ApiResponse.success(planningService.createDispatchTask(req), request);
    }

    @PutMapping("/dispatch-tasks/{id}")
    public ApiResponse<DispatchTaskVO> updateDispatchTask(@PathVariable Long id, @Valid @RequestBody DispatchTaskRequest req,
                                                           HttpServletRequest request) {
        return ApiResponse.success(planningService.updateDispatchTask(id, req), request);
    }

    @DeleteMapping("/dispatch-tasks/{id}")
    public ApiResponse<Void> deleteDispatchTask(@PathVariable Long id, HttpServletRequest request) {
        planningService.deleteDispatchTask(id);
        return ApiResponse.success(null, request);
    }
}
