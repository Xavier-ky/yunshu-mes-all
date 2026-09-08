package com.yunshu.mes.dashboard.service;

import com.yunshu.mes.dashboard.vo.DashboardAlertVO;
import com.yunshu.mes.dashboard.vo.DashboardMetricVO;
import com.yunshu.mes.dashboard.vo.DashboardSummaryVO;
import com.yunshu.mes.dashboard.vo.DashboardWorkOrderVO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 仪表盘服务：优先从数据库查询实时指标，不可用时回退硬编码示例数据。
 *
 * <p>与大屏/产线监控统一的口径：
 * <ul>
 *   <li>今日产出/完工：pro_feedback 当日合格数量</li>
 *   <li>今日计划产量：当日 production_task.task_qty 之和（活跃任务）</li>
 *   <li>在制工单：work_order + production_task 活跃状态去重计数</li>
 *   <li>工单达成率：今日完工 / 今日计划</li>
 * </ul>
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private static final String WORK_ORDER_SQL = """
            SELECT wo.work_order_no, p.product_name, l.line_name,
                   wo.plan_qty, COALESCE(wo.completed_qty, 0) AS completed_qty, wo.status
            FROM work_order wo
            LEFT JOIN product p ON wo.product_id = p.product_id
            LEFT JOIN (
                SELECT work_order_id, MAX(line_id) AS line_id
                FROM production_task
                WHERE status IN ('RUNNING', 'DISPATCHED', 'CREATED')
                GROUP BY work_order_id
            ) pt ON pt.work_order_id = wo.work_order_id
            LEFT JOIN production_line l ON pt.line_id = l.line_id
            WHERE wo.status IN ('RUNNING', 'DISPATCHED', 'CREATED')
              AND wo.is_deleted = 0
            ORDER BY wo.updated_at DESC
            LIMIT 10
            """;

    private static final String METRIC_SQL = """
            SELECT
              (SELECT COUNT(DISTINCT wo.work_order_id)
               FROM work_order wo
               JOIN production_task pt ON pt.work_order_id = wo.work_order_id
               WHERE wo.status IN ('RUNNING', 'DISPATCHED', 'CREATED')
                 AND wo.is_deleted = 0
                 AND pt.status IN ('RUNNING', 'DISPATCHED', 'CREATED')) AS active_wo_count,
              (SELECT COALESCE(SUM(pt.task_qty), 0)
               FROM production_task pt
               WHERE pt.status IN ('RUNNING', 'DISPATCHED', 'CREATED')
                 AND pt.task_date = CURDATE()) AS today_plan_qty,
              (SELECT COALESCE(SUM(pf.quantity_qualified), 0)
               FROM pro_feedback pf
               WHERE pf.status = 'FINISHED'
                 AND DATE(pf.feedback_time) = CURDATE()) AS today_completed,
              (SELECT COUNT(*)
               FROM andon_event
               WHERE status NOT IN ('CLOSED', 'RESOLVED')) AS open_andons
            """;

    private static final String ALERT_SQL = """
            SELECT 'MATERIAL' AS alert_type, CONCAT('物料批次 ', ib.batch_no, ' 库存低于安全线') AS message,
                   'HIGH' AS severity, '' AS assignee, 'OPEN' AS status
            FROM inventory_batch ib WHERE ib.available_qty < 100
            UNION ALL
            SELECT 'QUALITY', CONCAT('过程质检 ', q.ipqc_code, ' 待完成'), 'MEDIUM', '', 'OPEN'
            FROM qc_ipqc q WHERE q.status NOT IN ('FINISHED', 'CANCELED')
            UNION ALL
            SELECT 'EQUIPMENT', CONCAT('设备 ', dm.machinery_name, ' 待保养/停机'), 'LOW', '', 'OPEN'
            FROM dv_machinery dm WHERE dm.status IN ('REPAIR', 'STOP')
            LIMIT 10
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public DashboardService(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public DashboardSummaryVO getSummary() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) return fallbackSummary();
        try {
            return new DashboardSummaryVO(queryMetrics(jdbc), queryWorkOrders(jdbc), queryAlerts(jdbc));
        } catch (DataAccessException e) {
            log.warn("仪表盘查询回退 Mock：{}", e.getMessage());
            return fallbackSummary();
        }
    }

    private List<DashboardMetricVO> queryMetrics(JdbcTemplate jdbc) {
        try {
            return jdbc.query(METRIC_SQL, (rs, n) -> {
                long planQty = rs.getLong("today_plan_qty");
                long completed = rs.getLong("today_completed");
                String rate = planQty > 0 ? String.format("%.1f", 100.0 * completed / planQty) : "0.0";
                return List.of(
                        new DashboardMetricVO("今日计划产量", String.valueOf(planQty), "台", "—", "NORMAL"),
                        new DashboardMetricVO("今日完工数量", String.valueOf(completed), "台", "—", "NORMAL"),
                        new DashboardMetricVO("工单达成率", rate, "%", "—", "NORMAL"),
                        new DashboardMetricVO("安灯待处理", String.valueOf(rs.getLong("open_andons")), "件", "—", "NORMAL")
                );
            }).stream().flatMap(List::stream).toList();
        } catch (DataAccessException e) {
            log.warn("仪表盘指标查询回退 Mock：{}", e.getMessage());
            return fallbackMetrics();
        }
    }

    private List<DashboardWorkOrderVO> queryWorkOrders(JdbcTemplate jdbc) {
        try {
            return jdbc.query(WORK_ORDER_SQL, (rs, n) -> new DashboardWorkOrderVO(
                    rs.getString("work_order_no"), rs.getString("product_name"),
                    rs.getString("line_name"), String.valueOf(rs.getLong("plan_qty")),
                    String.valueOf(rs.getLong("completed_qty")), rs.getString("status")));
        } catch (DataAccessException e) {
            log.warn("在制工单查询回退 Mock：{}", e.getMessage());
            return fallbackOrders();
        }
    }

    private List<DashboardAlertVO> queryAlerts(JdbcTemplate jdbc) {
        try {
            return jdbc.query(ALERT_SQL, (rs, n) -> new DashboardAlertVO(
                    rs.getString("alert_type"), rs.getString("message"),
                    rs.getString("severity"), rs.getString("assignee"), rs.getString("status")));
        } catch (DataAccessException e) {
            return fallbackAlerts();
        }
    }

    private DashboardSummaryVO fallbackSummary() {
        return new DashboardSummaryVO(fallbackMetrics(), fallbackOrders(), fallbackAlerts());
    }

    private List<DashboardMetricVO> fallbackMetrics() {
        return List.of(
                new DashboardMetricVO("今日计划产量", "3200", "台", "+8.4%", "NORMAL"),
                new DashboardMetricVO("今日完工数量", "2768", "台", "+5.1%", "NORMAL"),
                new DashboardMetricVO("工单达成率", "86.5", "%", "+3.2%", "NORMAL"),
                new DashboardMetricVO("安灯待处理", "6", "件", "+2", "ALERT")
        );
    }

    private List<DashboardWorkOrderVO> fallbackOrders() {
        return List.of(
                new DashboardWorkOrderVO("WO20260706001", "16寸落地扇 FS-16A", "总装一线", "1200", "860", "RUNNING"),
                new DashboardWorkOrderVO("WO20260706002", "空气循环扇 AC-12B", "总装二线", "800", "240", "DISPATCHED"),
                new DashboardWorkOrderVO("WO20260706003", "台式风扇 DF-09C", "包装一线", "600", "600", "COMPLETED")
        );
    }

    private List<DashboardAlertVO> fallbackAlerts() {
        return List.of(
                new DashboardAlertVO("MATERIAL", "电机批次 MTR-20260706 欠料 320 件", "HIGH", "仓库物料员", "OPEN"),
                new DashboardAlertVO("QUALITY", "总装一线噪音检测出现连续不良", "MEDIUM", "质检员", "PROCESSING"),
                new DashboardAlertVO("EQUIPMENT", "包装一线封箱机待点检", "LOW", "设备维修员", "OPEN")
        );
    }
}
