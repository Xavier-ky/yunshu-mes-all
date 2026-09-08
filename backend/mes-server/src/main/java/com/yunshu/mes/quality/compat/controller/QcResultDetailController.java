package com.yunshu.mes.quality.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.quality.compat.QcDocSchemas;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
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
@RequestMapping("/api/mes/qc/qcresultdetail")
public class QcResultDetailController {

    private final JdbcTemplate jdbc;

    public QcResultDetailController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        var filters = QcDocSchemas.filter(params, "resultId", "indexCode", "indexName", "indexType");
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "qc_result_detail", "detail_id",
                QcDocSchemas.QC_RESULT_DETAIL, filters, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "qc_result_detail",
                QcDocSchemas.QC_RESULT_DETAIL, filters));
    }

    @GetMapping("/listDetails")
    public Map<String, Object> listDetails(@RequestParam Map<String, String> params) {
        if (params.containsKey("resultId") && params.get("resultId") != null && !params.get("resultId").isBlank()) {
            var filters = QcDocSchemas.filter(params, "resultId");
            List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "qc_result_detail", "detail_id",
                    QcDocSchemas.QC_RESULT_DETAIL, filters, 0, 1000);
            return MesApiResponse.ok(rows);
        }
        return MesApiResponse.ok(List.of());
    }

    @GetMapping("/{detailId}")
    public Map<String, Object> getInfo(@PathVariable Long detailId) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "qc_result_detail", "detail_id", "detailId",
                QcDocSchemas.QC_RESULT_DETAIL, detailId);
        return row == null ? MesApiResponse.error("明细不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "qc_result_detail", QcDocSchemas.QC_RESULT_DETAIL, body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("detailId")));
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "qc_result_detail", "detail_id",
                QcDocSchemas.QC_RESULT_DETAIL, body, id));
    }

    @DeleteMapping("/{detailIds}")
    public Map<String, Object> remove(@PathVariable String detailIds) {
        int n = 0;
        for (String p : detailIds.split(",")) {
            n += WmSqlHelper.delete(jdbc, "qc_result_detail", "detail_id", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
