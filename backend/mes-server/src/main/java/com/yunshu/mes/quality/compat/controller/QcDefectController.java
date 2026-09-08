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
@RequestMapping("/api/mes/qc/qcdefect")
public class QcDefectController {

    private final JdbcTemplate jdbc;

    public QcDefectController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        var filters = QcDocSchemas.filter(params, "defectCode", "defectName", "indexType", "defectLevel");
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "qc_defect", "defect_id",
                QcDocSchemas.QC_DEFECT, filters, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "qc_defect", QcDocSchemas.QC_DEFECT, filters));
    }

    @GetMapping("/{defectId}")
    public Map<String, Object> getInfo(@PathVariable Long defectId) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "qc_defect", "defect_id", "defectId",
                QcDocSchemas.QC_DEFECT, defectId);
        return row == null ? MesApiResponse.error("缺陷不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "qc_defect", QcDocSchemas.QC_DEFECT, body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("defectId")));
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "qc_defect", "defect_id",
                QcDocSchemas.QC_DEFECT, body, id));
    }

    @DeleteMapping("/{defectIds}")
    public Map<String, Object> remove(@PathVariable String defectIds) {
        int n = 0;
        for (String p : defectIds.split(",")) {
            n += WmSqlHelper.delete(jdbc, "qc_defect", "defect_id", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
