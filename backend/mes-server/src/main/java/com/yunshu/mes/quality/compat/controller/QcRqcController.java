package com.yunshu.mes.quality.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.quality.compat.QcDocSchemas;
import com.yunshu.mes.quality.compat.QcFinishWriteback;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/mes/qc/rqc")
public class QcRqcController {

    private final JdbcTemplate jdbc;

    public QcRqcController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        var filters = QcDocSchemas.filter(params, "rqcCode", "rqcName", "rqcType", "itemCode", "itemName", "status", "checkResult");
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "qc_rqc", "rqc_id",
                QcDocSchemas.QC_RQC, filters, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "qc_rqc", QcDocSchemas.QC_RQC, filters));
    }

    @GetMapping("/{rqcId}")
    public Map<String, Object> getInfo(@PathVariable Long rqcId) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "qc_rqc", "rqc_id", "rqcId",
                QcDocSchemas.QC_RQC, rqcId);
        return row == null ? MesApiResponse.error("退料检验单不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Long id = WmSqlHelper.insert(jdbc, "qc_rqc", QcDocSchemas.QC_RQC, body);
        if (id != null && body.get("templateId") != null) {
            generateLine(id, Long.parseLong(String.valueOf(body.get("templateId"))));
        }
        return MesApiResponse.ok(id);
    }

    @PutMapping
    @Transactional
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("rqcId")));
        if (QcFinishWriteback.isFinished(body)) {
            String err = QcFinishWriteback.validateQualifiedQty(body);
            if (err != null) {
                return MesApiResponse.error(err);
            }
            QcFinishWriteback.onRqcFinished(jdbc, body);
        }
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "qc_rqc", "rqc_id", QcDocSchemas.QC_RQC, body, id));
    }

    @DeleteMapping("/{rqcIds}")
    @Transactional
    public Map<String, Object> remove(@PathVariable String rqcIds) {
        int n = 0;
        for (String p : rqcIds.split(",")) {
            Long id = Long.parseLong(p.trim());
            String err = QcFinishWriteback.requirePrepareOrError(jdbc, "qc_rqc", "rqc_id", id);
            if (err != null) {
                return MesApiResponse.error(err);
            }
            jdbc.update("DELETE FROM qc_rqc_line WHERE rqc_id = ?", id);
            jdbc.update("DELETE FROM qc_defect_record WHERE qc_id = ? AND qc_type IN ('RQC','PRQC','CRQC')", id);
            n += WmSqlHelper.delete(jdbc, "qc_rqc", "rqc_id", id);
        }
        return MesApiResponse.toAjax(n);
    }

    private void generateLine(Long rqcId, Long templateId) {
        List<Map<String, Object>> indexes = WmSqlHelper.list(jdbc, "qc_template_index", "record_id",
                QcDocSchemas.QC_TEMPLATE_INDEX, Map.of("templateId", String.valueOf(templateId)), 0, 500);
        for (Map<String, Object> idx : indexes) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("rqcId", rqcId);
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
            WmSqlHelper.insert(jdbc, "qc_rqc_line", QcDocSchemas.QC_RQC_LINE, line);
        }
    }
}
