package com.yunshu.mes.agent.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fixed, read-only data contract for the companion's “today production overview” preset.
 * It deliberately never returns fallback/mock data: callers must know if live data is unavailable.
 */
@RestController
@RequestMapping("/api/agent/companion")
public class CompanionProductionOverviewController {

    /*
     * A work order normally creates one production task and one feedback record per process step.
     * Production overview therefore uses the final route step as the finished-product boundary.
     * Summing every feedback record would count the same physical product once at each process.
     */
    private static final String OVERVIEW_SQL = """
            WITH default_product_routes AS (
              SELECT product_id, MAX(route_id) AS route_id
              FROM product_route
              WHERE is_default = 1 AND status = 'ENABLED'
              GROUP BY product_id
            ),
            work_order_routes AS (
              SELECT wo.work_order_id, COALESCE(wo.route_id, dpr.route_id) AS route_id
              FROM work_order wo
              LEFT JOIN default_product_routes dpr ON dpr.product_id = wo.product_id
              WHERE wo.is_deleted = 0
            ),
            final_route_steps AS (
              SELECT prs.route_id, prs.step_id
              FROM process_route_step prs
              JOIN (
                SELECT route_id, MAX(step_seq) AS max_step_seq
                FROM process_route_step
                GROUP BY route_id
              ) final_step ON final_step.route_id = prs.route_id
                         AND final_step.max_step_seq = prs.step_seq
            )
            SELECT
              (SELECT COALESCE(SUM(pt.task_qty), 0)
               FROM production_task pt
               JOIN work_order_routes wor ON wor.work_order_id = pt.work_order_id
               JOIN final_route_steps frs ON frs.route_id = wor.route_id AND frs.step_id = pt.step_id
               WHERE pt.task_date = CURDATE()
                 AND pt.status <> 'CANCELLED') AS today_plan_qty,
              (SELECT COALESCE(SUM(pf.quantity_qualified), 0)
               FROM pro_feedback pf
               JOIN work_order_routes wor ON wor.work_order_id = pf.workorder_id
               JOIN final_route_steps frs ON frs.route_id = wor.route_id AND frs.step_id = pf.process_id
               WHERE pf.status = 'FINISHED'
                 AND DATE(pf.feedback_time) = CURDATE()) AS today_completed_qty,
              (SELECT COUNT(DISTINCT wo.work_order_id)
               FROM work_order wo
               JOIN production_task pt ON pt.work_order_id = wo.work_order_id
               WHERE wo.status IN ('RUNNING', 'DISPATCHED', 'CREATED')
                 AND wo.is_deleted = 0
                 AND pt.status IN ('RUNNING', 'DISPATCHED', 'CREATED')) AS active_work_order_count,
              (SELECT COUNT(*) FROM andon_event
               WHERE status NOT IN ('CLOSED', 'RESOLVED')) AS open_andon_count,
              (SELECT COUNT(*) FROM inventory_batch
               WHERE available_qty < 100) AS low_inventory_batch_count,
              (SELECT COUNT(*) FROM qc_ipqc
               WHERE status IN ('PREPARE', 'APPROVING')) AS pending_quality_task_count,
             (SELECT COUNT(*) FROM dv_machinery WHERE status IN ('REPAIR', 'STOP')) AS fault_device_count
            """;

    private static final String TODAY_COMPLETED_PRODUCT_SQL = """
            WITH default_product_routes AS (
              SELECT product_id, MAX(route_id) AS route_id
              FROM product_route
              WHERE is_default = 1 AND status = 'ENABLED'
              GROUP BY product_id
            ),
            work_order_routes AS (
              SELECT wo.work_order_id, wo.work_order_no, wo.product_id,
                     COALESCE(wo.route_id, dpr.route_id) AS route_id
              FROM work_order wo
              LEFT JOIN default_product_routes dpr ON dpr.product_id = wo.product_id
              WHERE wo.is_deleted = 0
            ),
            final_route_steps AS (
              SELECT prs.route_id, prs.step_id
              FROM process_route_step prs
              JOIN (
                SELECT route_id, MAX(step_seq) AS max_step_seq
                FROM process_route_step
                GROUP BY route_id
              ) final_step ON final_step.route_id = prs.route_id
                         AND final_step.max_step_seq = prs.step_seq
            )
            SELECT COALESCE(p.product_name, '') AS product_name,
                   COALESCE(SUM(pf.quantity_qualified), 0) AS completed_qty,
                   GROUP_CONCAT(DISTINCT wor.work_order_no ORDER BY wor.work_order_no SEPARATOR ', ') AS work_order_nos
            FROM pro_feedback pf
            JOIN work_order_routes wor ON wor.work_order_id = pf.workorder_id
            JOIN final_route_steps frs ON frs.route_id = wor.route_id AND frs.step_id = pf.process_id
            LEFT JOIN product p ON p.product_id = wor.product_id
            WHERE pf.status = 'FINISHED'
              AND DATE(pf.feedback_time) = CURDATE()
            GROUP BY p.product_id, p.product_name
            ORDER BY completed_qty DESC, product_name
            LIMIT 5
            """;

    private static final String ACTIVE_WORK_ORDER_SQL = """
            SELECT wo.work_order_no, COALESCE(p.product_name, '') AS product_name,
                   COALESCE(l.line_name, '') AS line_name, wo.plan_qty,
                   COALESCE(wo.completed_qty, 0) AS completed_qty, wo.status
            FROM work_order wo
            LEFT JOIN product p ON p.product_id = wo.product_id
            LEFT JOIN (
                SELECT work_order_id, MAX(line_id) AS line_id
                FROM production_task
                WHERE status IN ('RUNNING', 'DISPATCHED', 'CREATED')
                GROUP BY work_order_id
            ) pt ON pt.work_order_id = wo.work_order_id
            LEFT JOIN production_line l ON l.line_id = pt.line_id
            WHERE wo.status IN ('RUNNING', 'DISPATCHED', 'CREATED') AND wo.is_deleted = 0
            ORDER BY wo.updated_at DESC
            LIMIT 5
            """;

    private final JdbcTemplate jdbc;

    public CompanionProductionOverviewController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/production-overview")
    public ApiResponse<Map<String, Object>> productionOverview(HttpServletRequest request) {
        try {
            Map<String, Object> row = jdbc.queryForMap(OVERVIEW_SQL);
            double planQty = number(row, "today_plan_qty");
            double completedQty = number(row, "today_completed_qty");

            Map<String, Object> overview = new LinkedHashMap<>();
            overview.put("todayPlanQty", wholeNumber(planQty));
            overview.put("todayCompletedQty", wholeNumber(completedQty));
            overview.put("achievementRate", planQty == 0 ? 0.0 : roundOneDecimal(completedQty * 100.0 / planQty));
            overview.put("activeWorkOrderCount", wholeNumber(number(row, "active_work_order_count")));
            overview.put("openAndonCount", wholeNumber(number(row, "open_andon_count")));
            overview.put("lowInventoryBatchCount", wholeNumber(number(row, "low_inventory_batch_count")));
            overview.put("pendingQualityTaskCount", wholeNumber(number(row, "pending_quality_task_count")));
            overview.put("faultDeviceCount", wholeNumber(number(row, "fault_device_count")));

            List<Map<String, Object>> activeWorkOrders = jdbc.query(ACTIVE_WORK_ORDER_SQL, (rs, index) -> {
                Map<String, Object> workOrder = new LinkedHashMap<>();
                workOrder.put("workOrderNo", rs.getString("work_order_no"));
                workOrder.put("productName", rs.getString("product_name"));
                workOrder.put("lineName", rs.getString("line_name"));
                workOrder.put("planQty", rs.getLong("plan_qty"));
                workOrder.put("completedQty", rs.getLong("completed_qty"));
                workOrder.put("status", rs.getString("status"));
                return workOrder;
            });
            List<Map<String, Object>> todayCompletedProducts = jdbc.query(
                    TODAY_COMPLETED_PRODUCT_SQL, (rs, index) -> {
                        Map<String, Object> product = new LinkedHashMap<>();
                        product.put("productName", rs.getString("product_name"));
                        product.put("completedQty", rs.getLong("completed_qty"));
                        product.put("workOrderNos", rs.getString("work_order_nos"));
                        return product;
                    });

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("dataSource", "LIVE");
            payload.put("businessDate", java.time.LocalDate.now().toString());
            payload.put("queriedAt", LocalDateTime.now().toString());
            payload.put("overview", overview);
            payload.put("todayCompletedProducts", todayCompletedProducts);
            payload.put("activeWorkOrders", activeWorkOrders);
            return ApiResponse.success(payload, request);
        } catch (DataAccessException ex) {
            return ApiResponse.fail("LIVE_DATA_UNAVAILABLE", "实时生产概览数据暂不可用", request);
        }
    }

    private static double number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    private static long wholeNumber(double value) {
        return Math.round(value);
    }

    private static double roundOneDecimal(double value) {
        return Double.parseDouble(String.format(Locale.ROOT, "%.1f", value));
    }
}
