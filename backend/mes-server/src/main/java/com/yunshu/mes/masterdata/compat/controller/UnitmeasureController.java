package com.yunshu.mes.masterdata.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import com.yunshu.mes.planning.compat.MesApiResponse;
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
@RequestMapping("/api/mes/md/unitmeasure")
public class UnitmeasureController {

    private final JdbcTemplate jdbc;

    public UnitmeasureController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "md_unit_measure", "measure_id",
                MdDocSchemas.MD_UNIT_MEASURE, MdDocSchemas.filter(params, "measureCode", "measureName"),
                PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "md_unit_measure", MdDocSchemas.MD_UNIT_MEASURE,
                MdDocSchemas.filter(params, "measureCode", "measureName")));
    }

    @GetMapping("/listAll")
    public Map<String, Object> listAll() {
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "md_unit_measure", "measure_id",
                MdDocSchemas.MD_UNIT_MEASURE, Map.of(), 0, 1000);
        return MesApiResponse.ok(rows);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "md_unit_measure", MdDocSchemas.MD_UNIT_MEASURE, body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("measureId")));
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "md_unit_measure", "measure_id",
                MdDocSchemas.MD_UNIT_MEASURE, body, id));
    }

    @DeleteMapping("/{measureIds}")
    public Map<String, Object> remove(@PathVariable String measureIds) {
        int n = 0;
        for (String p : measureIds.split(",")) {
            n += WmSqlHelper.delete(jdbc, "md_unit_measure", "measure_id", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
