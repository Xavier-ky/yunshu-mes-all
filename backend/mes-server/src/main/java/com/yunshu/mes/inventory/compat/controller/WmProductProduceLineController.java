package com.yunshu.mes.inventory.compat.controller;

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
@RequestMapping("/api/mes/wm/productproduceline")
public class WmProductProduceLineController {

    private final JdbcTemplate jdbc;

    public WmProductProduceLineController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "wm_product_produce_line", "line_id",
                MdDocSchemas.WM_PRODUCT_PRODUCE_LINE, MdDocSchemas.filter(params, "recordId"),
                PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "wm_product_produce_line",
                MdDocSchemas.WM_PRODUCT_PRODUCE_LINE, MdDocSchemas.filter(params, "recordId")));
    }

    @GetMapping("/{lineId}")
    public Map<String, Object> getInfo(@PathVariable Long lineId) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "wm_product_produce_line", "line_id", "lineId",
                MdDocSchemas.WM_PRODUCT_PRODUCE_LINE, lineId);
        return row == null ? MesApiResponse.error("行不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "wm_product_produce_line", MdDocSchemas.WM_PRODUCT_PRODUCE_LINE, body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("lineId")));
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "wm_product_produce_line", "line_id",
                MdDocSchemas.WM_PRODUCT_PRODUCE_LINE, body, id));
    }

    @DeleteMapping("/{lineIds}")
    public Map<String, Object> remove(@PathVariable String lineIds) {
        int n = 0;
        for (String p : lineIds.split(",")) {
            n += WmSqlHelper.delete(jdbc, "wm_product_produce_line", "line_id", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
