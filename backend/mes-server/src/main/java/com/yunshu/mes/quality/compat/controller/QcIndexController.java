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
@RequestMapping("/api/mes/qc/qcindex")
public class QcIndexController {

    private final JdbcTemplate jdbc;

    public QcIndexController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        var filters = QcDocSchemas.filter(params, "indexCode", "indexName", "indexType");
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "qc_index", "index_id",
                QcDocSchemas.QC_INDEX, filters, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "qc_index", QcDocSchemas.QC_INDEX, filters));
    }

    @GetMapping("/{indexId}")
    public Map<String, Object> getInfo(@PathVariable Long indexId) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "qc_index", "index_id", "indexId",
                QcDocSchemas.QC_INDEX, indexId);
        return row == null ? MesApiResponse.error("检测项不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "qc_index", QcDocSchemas.QC_INDEX, body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("indexId")));
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "qc_index", "index_id",
                QcDocSchemas.QC_INDEX, body, id));
    }

    @DeleteMapping("/{indexIds}")
    public Map<String, Object> remove(@PathVariable String indexIds) {
        int n = 0;
        for (String p : indexIds.split(",")) {
            n += WmSqlHelper.delete(jdbc, "qc_index", "index_id", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
