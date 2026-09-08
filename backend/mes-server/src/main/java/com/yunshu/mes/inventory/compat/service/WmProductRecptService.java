package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.inventory.compat.WmDocSchemas;
import com.yunshu.mes.planning.compat.repository.CompatWorkorderRepository;
import com.yunshu.mes.planning.workflow.WorkOrderLifecycleService;
import com.yunshu.mes.system.compat.service.AutocodeGenService;
import com.yunshu.mes.traceability.service.WorkOrderTraceSyncService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmProductRecptService {

    private final JdbcTemplate jdbc;
    private final StorageCoreService storageCore;
    private final WorkOrderLifecycleService lifecycle;
    private final CompatWorkorderRepository workorderRepo;
    private final AutocodeGenService autocodeGenService;
    private final WorkOrderTraceSyncService traceSync;

    public WmProductRecptService(
            JdbcTemplate jdbc,
            StorageCoreService storageCore,
            WorkOrderLifecycleService lifecycle,
            CompatWorkorderRepository workorderRepo,
            AutocodeGenService autocodeGenService,
            WorkOrderTraceSyncService traceSync) {
        this.jdbc = jdbc;
        this.storageCore = storageCore;
        this.lifecycle = lifecycle;
        this.workorderRepo = workorderRepo;
        this.autocodeGenService = autocodeGenService;
        this.traceSync = traceSync;
    }

    public List<Map<String, Object>> listHeader(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_product_recpt", "recpt_id", WmDocSchemas.PRODUCT_RECPT,
                WmDocSchemas.filter(params, "recptCode", "recptName", "workorderCode", "itemCode", "status"),
                PageUtil.offset(pn, ps), ps);
    }

    public long countHeader(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_product_recpt", WmDocSchemas.PRODUCT_RECPT,
                WmDocSchemas.filter(params, "recptCode", "recptName", "workorderCode", "itemCode", "status"));
    }

    public Map<String, Object> getHeader(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_product_recpt", "recpt_id", "recptId", WmDocSchemas.PRODUCT_RECPT, id);
    }

    @Transactional
    public Long createHeader(Map<String, Object> body) {
        enrichHeaderFromWorkOrder(body);
        if (body.get("status") == null) {
            body.put("status", "PREPARE");
        }
        return WmSqlHelper.insert(jdbc, "wm_product_recpt", WmDocSchemas.PRODUCT_RECPT, body);
    }

    @Transactional
    public Long createFromWorkOrder(Long workOrderId) {
        lifecycle.validateProductRecpt(workOrderId);
        Map<String, Object> wo = workorderRepo.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("生产工单不存在"));
        String woCode = String.valueOf(wo.get("workorderCode"));

        Long existing = jdbc.query("""
                SELECT recpt_id FROM wm_product_recpt
                WHERE workorder_id = ? AND status NOT IN ('FINISHED', 'CANCELED')
                ORDER BY recpt_id DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong("recpt_id") : null, workOrderId);
        if (existing != null) {
            long lineCount = WmSqlHelper.count(jdbc, "wm_product_recpt_line", WmDocSchemas.PRODUCT_RECPT_LINE,
                    Map.of("recptId", String.valueOf(existing)));
            if (lineCount > 0) {
                ensureShelfDetails(existing);
                return existing;
            }
            appendProductLine(existing, wo);
            ensureShelfDetails(existing);
            return existing;
        }

        Map<String, Object> item = resolveProductItem(wo);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("recptCode", genRecptCode());
        header.put("recptName", "成品入库-" + woCode);
        header.put("workorderId", workOrderId);
        header.put("workorderCode", woCode);
        header.put("workorderName", wo.get("workorderName"));
        header.put("itemId", item.get("itemId"));
        header.put("itemCode", item.get("itemCode"));
        header.put("itemName", item.get("itemName"));
        header.put("specification", item.get("specification"));
        header.put("unitOfMeasure", item.get("unitOfMeasure"));
        header.put("unitName", item.get("unitName"));
        header.put("recptDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        header.put("status", "PREPARE");
        Long recptId = WmSqlHelper.insert(jdbc, "wm_product_recpt", WmDocSchemas.PRODUCT_RECPT, header);

        appendProductLine(recptId, wo);
        ensureShelfDetails(recptId);
        return recptId;
    }

    @Transactional
    public int updateHeader(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("recptId")));
        if ("APPROVING".equals(body.get("status"))) {
            ensureShelfDetails(id);
        }
        return WmSqlHelper.update(jdbc, "wm_product_recpt", "recpt_id", WmDocSchemas.PRODUCT_RECPT, body, id);
    }

    @Transactional
    public int deleteHeader(Long id) {
        Map<String, Object> h = getHeader(id);
        if (h != null && !"PREPARE".equals(h.get("status"))) {
            throw new IllegalArgumentException("只能删除草稿状态的单据!");
        }
        jdbc.update("DELETE FROM wm_product_recpt_detail WHERE recpt_id = ?", id);
        jdbc.update("DELETE FROM wm_product_recpt_line WHERE recpt_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_product_recpt", "recpt_id", id);
    }

    public boolean checkQuantity(Long recptId) {
        List<Map<String, Object>> lines = listLine(Map.of("recptId", String.valueOf(recptId), "pageNum", "1", "pageSize", "1000"));
        for (Map<String, Object> line : lines) {
            double received = toDouble(line.get("quantityRecived"));
            Long lineId = Long.parseLong(String.valueOf(line.get("lineId")));
            Double picked = jdbc.queryForObject(
                    "SELECT IFNULL(SUM(quantity),0) FROM wm_product_recpt_detail WHERE line_id = ?", Double.class, lineId);
            if (Math.abs(received - (picked == null ? 0 : picked)) > 0.001) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public void execute(Long recptId) {
        Map<String, Object> h = getHeader(recptId);
        if (h == null) {
            throw new IllegalArgumentException("产品入库单不存在");
        }
        Long lines = jdbc.queryForObject("SELECT COUNT(*) FROM wm_product_recpt_line WHERE recpt_id = ?", Long.class, recptId);
        if (lines == null || lines == 0) {
            throw new IllegalArgumentException("请添加产品入库单行");
        }
        Long details = jdbc.queryForObject("SELECT COUNT(*) FROM wm_product_recpt_detail WHERE recpt_id = ?", Long.class, recptId);
        if (details == null || details == 0) {
            ensureShelfDetails(recptId);
            details = jdbc.queryForObject("SELECT COUNT(*) FROM wm_product_recpt_detail WHERE recpt_id = ?", Long.class, recptId);
            if (details == null || details == 0) {
                throw new IllegalArgumentException("请添加入库明细（上架到成品库位）");
            }
        }
        if (!checkQuantity(recptId)) {
            throw new IllegalArgumentException("上架数量与入库数量不一致");
        }
        Object woId = h.get("workorderId");
        if (woId == null) {
            woId = lifecycle.resolveWorkOrderIdFromCode(String.valueOf(h.get("workorderCode")));
        }
        if (woId instanceof Number n) {
            lifecycle.validateProductRecpt(n.longValue());
        }
        storageCore.processProductRecpt(recptId);
        jdbc.update("UPDATE wm_product_recpt SET status = 'FINISHED', update_time = NOW() WHERE recpt_id = ?", recptId);
        if (woId instanceof Number n) {
            lifecycle.advance(n.longValue(), WorkOrderLifecycleService.COMPLETED);
            traceSync.syncFromWorkOrder(n.longValue());
        }
    }

    public List<Map<String, Object>> listLine(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_product_recpt_line", "line_id", WmDocSchemas.PRODUCT_RECPT_LINE,
                WmDocSchemas.filter(params, "recptId"), PageUtil.offset(pn, ps), ps);
    }

    public long countLine(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_product_recpt_line", WmDocSchemas.PRODUCT_RECPT_LINE,
                WmDocSchemas.filter(params, "recptId"));
    }

    public List<Map<String, Object>> listLineWithDetail(Map<String, String> params) {
        List<Map<String, Object>> lines = listLine(params);
        if (lines.isEmpty()) {
            return lines;
        }
        Long recptId = Long.parseLong(String.valueOf(lines.get(0).get("recptId")));
        List<Map<String, Object>> details = listDetail(Map.of("recptId", String.valueOf(recptId), "pageNum", "1", "pageSize", "1000"));
        var byLine = details.stream().collect(Collectors.groupingBy(d -> Long.parseLong(String.valueOf(d.get("lineId")))));
        for (Map<String, Object> line : lines) {
            Long lineId = Long.parseLong(String.valueOf(line.get("lineId")));
            line.put("details", byLine.getOrDefault(lineId, List.of()));
        }
        return lines;
    }

    public Map<String, Object> getLine(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_product_recpt_line", "line_id", "lineId", WmDocSchemas.PRODUCT_RECPT_LINE, id);
    }

    @Transactional
    public Long createLine(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_product_recpt_line", WmDocSchemas.PRODUCT_RECPT_LINE, body);
    }

    @Transactional
    public int updateLine(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("lineId")));
        return WmSqlHelper.update(jdbc, "wm_product_recpt_line", "line_id", WmDocSchemas.PRODUCT_RECPT_LINE, body, id);
    }

    @Transactional
    public int deleteLine(Long id) {
        jdbc.update("DELETE FROM wm_product_recpt_detail WHERE line_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_product_recpt_line", "line_id", id);
    }

    public List<Map<String, Object>> listDetail(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_product_recpt_detail", "detail_id", WmDocSchemas.PRODUCT_RECPT_DETAIL,
                WmDocSchemas.filter(params, "recptId", "lineId"), PageUtil.offset(pn, ps), ps);
    }

    public long countDetail(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_product_recpt_detail", WmDocSchemas.PRODUCT_RECPT_DETAIL,
                WmDocSchemas.filter(params, "recptId", "lineId"));
    }

    public Map<String, Object> getDetail(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_product_recpt_detail", "detail_id", "detailId", WmDocSchemas.PRODUCT_RECPT_DETAIL, id);
    }

    @Transactional
    public Long createDetail(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_product_recpt_detail", WmDocSchemas.PRODUCT_RECPT_DETAIL, body);
    }

    @Transactional
    public int updateDetail(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("detailId")));
        return WmSqlHelper.update(jdbc, "wm_product_recpt_detail", "detail_id", WmDocSchemas.PRODUCT_RECPT_DETAIL, body, id);
    }

    @Transactional
    public int deleteDetail(Long id) {
        return WmSqlHelper.delete(jdbc, "wm_product_recpt_detail", "detail_id", id);
    }

    @Transactional
    public void ensureShelfDetails(Long recptId) {
        List<Map<String, Object>> lines = listLine(Map.of("recptId", String.valueOf(recptId), "pageNum", "1", "pageSize", "1000"));
        if (lines.isEmpty()) {
            return;
        }
        Map<String, Object> wh = defaultFgWarehouse()
                .orElseThrow(() -> new IllegalArgumentException("未配置成品仓 WH-FIN，无法自动上架"));
        for (Map<String, Object> line : lines) {
            Long lineId = Long.parseLong(String.valueOf(line.get("lineId")));
            Double picked = jdbc.queryForObject(
                    "SELECT IFNULL(SUM(quantity),0) FROM wm_product_recpt_detail WHERE line_id = ?", Double.class, lineId);
            double received = toDouble(line.get("quantityRecived"));
            double gap = received - (picked == null ? 0 : picked);
            if (gap <= 0.001) {
                continue;
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("lineId", lineId);
            detail.put("recptId", recptId);
            detail.put("itemId", line.get("itemId"));
            detail.put("itemCode", line.get("itemCode"));
            detail.put("itemName", line.get("itemName"));
            detail.put("specification", line.get("specification"));
            detail.put("unitOfMeasure", line.get("unitOfMeasure"));
            detail.put("unitName", line.get("unitName"));
            detail.put("quantity", gap);
            detail.put("batchCode", line.get("batchCode"));
            detail.put("batchId", line.get("batchId"));
            detail.put("warehouseId", wh.get("warehouseId"));
            detail.put("warehouseCode", wh.get("warehouseCode"));
            detail.put("warehouseName", wh.get("warehouseName"));
            detail.put("locationId", wh.get("locationId"));
            detail.put("locationCode", wh.get("locationCode"));
            detail.put("locationName", wh.get("locationName"));
            detail.put("areaId", wh.get("areaId"));
            detail.put("areaCode", wh.get("areaCode"));
            detail.put("areaName", wh.get("areaName"));
            createDetail(detail);
        }
    }

    private void appendProductLine(Long recptId, Map<String, Object> wo) {
        Map<String, Object> item = resolveProductItem(wo);
        double plan = toDouble(wo.get("quantity"));
        double produced = toDouble(wo.get("quantityProduced"));
        double qty = plan > 0 ? plan : (produced > 0 ? produced : 1);
        String woCode = String.valueOf(wo.get("workorderCode"));
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("recptId", recptId);
        line.put("itemId", item.get("itemId"));
        line.put("itemCode", item.get("itemCode"));
        line.put("itemName", item.get("itemName"));
        line.put("specification", item.get("specification"));
        line.put("unitOfMeasure", item.get("unitOfMeasure"));
        line.put("unitName", item.get("unitName"));
        line.put("quantityRecived", qty);
        line.put("batchCode", "PB-" + woCode.replace("WO", ""));
        line.put("workorderId", wo.get("workorderId"));
        line.put("workorderCode", woCode);
        line.put("workorderName", wo.get("workorderName"));
        createLine(line);
    }

    private void enrichHeaderFromWorkOrder(Map<String, Object> body) {
        Object woIdObj = body.get("workorderId");
        if (woIdObj == null) {
            return;
        }
        long woId = Long.parseLong(String.valueOf(woIdObj));
        Map<String, Object> wo = workorderRepo.findById(woId).orElse(null);
        if (wo == null) {
            return;
        }
        Map<String, Object> item = resolveProductItem(wo);
        body.putIfAbsent("workorderCode", wo.get("workorderCode"));
        body.putIfAbsent("workorderName", wo.get("workorderName"));
        body.putIfAbsent("itemId", item.get("itemId"));
        body.putIfAbsent("itemCode", item.get("itemCode"));
        body.putIfAbsent("itemName", item.get("itemName"));
        body.putIfAbsent("specification", item.get("specification"));
        body.putIfAbsent("unitOfMeasure", item.get("unitOfMeasure"));
        body.putIfAbsent("unitName", item.get("unitName"));
    }

    private Map<String, Object> resolveProductItem(Map<String, Object> wo) {
        Long productId = wo.get("productId") instanceof Number n ? n.longValue() : null;
        if (productId != null) {
            Optional<Map<String, Object>> md = findMdItemByProductId(productId);
            if (md.isPresent()) {
                return md.get();
            }
        }
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("itemId", productId);
        fallback.put("itemCode", wo.get("productCode"));
        fallback.put("itemName", wo.get("productName"));
        fallback.put("specification", wo.get("productSpc"));
        fallback.put("unitOfMeasure", wo.get("unitOfMeasure"));
        fallback.put("unitName", wo.get("unitName"));
        return fallback;
    }

    private Optional<Map<String, Object>> findMdItemByProductId(Long productId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT item_id, item_code, item_name, specification, unit_of_measure, unit_name
                FROM md_item WHERE attr1 = 'PRODUCT' AND attr2 = ? LIMIT 1
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", rs.getLong("item_id"));
            m.put("itemCode", rs.getString("item_code"));
            m.put("itemName", rs.getString("item_name"));
            m.put("specification", rs.getString("specification"));
            m.put("unitOfMeasure", rs.getString("unit_of_measure"));
            m.put("unitName", rs.getString("unit_name"));
            return m;
        }, String.valueOf(productId));
        return rows.stream().findFirst();
    }

    private Optional<Map<String, Object>> defaultFgWarehouse() {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT w.warehouse_id, w.warehouse_code, w.warehouse_name,
                       sz.zone_id AS location_id, sz.zone_code AS location_code, sz.zone_name AS location_name,
                       sb.bin_id AS area_id, sb.bin_code AS area_code, sb.bin_name AS area_name
                FROM warehouse w
                JOIN storage_zone sz ON sz.warehouse_id = w.warehouse_id
                JOIN storage_bin sb ON sb.zone_id = sz.zone_id
                WHERE w.warehouse_code = 'WH-FIN'
                ORDER BY sz.zone_id, sb.bin_id
                LIMIT 1
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("warehouseId", rs.getLong("warehouse_id"));
            m.put("warehouseCode", rs.getString("warehouse_code"));
            m.put("warehouseName", rs.getString("warehouse_name"));
            m.put("locationId", rs.getLong("location_id"));
            m.put("locationCode", rs.getString("location_code"));
            m.put("locationName", rs.getString("location_name"));
            m.put("areaId", rs.getLong("area_id"));
            m.put("areaCode", rs.getString("area_code"));
            m.put("areaName", rs.getString("area_name"));
            return m;
        });
        return rows.stream().findFirst();
    }

    private String genRecptCode() {
        try {
            return autocodeGenService.genSerialCode("PRODUCTRECPT_CODE", null);
        } catch (RuntimeException ex) {
            return "PR" + System.currentTimeMillis() % 100000;
        }
    }

    private static double toDouble(Object v) {
        return v == null ? 0 : Double.parseDouble(String.valueOf(v));
    }
}
