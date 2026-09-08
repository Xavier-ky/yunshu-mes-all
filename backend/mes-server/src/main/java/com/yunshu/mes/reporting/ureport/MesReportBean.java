package com.yunshu.mes.reporting.ureport;

import com.yunshu.mes.inventory.compat.service.WmBarcodeService;
import com.yunshu.mes.planning.repository.WorkOrderRepository;
import com.yunshu.mes.planning.vo.WorkOrderVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * UReport Spring Bean 数据集 — 工单 / 子工单 / 条码 QC。
 */
@Component
public class MesReportBean {

    private final WorkOrderRepository workOrderRepository;
    private final JdbcTemplate jdbc;
    private final WmBarcodeService wmBarcodeService;

    public MesReportBean(WorkOrderRepository workOrderRepository, JdbcTemplate jdbc, WmBarcodeService wmBarcodeService) {
        this.workOrderRepository = workOrderRepository;
        this.jdbc = jdbc;
        this.wmBarcodeService = wmBarcodeService;
    }

    public List<Map<String, Object>> getData(String dsName, String datasetName, Map<String, Object> parameters) {
        long id = resolveWorkOrderId(parameters);
        return workOrderRepository.findById(id)
                .map(wo -> List.<Map<String, Object>>of(toWorkOrderMap(wo)))
                .orElseGet(List::of);
    }

    public List<Map<String, Object>> getChildData(String dsName, String datasetName, Map<String, Object> parameters) {
        long parentId = parseId(parameters);
        return jdbc.query("""
                SELECT wo.work_order_id, wo.work_order_no, wo.order_id, co.order_no,
                       wo.product_id, p.product_name, wo.plan_qty, wo.completed_qty, wo.status
                FROM work_order wo
                LEFT JOIN product p ON wo.product_id = p.product_id
                LEFT JOIN customer_order co ON wo.order_id = co.order_id
                WHERE wo.parent_work_order_id = ?
                ORDER BY wo.work_order_id
                """, (rs, n) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("workorderId", rs.getLong("work_order_id"));
            m.put("workorderCode", rs.getString("work_order_no"));
            m.put("orderId", rs.getObject("order_id"));
            m.put("orderCode", rs.getString("order_no"));
            m.put("productId", rs.getLong("product_id"));
            m.put("productName", rs.getString("product_name"));
            m.put("quantity", rs.getLong("plan_qty"));
            m.put("quantityProduced", rs.getLong("completed_qty"));
            String status = rs.getString("status");
            m.put("status", status);
            putChineseWorkOrderFields(m, rs.getString("work_order_no"), rs.getString("order_no"),
                    rs.getString("product_name"), rs.getLong("plan_qty"), rs.getLong("completed_qty"), status);
            return m;
        }, parentId);
    }

    public List<Map<String, Object>> getQc(String dsName, String datasetName, Map<String, Object> parameters) {
        long id = parseId(parameters);
        Map<String, Object> barcode = wmBarcodeService.getById(id);
        if (barcode == null || barcode.isEmpty()) {
            return List.of();
        }
        return List.of(barcode);
    }

    private static long parseId(Map<String, Object> parameters) {
        if (parameters == null) {
            return 0L;
        }
        Object raw = parameters.get("id");
        if (raw == null) {
            return 0L;
        }
        String text = String.valueOf(raw);
        if (!StringUtils.hasText(text)) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private long resolveWorkOrderId(Map<String, Object> parameters) {
        long id = parseId(parameters);
        if (id > 0 || parameters == null) {
            return id;
        }
        Object rawCode = parameters.get("code");
        if (rawCode == null) {
            rawCode = parameters.get("workorderCode");
        }
        String code = rawCode == null ? "" : String.valueOf(rawCode).trim();
        if (!StringUtils.hasText(code)) {
            return 0L;
        }
        try {
            Long resolved = jdbc.queryForObject(
                    "SELECT work_order_id FROM work_order WHERE work_order_no = ?",
                    Long.class,
                    code);
            return resolved == null ? 0L : resolved;
        } catch (EmptyResultDataAccessException ignored) {
            return 0L;
        }
    }

    private static Map<String, Object> toWorkOrderMap(WorkOrderVO wo) {
        Map<String, Object> m = new HashMap<>();
        m.put("workorderId", wo.workOrderId());
        m.put("workorderCode", wo.workOrderNo());
        m.put("orderId", wo.orderId());
        m.put("orderCode", wo.orderNo());
        m.put("productId", wo.productId());
        m.put("productName", wo.productName());
        m.put("quantity", wo.planQty());
        m.put("quantityProduced", wo.completedQty());
        m.put("status", wo.status());
        putChineseWorkOrderFields(m, wo.workOrderNo(), wo.orderNo(), wo.productName(),
                wo.planQty(), wo.completedQty(), wo.status());
        return m;
    }

    private static void putChineseWorkOrderFields(Map<String, Object> m, String workOrderNo, String orderNo,
            String productName, long planQty, long completedQty, String status) {
        m.put("工单号", workOrderNo);
        m.put("订单号", orderNo);
        m.put("产品名称", productName);
        m.put("计划数量", planQty);
        m.put("完成数量", completedQty);
        m.put("状态", translateWorkOrderStatus(status));
    }

    private static String translateWorkOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        return switch (status) {
            case "PREPARE" -> "待生产";
            case "CONFIRMED" -> "已确认";
            case "RELEASED" -> "已下达";
            case "IN_PROGRESS" -> "生产中";
            case "FINISHED" -> "已完成";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }
}
