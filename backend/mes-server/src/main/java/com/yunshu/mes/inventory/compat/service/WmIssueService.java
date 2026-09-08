package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.inventory.compat.WmDocSchemas;
import com.yunshu.mes.inventory.workflow.InventoryBatchBridgeService;
import com.yunshu.mes.inventory.workflow.IssueConsumeSyncService;
import com.yunshu.mes.planning.compat.repository.CompatWorkorderRepository;
import com.yunshu.mes.planning.compat.repository.WorkOrderBomRepository;
import com.yunshu.mes.planning.workflow.WorkOrderLifecycleService;
import com.yunshu.mes.system.compat.service.AutocodeGenService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmIssueService {

    private final JdbcTemplate jdbc;
    private final StorageCoreService storageCore;
    private final WorkOrderLifecycleService lifecycle;
    private final IssueConsumeSyncService consumeSync;
    private final InventoryBatchBridgeService batchBridge;
    private final CompatWorkorderRepository workorderRepo;
    private final WorkOrderBomRepository bomRepo;
    private final AutocodeGenService autocodeGenService;

    public WmIssueService(
            JdbcTemplate jdbc,
            StorageCoreService storageCore,
            WorkOrderLifecycleService lifecycle,
            IssueConsumeSyncService consumeSync,
            InventoryBatchBridgeService batchBridge,
            CompatWorkorderRepository workorderRepo,
            WorkOrderBomRepository bomRepo,
            AutocodeGenService autocodeGenService) {
        this.jdbc = jdbc;
        this.storageCore = storageCore;
        this.lifecycle = lifecycle;
        this.consumeSync = consumeSync;
        this.batchBridge = batchBridge;
        this.workorderRepo = workorderRepo;
        this.bomRepo = bomRepo;
        this.autocodeGenService = autocodeGenService;
    }

    public List<Map<String, Object>> listHeader(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_issue_header", "issue_id", WmDocSchemas.ISSUE_HEADER,
                WmDocSchemas.filter(params, "issueCode", "issueName", "workorderCode", "status"),
                PageUtil.offset(pn, ps), ps);
    }

    public long countHeader(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_issue_header", WmDocSchemas.ISSUE_HEADER,
                WmDocSchemas.filter(params, "issueCode", "issueName", "workorderCode", "status"));
    }

    public Map<String, Object> getHeader(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_issue_header", "issue_id", "issueId", WmDocSchemas.ISSUE_HEADER, id);
    }

    @Transactional
    public Long createHeader(Map<String, Object> body) {
        Object woId = body.get("workorderId");
        if (woId == null && body.get("workorderCode") != null) {
            woId = lifecycle.resolveWorkOrderIdFromCode(String.valueOf(body.get("workorderCode")));
            if (woId != null) {
                body.put("workorderId", woId);
            }
        }
        if (woId instanceof Number n) {
            lifecycle.validateIssueAllowed(n.longValue());
        }
        return WmSqlHelper.insert(jdbc, "wm_issue_header", WmDocSchemas.ISSUE_HEADER, body);
    }

    @Transactional
    public Long createFromWorkOrder(Long workOrderId) {
        lifecycle.validateIssueAllowed(workOrderId);
        Map<String, Object> wo = workorderRepo.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("生产工单不存在"));
        String woCode = String.valueOf(wo.get("workorderCode"));
        Long existing = jdbc.query("""
                SELECT issue_id FROM wm_issue_header
                WHERE workorder_id = ? AND status NOT IN ('FINISHED', 'CANCELED')
                ORDER BY issue_id DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong("issue_id") : null, workOrderId);
        if (existing != null) {
            long lineCount = WmSqlHelper.count(jdbc, "wm_issue_line", WmDocSchemas.ISSUE_LINE,
                    Map.of("issueId", String.valueOf(existing)));
            if (lineCount > 0) {
                return existing;
            }
            appendBomLines(existing, workOrderId);
            return existing;
        }

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("issueCode", genIssueCode());
        header.put("issueName", "生产领料-" + woCode);
        header.put("workorderId", workOrderId);
        header.put("workorderCode", woCode);
        header.put("clientId", wo.get("clientId"));
        header.put("clientCode", wo.get("clientCode"));
        header.put("clientName", wo.get("clientName"));
        header.put("requiredTime", wo.get("requestDate") != null
                ? wo.get("requestDate")
                : LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        header.put("issueDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        header.put("status", "PREPARE");
        Long issueId = WmSqlHelper.insert(jdbc, "wm_issue_header", WmDocSchemas.ISSUE_HEADER, header);

        appendBomLines(issueId, workOrderId);
        return issueId;
    }

    private void appendBomLines(Long issueId, Long workOrderId) {
        for (Map<String, Object> bom : bomRepo.findByWorkOrderId(workOrderId)) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("issueId", issueId);
            line.put("itemId", bom.get("itemId"));
            line.put("itemCode", bom.get("itemCode"));
            line.put("itemName", bom.get("itemName"));
            line.put("specification", bom.get("itemSpc"));
            line.put("unitOfMeasure", bom.get("unitOfMeasure"));
            line.put("unitName", bom.get("unitName"));
            line.put("quantityIssued", bom.get("quantity"));
            createLine(line);
        }
    }

    private String genIssueCode() {
        try {
            return autocodeGenService.genSerialCode("ISSUE_CODE", null);
        } catch (RuntimeException ex) {
            return "IS" + System.currentTimeMillis() % 100000;
        }
    }

    @Transactional
    public int updateHeader(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("issueId")));
        return WmSqlHelper.update(jdbc, "wm_issue_header", "issue_id", WmDocSchemas.ISSUE_HEADER, body, id);
    }

    @Transactional
    public int deleteHeader(Long id) {
        Map<String, Object> h = getHeader(id);
        if (h != null && !"PREPARE".equals(h.get("status"))) {
            throw new IllegalArgumentException("只能删除草稿状态的单据!");
        }
        jdbc.update("DELETE FROM wm_issue_detail WHERE issue_id = ?", id);
        jdbc.update("DELETE FROM wm_issue_line WHERE issue_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_issue_header", "issue_id", id);
    }

    public boolean checkQuantity(Long issueId) {
        List<Map<String, Object>> lines = listLine(Map.of("issueId", String.valueOf(issueId), "pageNum", "1", "pageSize", "1000"));
        for (Map<String, Object> line : lines) {
            double issued = toDouble(line.get("quantityIssued"));
            Long lineId = Long.parseLong(String.valueOf(line.get("lineId")));
            Double picked = jdbc.queryForObject(
                    "SELECT IFNULL(SUM(quantity),0) FROM wm_issue_detail WHERE line_id = ?", Double.class, lineId);
            if (Math.abs(issued - (picked == null ? 0 : picked)) > 0.001) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public void execute(Long issueId) {
        Map<String, Object> h = getHeader(issueId);
        if (h == null) {
            throw new IllegalArgumentException("领料单不存在");
        }
        if (!"APPROVED".equals(h.get("status"))) {
            throw new IllegalArgumentException("只有待执行状态的单据才能执行出库");
        }
        Object woId = h.get("workorderId");
        if (woId == null) {
            woId = lifecycle.resolveWorkOrderIdFromCode(String.valueOf(h.get("workorderCode")));
        }
        if (woId instanceof Number n) {
            lifecycle.validateIssueAllowed(n.longValue());
        }
        if (!checkQuantity(issueId)) {
            throw new IllegalArgumentException("拣货数量与领料数量不一致");
        }
        storageCore.processIssue(issueId);
        batchBridge.syncOutboundFromIssue(issueId);
        jdbc.update("UPDATE wm_issue_header SET status = 'FINISHED', update_time = NOW() WHERE issue_id = ?", issueId);
        consumeSync.syncFromIssue(issueId, h);
        if (woId instanceof Number n) {
            lifecycle.advanceIfNullOrEarlier(n.longValue(), WorkOrderLifecycleService.MATERIAL_ISSUED);
        }
    }

    public List<Map<String, Object>> listLine(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_issue_line", "line_id", WmDocSchemas.ISSUE_LINE,
                WmDocSchemas.filter(params, "issueId"), PageUtil.offset(pn, ps), ps);
    }

    public long countLine(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_issue_line", WmDocSchemas.ISSUE_LINE,
                WmDocSchemas.filter(params, "issueId"));
    }

    public List<Map<String, Object>> listLineWithDetail(Map<String, String> params) {
        List<Map<String, Object>> lines = listLine(params);
        if (lines.isEmpty()) {
            return lines;
        }
        Long issueId = Long.parseLong(String.valueOf(lines.get(0).get("issueId")));
        List<Map<String, Object>> details = listDetail(Map.of("issueId", String.valueOf(issueId), "pageNum", "1", "pageSize", "1000"));
        var byLine = details.stream().collect(Collectors.groupingBy(d -> Long.parseLong(String.valueOf(d.get("lineId")))));
        for (Map<String, Object> line : lines) {
            Long lineId = Long.parseLong(String.valueOf(line.get("lineId")));
            line.put("details", byLine.getOrDefault(lineId, List.of()));
        }
        return lines;
    }

    public Map<String, Object> getLine(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_issue_line", "line_id", "lineId", WmDocSchemas.ISSUE_LINE, id);
    }

    @Transactional
    public Long createLine(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_issue_line", WmDocSchemas.ISSUE_LINE, body);
    }

    @Transactional
    public int updateLine(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("lineId")));
        return WmSqlHelper.update(jdbc, "wm_issue_line", "line_id", WmDocSchemas.ISSUE_LINE, body, id);
    }

    @Transactional
    public int deleteLine(Long id) {
        jdbc.update("DELETE FROM wm_issue_detail WHERE line_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_issue_line", "line_id", id);
    }

    public List<Map<String, Object>> listDetail(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_issue_detail", "detail_id", WmDocSchemas.ISSUE_DETAIL,
                WmDocSchemas.filter(params, "issueId", "lineId"), PageUtil.offset(pn, ps), ps);
    }

    public long countDetail(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_issue_detail", WmDocSchemas.ISSUE_DETAIL,
                WmDocSchemas.filter(params, "issueId", "lineId"));
    }

    public Map<String, Object> getDetail(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_issue_detail", "detail_id", "detailId", WmDocSchemas.ISSUE_DETAIL, id);
    }

    @Transactional
    public Long createDetail(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_issue_detail", WmDocSchemas.ISSUE_DETAIL, body);
    }

    @Transactional
    public int updateDetail(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("detailId")));
        return WmSqlHelper.update(jdbc, "wm_issue_detail", "detail_id", WmDocSchemas.ISSUE_DETAIL, body, id);
    }

    @Transactional
    public int deleteDetail(Long id) {
        return WmSqlHelper.delete(jdbc, "wm_issue_detail", "detail_id", id);
    }

    private static double toDouble(Object v) {
        return v == null ? 0 : Double.parseDouble(String.valueOf(v));
    }
}
