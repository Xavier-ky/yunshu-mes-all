package com.yunshu.mes.quality.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.inventory.compat.service.WmProductRecptService;
import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.quality.compat.QcDocSchemas;
import com.yunshu.mes.planning.workflow.WorkOrderLifecycleService;
import com.yunshu.mes.quality.compat.QcFinishWriteback;
import com.yunshu.mes.quality.compat.QcTemplateResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/qc/ipqc")
public class QcIpqcController {

    private static final Logger log = LoggerFactory.getLogger(QcIpqcController.class);

    private final JdbcTemplate jdbc;
    private final WorkOrderLifecycleService lifecycle;
    private final WmProductRecptService productRecptService;

    public QcIpqcController(
            JdbcTemplate jdbc,
            WorkOrderLifecycleService lifecycle,
            WmProductRecptService productRecptService) {
        this.jdbc = jdbc;
        this.lifecycle = lifecycle;
        this.productRecptService = productRecptService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        var filters = QcDocSchemas.filter(params, "ipqcCode", "ipqcName", "ipqcType", "workorderCode",
                "itemCode", "itemName", "status", "checkResult");
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "qc_ipqc", "ipqc_id",
                QcDocSchemas.QC_IPQC, filters, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "qc_ipqc", QcDocSchemas.QC_IPQC, filters));
    }

    @GetMapping("/{ipqcId}")
    public Map<String, Object> getInfo(@PathVariable Long ipqcId) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "qc_ipqc", "ipqc_id", "ipqcId",
                QcDocSchemas.QC_IPQC, ipqcId);
        return row == null ? MesApiResponse.error("过程检验单不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        QcTemplateResolver.ensureTemplateId(jdbc, body);
        Long id = WmSqlHelper.insert(jdbc, "qc_ipqc", QcDocSchemas.QC_IPQC, body);
        if (id != null && body.get("templateId") != null) {
            generateLine(id, Long.parseLong(String.valueOf(body.get("templateId"))));
        }
        return MesApiResponse.ok(id);
    }

    @PutMapping
    @Transactional
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("ipqcId")));
        QcTemplateResolver.ensureTemplateId(jdbc, body);
        if (QcFinishWriteback.isFinished(body)) {
            String err = QcFinishWriteback.validateQualifiedQty(body);
            if (err != null) {
                return MesApiResponse.error(err);
            }
            QcFinishWriteback.onIpqcFinished(jdbc, body);
            writebackWorkOrderLifecycle(body);
        }
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "qc_ipqc", "ipqc_id", QcDocSchemas.QC_IPQC, body, id));
    }

    @DeleteMapping("/{ipqcIds}")
    @Transactional
    public Map<String, Object> remove(@PathVariable String ipqcIds) {
        int n = 0;
        for (String p : ipqcIds.split(",")) {
            Long id = Long.parseLong(p.trim());
            String err = QcFinishWriteback.requirePrepareOrError(jdbc, "qc_ipqc", "ipqc_id", id);
            if (err != null) {
                return MesApiResponse.error(err);
            }
            jdbc.update("DELETE FROM qc_ipqc_line WHERE ipqc_id = ?", id);
            jdbc.update("DELETE FROM qc_defect_record WHERE qc_id = ? AND qc_type = 'IPQC'", id);
            n += WmSqlHelper.delete(jdbc, "qc_ipqc", "ipqc_id", id);
        }
        return MesApiResponse.toAjax(n);
    }

    private void generateLine(Long ipqcId, Long templateId) {
        List<Map<String, Object>> indexes = WmSqlHelper.list(jdbc, "qc_template_index", "record_id",
                QcDocSchemas.QC_TEMPLATE_INDEX, Map.of("templateId", String.valueOf(templateId)), 0, 500);
        for (Map<String, Object> idx : indexes) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("ipqcId", ipqcId);
            line.put("indexId", idx.get("indexId"));
            line.put("indexCode", idx.get("indexCode"));
            line.put("indexName", idx.get("indexName"));
            line.put("indexType", idx.get("indexType"));
            line.put("qcTool", idx.get("qcTool"));
            line.put("checkMethod", idx.get("checkMethod"));
            line.put("standerVal", idx.get("standerVal"));
            line.put("unitOfMeasure", idx.get("unitOfMeasure"));
            line.put("thresholdMax", idx.get("thresholdMax"));
            line.put("thresholdMin", idx.get("thresholdMin"));
            line.put("crQuantity", 0);
            line.put("majQuantity", 0);
            line.put("minQuantity", 0);
            WmSqlHelper.insert(jdbc, "qc_ipqc_line", QcDocSchemas.QC_IPQC_LINE, line);
        }
    }

    private void writebackWorkOrderLifecycle(Map<String, Object> body) {
        Long woId = longOrNull(body.get("workorderId"));
        if (woId == null) {
            Object code = body.get("workorderCode");
            if (code != null) {
                woId = lifecycle.resolveWorkOrderIdFromCode(String.valueOf(code));
            }
        }
        if (woId == null) {
            return;
        }
        String checkResult = body.get("checkResult") == null ? "" : String.valueOf(body.get("checkResult"));
        if (QcFinishWriteback.ACCEPT.equalsIgnoreCase(checkResult)) {
            lifecycle.advance(woId, WorkOrderLifecycleService.QC_PASSED);
            maybeAutoCreateProductRecpt(woId);
        } else {
            lifecycle.advance(woId, WorkOrderLifecycleService.QC_FAILED);
        }
    }

    /** 工单待检全部完成后，自动生成成品入库草稿单（幂等） */
    private void maybeAutoCreateProductRecpt(Long workOrderId) {
        try {
            Integer feedbackPending = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM pro_feedback
                    WHERE workorder_id = ? AND status = 'FINISHED' AND IFNULL(quantity_uncheck, 0) > 0
                    """, Integer.class, workOrderId);
            if (feedbackPending != null && feedbackPending > 0) {
                return;
            }
            Integer producePending = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM wm_product_produce p
                    JOIN wm_product_produce_line l ON l.record_id = p.record_id
                    WHERE p.workorder_id = ? AND p.status NOT IN ('FINISHED', 'CANCELED')
                      AND (l.quality_status IS NULL OR l.quality_status IN ('', 'NT', 'WAIT'))
                    """, Integer.class, workOrderId);
            if (producePending != null && producePending > 0) {
                return;
            }
            productRecptService.createFromWorkOrder(workOrderId);
        } catch (Exception ex) {
            log.warn("auto product recpt skipped for workOrderId={}: {}", workOrderId, ex.getMessage());
        }
    }

    private static Long longOrNull(Object v) {
        if (v == null || "".equals(String.valueOf(v))) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
