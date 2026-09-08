package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/wm/productproduce")
public class WmProductProduceController {

    private static final Map<String, String> SCHEMA = new LinkedHashMap<>();
    static {
        SCHEMA.put("recordId", "record_id");
        SCHEMA.put("feedbackId", "feedback_id");
        SCHEMA.put("status", "status");
    }

    private final JdbcTemplate jdbc;

    public WmProductProduceController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "wm_product_produce", "record_id", SCHEMA,
                MdDocSchemas.filter(params, "feedbackId"), PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "wm_product_produce", SCHEMA,
                MdDocSchemas.filter(params, "feedbackId")));
    }
}
