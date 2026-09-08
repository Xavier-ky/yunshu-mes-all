package com.yunshu.mes.planning.service.impl;

import com.yunshu.mes.planning.repository.CustomerOrderItemRepository;
import com.yunshu.mes.planning.service.PlanningMockDataService;
import com.yunshu.mes.planning.service.PlanningSummaryService;
import com.yunshu.mes.planning.vo.CustomerOrderItemVO;
import com.yunshu.mes.planning.vo.CustomerOrderVO;
import com.yunshu.mes.planning.vo.DispatchTaskVO;
import com.yunshu.mes.planning.vo.ProductionTaskVO;
import com.yunshu.mes.planning.vo.WorkOrderVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PlanningSummaryServiceImpl implements PlanningSummaryService {

    private static final Logger log = LoggerFactory.getLogger(PlanningSummaryServiceImpl.class);
    private static final double LINE_CAPACITY = 500.0;

    private final JdbcTemplate jdbc;
    private final CustomerOrderItemRepository orderItemRepo;
    private final PlanningMockDataService mock;

    public PlanningSummaryServiceImpl(JdbcTemplate jdbc, CustomerOrderItemRepository orderItemRepo,
                                      PlanningMockDataService mock) {
        this.jdbc = jdbc;
        this.orderItemRepo = orderItemRepo;
        this.mock = mock;
    }

    @Override
    public Map<String, Object> getSummary() {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orders", buildOrderSummary());
            result.put("workOrders", buildWorkOrderSummary());
            result.put("scheduling", getSchedulingOverview());
            return result;
        } catch (DataAccessException e) {
            log.warn("Planning summary 回退 Mock：{}", e.getMessage());
            return buildMockSummary();
        }
    }

    @Override
    public List<CustomerOrderItemVO> listOrderItems(Long orderId) {
        try {
            return orderItemRepo.findByOrderId(orderId);
        } catch (DataAccessException e) {
            log.warn("订单明细回退 Mock：{}", e.getMessage());
            return mock.listOrderItems(orderId);
        }
    }

    @Override
    public Map<String, Object> getSchedulingOverview() {
        try {
            Map<String, Object> overview = new LinkedHashMap<>();

            var lineLoads = jdbc.queryForList("""
                SELECT l.line_id, l.line_name, COALESCE(SUM(pt.task_qty), 0) AS plan_qty
                FROM production_line l
                LEFT JOIN production_task pt ON pt.line_id = l.line_id AND pt.status IN ('CREATED','RUNNING','DISPATCHED')
                GROUP BY l.line_id, l.line_name
                ORDER BY l.line_id
                """);

            List<Map<String, Object>> lines = new ArrayList<>();
            double maxLoadPct = 0;
            for (var row : lineLoads) {
                double planQty = ((Number) row.get("plan_qty")).doubleValue();
                double loadPct = Math.min(100, Math.round(planQty / LINE_CAPACITY * 1000) / 10.0);
                maxLoadPct = Math.max(maxLoadPct, loadPct);
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("lineId", row.get("line_id"));
                line.put("lineName", row.get("line_name"));
                line.put("planQty", planQty);
                line.put("loadPct", loadPct);
                lines.add(line);
            }

            Number pendingWo = jdbc.queryForObject("""
                SELECT COUNT(*) FROM work_order wo
                WHERE wo.status = 'DISPATCHED'
                AND NOT EXISTS (SELECT 1 FROM production_task pt WHERE pt.work_order_id = wo.work_order_id)
                """, Number.class);

            var dispatchStats = jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(completed_qty), 0) AS completed,
                       COALESCE(SUM(planned_qty), 0) AS planned
                FROM dispatch_task
                """);

            double planned = ((Number) dispatchStats.get("planned")).doubleValue();
            double completed = ((Number) dispatchStats.get("completed")).doubleValue();
            double dispatchRate = planned > 0 ? Math.round(completed / planned * 1000) / 10.0 : 0;

            overview.put("lineLoads", lines);
            overview.put("maxLineLoadPct", maxLoadPct);
            overview.put("pendingScheduleCount", pendingWo);
            overview.put("dispatchTotal", dispatchStats.get("total"));
            overview.put("dispatchCompleteRate", dispatchRate);
            return overview;
        } catch (DataAccessException e) {
            log.warn("Scheduling overview 回退 Mock：{}", e.getMessage());
            return buildMockSchedulingOverview();
        }
    }

    private Map<String, Object> buildOrderSummary() {
        Map<String, Object> orders = new LinkedHashMap<>();
        orders.put("total", jdbc.queryForObject("SELECT COUNT(*) FROM customer_order", Long.class));

        orders.put("pendingConfirm", jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_order WHERE status = 'CREATED'", Long.class));

        orders.put("overdue", jdbc.queryForObject("""
            SELECT COUNT(*) FROM customer_order
            WHERE status NOT IN ('COMPLETED','CANCELLED')
            AND delivery_date IS NOT NULL AND delivery_date < CURDATE()
            """, Long.class));

        var statusRows = jdbc.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM customer_order GROUP BY status");
        List<Map<String, Object>> statusDist = new ArrayList<>();
        for (var row : statusRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", row.get("status"));
            item.put("count", row.get("cnt"));
            statusDist.add(item);
        }
        orders.put("statusDistribution", statusDist);

        var weekRows = jdbc.queryForList("""
            SELECT DATE_FORMAT(delivery_date, '%Y-%u') AS week_key,
                   COUNT(*) AS cnt
            FROM customer_order
            WHERE delivery_date >= DATE_SUB(CURDATE(), INTERVAL 28 DAY)
            GROUP BY week_key ORDER BY week_key
            """);
        orders.put("deliveryByWeek", weekRows);
        return orders;
    }

    private Map<String, Object> buildWorkOrderSummary() {
        Map<String, Object> wo = new LinkedHashMap<>();
        wo.put("total", jdbc.queryForObject("SELECT COUNT(*) FROM work_order", Long.class));
        wo.put("running", jdbc.queryForObject("SELECT COUNT(*) FROM work_order WHERE status = 'RUNNING'", Long.class));
        wo.put("dispatched", jdbc.queryForObject("SELECT COUNT(*) FROM work_order WHERE status = 'DISPATCHED'", Long.class));
        wo.put("pendingKitting", jdbc.queryForObject("""
            SELECT COUNT(*) FROM work_order wo
            WHERE wo.status = 'CREATED'
            AND NOT EXISTS (
                SELECT 1 FROM kitting_analysis ka WHERE ka.work_order_id = wo.work_order_id
            )
            """, Long.class));

        var totals = jdbc.queryForMap("""
            SELECT COALESCE(SUM(plan_qty),0) AS plan_qty, COALESCE(SUM(completed_qty),0) AS completed_qty
            FROM work_order WHERE status NOT IN ('CANCELLED')
            """);
        long plan = ((Number) totals.get("plan_qty")).longValue();
        long done = ((Number) totals.get("completed_qty")).longValue();
        wo.put("avgCompletionRate", plan > 0 ? Math.round(done * 1000.0 / plan) / 10.0 : 0);

        var statusRows = jdbc.queryForList("SELECT status, COUNT(*) AS cnt FROM work_order GROUP BY status");
        wo.put("statusDistribution", statusRows);

        var shortages = jdbc.queryForList("""
            SELECT m.material_name, SUM(ms.shortage_qty) AS shortage_qty
            FROM material_shortage ms
            JOIN material m ON ms.material_id = m.material_id
            WHERE ms.status = 'OPEN'
            GROUP BY m.material_id, m.material_name
            ORDER BY shortage_qty DESC LIMIT 5
            """);
        wo.put("topShortages", shortages);
        return wo;
    }

    private Map<String, Object> buildMockSummary() {
        List<CustomerOrderVO> orders = mock.listOrders();
        List<WorkOrderVO> wos = mock.listWorkOrders();
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> orderSum = new LinkedHashMap<>();
        orderSum.put("total", orders.size());
        orderSum.put("pendingConfirm", orders.stream().filter(o -> "CREATED".equals(o.status())).count());
        orderSum.put("overdue", 0L);
        orderSum.put("statusDistribution", List.of(
                Map.of("status", "CREATED", "count", 1),
                Map.of("status", "CONFIRMED", "count", 2)));
        orderSum.put("deliveryByWeek", List.of());
        result.put("orders", orderSum);

        Map<String, Object> woSum = new LinkedHashMap<>();
        woSum.put("total", wos.size());
        woSum.put("running", wos.stream().filter(w -> "RUNNING".equals(w.status())).count());
        woSum.put("dispatched", 0L);
        woSum.put("pendingKitting", wos.size());
        woSum.put("avgCompletionRate", 0);
        woSum.put("statusDistribution", List.of());
        woSum.put("topShortages", List.of());
        result.put("workOrders", woSum);
        result.put("scheduling", buildMockSchedulingOverview());
        return result;
    }

    private Map<String, Object> buildMockSchedulingOverview() {
        List<ProductionTaskVO> tasks = mock.listTasks();
        List<DispatchTaskVO> dispatches = mock.listDispatchTasks();
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("lineLoads", List.of(Map.of("lineName", "A线", "loadPct", 45)));
        overview.put("maxLineLoadPct", 45);
        overview.put("pendingScheduleCount", 1);
        overview.put("dispatchTotal", dispatches.size());
        double planned = dispatches.stream().mapToLong(d -> d.plannedQty() != null ? d.plannedQty() : 0).sum();
        double completed = dispatches.stream().mapToLong(d -> d.completedQty() != null ? d.completedQty() : 0).sum();
        overview.put("dispatchCompleteRate", planned > 0 ? Math.round(completed / planned * 1000) / 10.0 : 0);
        overview.put("taskCount", tasks.size());
        return overview;
    }
}
