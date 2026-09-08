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
@RequestMapping("/api/mes/qc/qcresult")
public class QcResultController {

    private final JdbcTemplate jdbc;

    public QcResultController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        var filters = QcDocSchemas.filter(params, "resultCode", "sourceDocCode", "sourceDocType", "itemCode", "itemName", "snCode");
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "qc_result", "result_id",
                QcDocSchemas.QC_RESULT, filters, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "qc_result", QcDocSchemas.QC_RESULT, filters));
    }

    @GetMapping("/{resultId}")
    public Map<String, Object> getInfo(@PathVariable Long resultId) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "qc_result", "result_id", "resultId",
                QcDocSchemas.QC_RESULT, resultId);
        return row == null ? MesApiResponse.error("检测结果不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "qc_result", QcDocSchemas.QC_RESULT, body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("resultId")));
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "qc_result", "result_id",
                QcDocSchemas.QC_RESULT, body, id));
    }

    @DeleteMapping("/{resultIds}")
    public Map<String, Object> remove(@PathVariable String resultIds) {
        int n = 0;
        for (String p : resultIds.split(",")) {
            Long id = Long.parseLong(p.trim());
            jdbc.update("DELETE FROM qc_result_detail WHERE result_id = ?", id);
            n += WmSqlHelper.delete(jdbc, "qc_result", "result_id", id);
        }
        return MesApiResponse.toAjax(n);
    }
}
