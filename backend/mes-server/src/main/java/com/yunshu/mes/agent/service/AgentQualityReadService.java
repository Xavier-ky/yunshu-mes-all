package com.yunshu.mes.agent.service;

import com.yunshu.mes.quality.compat.controller.QcAnalyticsController;
import com.yunshu.mes.quality.compat.controller.QcPendingController;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Read-only quality facts made specifically for Agent orchestration.
 *
 * <p>The quality pages expose several analytical views. This facade combines
 * their existing live SQL-backed calculations into a bounded fact package and
 * does not call a quality command, lifecycle transition or page write-back.
 */
@Service
public class AgentQualityReadService {

    private final JdbcTemplate jdbc;
    private final QcPendingController pendingController;
    private final QcAnalyticsController analyticsController;

    public AgentQualityReadService(
            JdbcTemplate jdbc,
            QcPendingController pendingController,
            QcAnalyticsController analyticsController) {
        this.jdbc = jdbc;
        this.pendingController = pendingController;
        this.analyticsController = analyticsController;
    }

    public Map<String, Object> overview(int days) {
        int windowDays = Math.min(Math.max(days, 1), 30);
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("windowDays", windowDays);
        facts.put("summary", dataMap(analyticsController.summary()));
        facts.put("trend", dataRows(analyticsController.trend(windowDays)));
        facts.put("defectTop", dataRows(analyticsController.defectTop(8)));
        facts.put("defectByLevel", dataRows(analyticsController.defectByLevel(windowDays)));
        facts.put("typeStats", dataMap(analyticsController.typeStats()));
        facts.put("pendingTasks", pendingRows());
        return snapshot(facts);
    }

    /**
     * Bounded report source package. The values come exclusively from the
     * existing SQL-backed quality workbench statistics; this method never
     * creates, updates or disposes a quality record.
     */
    public Map<String, Object> reportSnapshot(int days) {
        int windowDays = Math.min(Math.max(days, 1), 30);
        Map<String, Object> facts = new LinkedHashMap<>(overview(windowDays));
        facts.put("recentFinished", dataRows(analyticsController.recentFinished(12)));
        facts.put("pendingDispositionRows", dataRows(analyticsController.pendingDisposition(12)));
        facts.put("reportSourceVersion", "QUALITY_REPORT_SNAPSHOT_V1");
        return snapshot(facts);
    }

    /**
     * Work-order quality view is deliberately narrow: it confirms that the
     * work order exists and filters the same live pending queue by work-order
     * identity. It never creates an IPQC task or modifies a QC result.
     */
    public Map<String, Object> workOrderQuality(String workOrderNo, int days) {
        List<Map<String, Object>> workOrders = jdbc.queryForList("""
                SELECT wo.work_order_id AS workOrderId, wo.work_order_no AS workOrderNo,
                       wo.lifecycle_status AS lifecycleStatus, wo.plan_qty AS planQty,
                       p.product_code AS productCode, p.product_name AS productName
                FROM work_order wo
                JOIN product p ON p.product_id = wo.product_id
                WHERE wo.work_order_no = ? AND wo.is_deleted = 0
                """, workOrderNo);
        if (workOrders.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("workOrder", workOrders.get(0));
        List<Map<String, Object>> pending = new ArrayList<>();
        for (Map<String, Object> row : pendingRows()) {
            if (workOrderNo.equalsIgnoreCase(String.valueOf(row.get("workOrderCode")))) {
                pending.add(row);
            }
        }
        facts.put("pendingTasks", pending);
        facts.put("pendingCount", pending.size());
        facts.put("qualityOverview", overview(days));
        return snapshot(facts);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> pendingRows() {
        Map<String, Object> payload = pendingController.list(Map.of());
        Object raw = payload.get("rows");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> row = new LinkedHashMap<>();
                map.forEach((key, value) -> row.put(String.valueOf(key), value));
                rows.add(row);
            }
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dataMap(Map<String, Object> payload) {
        Object raw = payload.get("data");
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> dataRows(Map<String, Object> payload) {
        Object raw = payload.get("data");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> row = new LinkedHashMap<>();
                map.forEach((key, value) -> row.put(String.valueOf(key), value));
                rows.add(row);
            }
        }
        return rows;
    }

    private static Map<String, Object> snapshot(Map<String, Object> facts) {
        Map<String, Object> result = new LinkedHashMap<>(facts);
        result.put("source", "MES_AGENT_QUALITY_READ_FACADE");
        result.put("queriedAt", Instant.now().toString());
        result.put("readOnly", true);
        return result;
    }
}
