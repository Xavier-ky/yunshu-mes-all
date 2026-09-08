package com.yunshu.mes.agent.service;

import com.yunshu.mes.inventory.compat.service.WmIssueService;
import com.yunshu.mes.planning.compat.repository.WorkOrderBomRepository;
import com.yunshu.mes.planning.workflow.WorkOrderLifecycleService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controlled write boundary for the production-material issue Agent.
 *
 * <p>The service deliberately invokes the same WMS document services used by
 * Outbound Operations. The model never supplies SQL, warehouse IDs, batch IDs
 * or quantities: all pick details are deterministically allocated from the
 * real on-hand stock inside this transaction.</p>
 */
@Service
public class AgentProductionIssueService {

    private final JdbcTemplate jdbc;
    private final WmIssueService issueService;
    private final WorkOrderBomRepository bomRepository;
    private final WorkOrderLifecycleService lifecycle;

    public AgentProductionIssueService(
            JdbcTemplate jdbc,
            WmIssueService issueService,
            WorkOrderBomRepository bomRepository,
            WorkOrderLifecycleService lifecycle) {
        this.jdbc = jdbc;
        this.issueService = issueService;
        this.bomRepository = bomRepository;
        this.lifecycle = lifecycle;
    }

    /** Execute scheduled-work-order -> issue document -> picking -> outbound -> reconciliation. */
    @Transactional
    public Map<String, Object> execute(Long workOrderId) {
        Map<String, Object> workOrder = loadWorkOrder(workOrderId);
        String lifecycleStatus = string(workOrder.get("lifecycle_status"));
        if (WorkOrderLifecycleService.MATERIAL_ISSUED.equals(lifecycleStatus)) {
            return alreadyIssuedResult(workOrderId, workOrder);
        }
        ensureEligible(workOrder);
        ensureDispatches(workOrderId);
        ensureBomSnapshot(workOrderId, workOrder);

        Long issueId = ensureIssueDocument(workOrderId);
        List<Map<String, Object>> details = allocatePicking(issueId);
        if (!issueService.checkQuantity(issueId)) {
            throw new IllegalStateException("Production Issue Agent quantity validation failed");
        }

        jdbc.update("UPDATE wm_issue_header SET status = 'APPROVED', update_time = NOW() WHERE issue_id = ? AND status = 'PREPARE'", issueId);
        issueService.execute(issueId);
        return reconcile(workOrderId, issueId, details);
    }

    private Map<String, Object> loadWorkOrder(Long workOrderId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT work_order_id, work_order_no, product_id, bom_id, plan_qty, status, lifecycle_status
                FROM work_order WHERE work_order_id = ? AND is_deleted = 0 FOR UPDATE
                """, workOrderId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Work order not found");
        }
        return rows.get(0);
    }

    private static void ensureEligible(Map<String, Object> workOrder) {
        String status = string(workOrder.get("status"));
        String lifecycleStatus = string(workOrder.get("lifecycle_status"));
        if (!"CONFIRMED".equals(status) && !"DISPATCHED".equals(status)) {
            throw new IllegalStateException("Only a confirmed or dispatched work order may enter production issue");
        }
        if (!WorkOrderLifecycleService.SCHEDULED.equals(lifecycleStatus)) {
            throw new IllegalStateException("Production Issue Agent requires lifecycle SCHEDULED, current=" + lifecycleStatus);
        }
    }

    private void ensureDispatches(Long workOrderId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dispatch_task WHERE work_order_id = ?", Long.class, workOrderId);
        if (count == null || count <= 0) {
            throw new IllegalStateException("No dispatch tasks were generated for this work order");
        }
    }

    /**
     * The regular planning API does not materialize this compat snapshot. Make
     * it once here, from the released BOM selected for this work order, so the
     * WMS document has a frozen execution basis instead of a live BOM lookup.
     */
    private void ensureBomSnapshot(Long workOrderId, Map<String, Object> workOrder) {
        if (!bomRepository.findByWorkOrderId(workOrderId).isEmpty()) {
            return;
        }
        Long productId = number(workOrder.get("product_id"));
        Long bomId = number(workOrder.get("bom_id"));
        List<Map<String, Object>> boms = bomId == null
                ? jdbc.queryForList("""
                    SELECT bom_id FROM bom
                    WHERE product_id = ? AND status IN ('RELEASED', 'ENABLED')
                    ORDER BY bom_id DESC LIMIT 1
                    """, productId)
                : jdbc.queryForList("""
                    SELECT bom_id FROM bom
                    WHERE bom_id = ? AND status IN ('RELEASED', 'ENABLED')
                    """, bomId);
        if (boms.isEmpty()) {
            throw new IllegalStateException("No released BOM is available to create the work-order snapshot");
        }
        Long resolvedBomId = number(boms.get(0).get("bom_id"));
        BigDecimal planQty = decimal(workOrder.get("plan_qty"));
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT bi.material_id, bi.qty_per, COALESCE(bi.loss_rate, 0) AS loss_rate,
                       m.material_code, m.material_name, m.material_type,
                       COALESCE(u.unit_code, 'PCS') AS unit_code
                FROM bom_item bi
                JOIN material m ON m.material_id = bi.material_id
                LEFT JOIN uom u ON u.unit_id = m.unit_id
                WHERE bi.bom_id = ?
                ORDER BY bi.bom_item_id
                """, resolvedBomId);
        if (items.isEmpty()) {
            throw new IllegalStateException("The released BOM has no material lines");
        }
        for (Map<String, Object> item : items) {
            BigDecimal quantity = planQty.multiply(decimal(item.get("qty_per")))
                    .multiply(BigDecimal.ONE.add(decimal(item.get("loss_rate"))));
            bomRepository.insert(
                    workOrderId,
                    number(item.get("material_id")),
                    string(item.get("material_code")),
                    string(item.get("material_name")),
                    null,
                    string(item.get("unit_code")),
                    "FINISHED".equals(string(item.get("material_type"))) ? "PRODUCT" : "ITEM",
                    quantity,
                    "Frozen by ProductionIssueAgent from BOM " + resolvedBomId);
        }
        jdbc.update("UPDATE work_order SET bom_id = COALESCE(bom_id, ?) WHERE work_order_id = ?", resolvedBomId, workOrderId);
    }

    private Long ensureIssueDocument(Long workOrderId) {
        List<Map<String, Object>> headers = jdbc.queryForList("""
                SELECT issue_id, status, remark FROM wm_issue_header
                WHERE workorder_id = ? ORDER BY issue_id DESC LIMIT 1 FOR UPDATE
                """, workOrderId);
        if (!headers.isEmpty()) {
            Map<String, Object> header = headers.get(0);
            Long issueId = number(header.get("issue_id"));
            String status = string(header.get("status"));
            if ("FINISHED".equals(status)) {
                return issueId;
            }
            if (!"PREPARE".equals(status)) {
                throw new IllegalStateException("A manually handled issue document is already in progress: " + issueId);
            }
            Long detailCount = jdbc.queryForObject("SELECT COUNT(*) FROM wm_issue_detail WHERE issue_id = ?", Long.class, issueId);
            if (detailCount != null && detailCount > 0) {
                throw new IllegalStateException("Existing issue document already has pick details; it will not be overwritten by the Agent");
            }
        }
        Long issueId = issueService.createFromWorkOrder(workOrderId);
        Long lineCount = jdbc.queryForObject("SELECT COUNT(*) FROM wm_issue_line WHERE issue_id = ?", Long.class, issueId);
        if (lineCount == null || lineCount <= 0) {
            throw new IllegalStateException("Issue document has no BOM lines; outbound was not executed");
        }
        jdbc.update("UPDATE wm_issue_header SET remark = 'AGENT_PRODUCTION_ISSUE', update_time = NOW() WHERE issue_id = ?", issueId);
        return issueId;
    }

    /** Allocate FIFO/FEFO physical stock, splitting a line across stocks when needed. */
    private List<Map<String, Object>> allocatePicking(Long issueId) {
        List<Map<String, Object>> lines = jdbc.queryForList("""
                SELECT line_id, item_id, item_code, item_name, specification,
                       unit_of_measure, unit_name, quantity_issued
                FROM wm_issue_line WHERE issue_id = ? ORDER BY line_id FOR UPDATE
                """, issueId);
        List<Map<String, Object>> created = new ArrayList<>();
        for (Map<String, Object> line : lines) {
            Long lineId = number(line.get("line_id"));
            Long itemId = number(line.get("item_id"));
            BigDecimal required = decimal(line.get("quantity_issued"));
            if (required.signum() <= 0) {
                throw new IllegalStateException("Issue line quantity must be positive");
            }
            List<Map<String, Object>> stocks = jdbc.queryForList("""
                    SELECT material_stock_id, item_id, item_code, item_name, specification,
                           unit_of_measure, unit_name, batch_id, batch_code,
                           warehouse_id, warehouse_code, warehouse_name,
                           location_id, location_code, location_name,
                           area_id, area_code, area_name, quantity_onhand
                    FROM wm_material_stock
                    WHERE item_id = ? AND frozen_flag = 'N' AND quantity_onhand > 0
                    ORDER BY CASE WHEN expire_date IS NULL THEN 1 ELSE 0 END,
                             expire_date, recpt_date, material_stock_id
                    FOR UPDATE
                    """, itemId);
            BigDecimal remaining = required;
            for (Map<String, Object> stock : stocks) {
                BigDecimal picked = decimal(stock.get("quantity_onhand")).min(remaining);
                if (picked.signum() <= 0) continue;
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("issueId", issueId);
                detail.put("lineId", lineId);
                detail.put("materialStockId", number(stock.get("material_stock_id")));
                detail.put("itemId", itemId);
                detail.put("itemCode", string(stock.get("item_code")));
                detail.put("itemName", string(stock.get("item_name")));
                detail.put("specification", string(stock.get("specification")));
                detail.put("unitOfMeasure", string(stock.get("unit_of_measure")));
                detail.put("unitName", string(stock.get("unit_name")));
                detail.put("quantity", picked);
                detail.put("batchId", number(stock.get("batch_id")));
                detail.put("batchCode", string(stock.get("batch_code")));
                detail.put("warehouseId", number(stock.get("warehouse_id")));
                detail.put("warehouseCode", string(stock.get("warehouse_code")));
                detail.put("warehouseName", string(stock.get("warehouse_name")));
                detail.put("locationId", number(stock.get("location_id")));
                detail.put("locationCode", string(stock.get("location_code")));
                detail.put("locationName", string(stock.get("location_name")));
                detail.put("areaId", number(stock.get("area_id")));
                detail.put("areaCode", string(stock.get("area_code")));
                detail.put("areaName", string(stock.get("area_name")));
                detail.put("remark", "Allocated by ProductionIssueAgent");
                Long detailId = issueService.createDetail(detail);
                detail.put("detailId", detailId);
                created.add(detail);
                remaining = remaining.subtract(picked);
                if (remaining.signum() == 0) break;
            }
            if (remaining.signum() > 0) {
                throw new IllegalStateException("Insufficient physical stock for " + string(line.get("item_code"))
                        + "; shortage=" + remaining.stripTrailingZeros().toPlainString());
            }
        }
        return created;
    }

    private Map<String, Object> reconcile(Long workOrderId, Long issueId, List<Map<String, Object>> details) {
        Map<String, Object> header = jdbc.queryForMap(
                "SELECT issue_code, status FROM wm_issue_header WHERE issue_id = ?", issueId);
        if (!"FINISHED".equals(header.get("status"))) {
            throw new IllegalStateException("Issue document was not finished");
        }
        Long transactionCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM wm_transaction
                WHERE source_doc_type = 'IS' AND source_doc_id = ? AND transaction_flag = -1
                """, Long.class, issueId);
        Long consumeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wm_item_consume WHERE attr1 = ? AND status = 'FINISHED'",
                Long.class, String.valueOf(issueId));
        Long consumeDetailCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM wm_item_consume_detail d
                JOIN wm_item_consume_line l ON l.line_id = d.line_id
                JOIN wm_item_consume c ON c.record_id = l.record_id
                WHERE c.attr1 = ? AND c.status = 'FINISHED'
                """, Long.class, String.valueOf(issueId));
        String finalLifecycle = lifecycle.getLifecycle(workOrderId);
        if (transactionCount == null || transactionCount != details.size()
                || consumeCount == null || consumeCount != 1
                || consumeDetailCount == null || consumeDetailCount != details.size()
                || !WorkOrderLifecycleService.MATERIAL_ISSUED.equals(finalLifecycle)) {
            throw new IllegalStateException("Production issue reconciliation did not complete");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workOrderId", workOrderId);
        result.put("issueId", issueId);
        result.put("issueCode", header.get("issue_code"));
        result.put("issueStatus", header.get("status"));
        result.put("pickDetailCount", details.size());
        result.put("stockTransactionCount", transactionCount);
        result.put("consumeRecordCount", consumeCount);
        result.put("consumeDetailCount", consumeDetailCount);
        result.put("lifecycleStatus", finalLifecycle);
        result.put("readyForShopFloor", true);
        result.put("message", "Material issue completed; the work order is ready for shop-floor execution");
        return result;
    }

    private Map<String, Object> alreadyIssuedResult(Long workOrderId, Map<String, Object> workOrder) {
        List<Map<String, Object>> issues = jdbc.queryForList("""
                SELECT issue_id, issue_code, status FROM wm_issue_header
                WHERE workorder_id = ? AND status = 'FINISHED'
                ORDER BY issue_id DESC LIMIT 1
                """, workOrderId);
        if (issues.isEmpty()) {
            throw new IllegalStateException("Work order is MATERIAL_ISSUED but no finished issue document is traceable");
        }
        Map<String, Object> issue = issues.get(0);
        return Map.of(
                "workOrderId", workOrderId,
                "workOrderNo", workOrder.get("work_order_no"),
                "issueId", number(issue.get("issue_id")),
                "issueCode", issue.get("issue_code"),
                "issueStatus", issue.get("status"),
                "lifecycleStatus", WorkOrderLifecycleService.MATERIAL_ISSUED,
                "readyForShopFloor", true,
                "alreadyIssued", true,
                "message", "Material issue was already completed");
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(String.valueOf(number));
        return new BigDecimal(String.valueOf(value));
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
