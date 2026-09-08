package com.yunshu.mes.planning.compat.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WorkorderProgressRepository {

    private final JdbcTemplate jdbc;

    public WorkorderProgressRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> tasksForWorkorder(Long workorderId) {
        if (!tableExists("pro_route_product")) {
            return tasksFromProductionTask(workorderId);
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT a.process_id AS processId, a.process_code AS processCode, a.process_name AS processName,
                       IFNULL(b.quantity, a.plan_qty) AS quantity,
                       IFNULL(b.quantity_produced, 0) AS quantityProduced
                FROM (
                    SELECT wo.work_order_id, wo.plan_qty, psp.process_id, psp.process_code, psp.process_name, psp.order_num
                    FROM work_order wo
                    JOIN pro_route_product prp ON prp.item_id = wo.product_id
                    JOIN pro_route_process psp ON psp.route_id = prp.route_id
                    WHERE wo.work_order_id = ?
                    UNION
                    SELECT wo.work_order_id, wo.plan_qty, ps.step_id, ps.step_code, ps.step_name, prs.step_seq
                    FROM work_order wo
                    JOIN process_route_step prs ON prs.route_id = wo.route_id
                    JOIN process_step ps ON ps.step_id = prs.step_id
                    WHERE wo.work_order_id = ?
                      AND NOT EXISTS (SELECT 1 FROM pro_route_product prp2 WHERE prp2.item_id = wo.product_id)
                ) a
                LEFT JOIN (
                    SELECT pt.work_order_id, pt.step_id AS process_id,
                           SUM(pt.task_qty) AS quantity,
                           SUM(pt.completed_qty) AS quantity_produced
                    FROM production_task pt
                    WHERE pt.work_order_id = ?
                    GROUP BY pt.work_order_id, pt.step_id
                ) b ON b.work_order_id = a.work_order_id AND b.process_id = a.process_id
                ORDER BY a.order_num
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("processId", rs.getLong("processId"));
            m.put("processCode", rs.getString("processCode"));
            m.put("processName", rs.getString("processName"));
            m.put("quantity", rs.getBigDecimal("quantity"));
            m.put("quantityProduced", rs.getBigDecimal("quantityProduced"));
            return m;
        }, workorderId, workorderId, workorderId);
        if (rows.isEmpty()) {
            return tasksFromProductionTask(workorderId);
        }
        return rows;
    }

    public List<Map<String, Object>> routeHomeForWorkorder(Long workorderId, BigDecimal totalQty) {
        List<Map<String, Object>> tasks = tasksForWorkorder(workorderId);
        Map<Long, BigDecimal> feedbackByProcess = feedbackByProcess(workorderId);
        List<Map<String, Object>> routeHomg = new ArrayList<>();
        for (Map<String, Object> task : tasks) {
            long processId = ((Number) task.get("processId")).longValue();
            BigDecimal complete = feedbackByProcess.getOrDefault(processId, (BigDecimal) task.get("quantityProduced"));
            if (complete == null) {
                complete = BigDecimal.ZERO;
            }
            BigDecimal total = totalQty != null ? totalQty : BigDecimal.ZERO;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("processId", processId);
            row.put("processCode", task.get("processCode"));
            row.put("processName", task.get("processName"));
            row.put("total", total);
            row.put("completeNumber", complete);
            row.put("incompleteNumber", total.subtract(complete).max(BigDecimal.ZERO));
            routeHomg.add(row);
        }
        return routeHomg;
    }

    private Map<Long, BigDecimal> feedbackByProcess(Long workorderId) {
        if (!tableExists("pro_feedback")) {
            return Map.of();
        }
        try {
            List<Map<String, Object>> rows = jdbc.query("""
                    SELECT process_id, SUM(IFNULL(quantity_feedback, 0)) AS qty
                    FROM pro_feedback WHERE workorder_id = ? GROUP BY process_id
                    """, (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("processId", rs.getLong("process_id"));
                m.put("qty", rs.getBigDecimal("qty"));
                return m;
            }, workorderId);
            Map<Long, BigDecimal> map = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                map.put(((Number) row.get("processId")).longValue(), (BigDecimal) row.get("qty"));
            }
            return map;
        } catch (DataAccessException ex) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> tasksFromProductionTask(Long workorderId) {
        try {
            return jdbc.query("""
                    SELECT ps.step_id AS processId, ps.step_code AS processCode, ps.step_name AS processName,
                           SUM(pt.task_qty) AS quantity, SUM(pt.completed_qty) AS quantityProduced
                    FROM production_task pt
                    JOIN process_step ps ON ps.step_id = pt.step_id
                    WHERE pt.work_order_id = ?
                    GROUP BY ps.step_id, ps.step_code, ps.step_name
                    ORDER BY MIN(pt.task_id)
                    """, (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("processId", rs.getLong("processId"));
                m.put("processCode", rs.getString("processCode"));
                m.put("processName", rs.getString("processName"));
                m.put("quantity", rs.getBigDecimal("quantity"));
                m.put("quantityProduced", rs.getBigDecimal("quantityProduced"));
                return m;
            }, workorderId);
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private boolean tableExists(String table) {
        try {
            jdbc.queryForObject("SELECT 1 FROM " + table + " LIMIT 1", Integer.class);
            return true;
        } catch (DataAccessException ex) {
            return false;
        }
    }
}
