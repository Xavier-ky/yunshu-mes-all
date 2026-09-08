package com.yunshu.mes.masterdata.compat.controller;

import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/md/batchconfig")
public class MdBatchConfigController {

    private final JdbcTemplate jdbc;

    public MdBatchConfigController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "md_item_batch_config", "config_id",
                MdDocSchemas.MD_ITEM_BATCH_CONFIG, MdDocSchemas.filter(params, "itemId"), 0, 100);
        return MesApiResponse.table(rows, rows.size());
    }

    @GetMapping("/{itemId}")
    public Map<String, Object> getByItem(@PathVariable Long itemId) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT * FROM md_item_batch_config WHERE item_id = ? LIMIT 1",
                (rs, n) -> com.yunshu.mes.inventory.compat.WmRowMapper.map(rs, MdDocSchemas.MD_ITEM_BATCH_CONFIG),
                itemId);
        return rows.isEmpty() ? MesApiResponse.ok(Map.of("itemId", itemId)) : MesApiResponse.ok(rows.get(0));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "md_item_batch_config", MdDocSchemas.MD_ITEM_BATCH_CONFIG, body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Object configId = body.get("configId");
        if (configId != null) {
            return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "md_item_batch_config", "config_id",
                    MdDocSchemas.MD_ITEM_BATCH_CONFIG, body, Long.parseLong(String.valueOf(configId))));
        }
        Object itemId = body.get("itemId");
        List<Map<String, Object>> existing = jdbc.query(
                "SELECT config_id FROM md_item_batch_config WHERE item_id = ? LIMIT 1",
                (rs, n) -> Map.of("configId", rs.getLong("config_id")), itemId);
        if (existing.isEmpty()) {
            return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "md_item_batch_config", MdDocSchemas.MD_ITEM_BATCH_CONFIG, body));
        }
        body.put("configId", existing.get(0).get("configId"));
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "md_item_batch_config", "config_id",
                MdDocSchemas.MD_ITEM_BATCH_CONFIG, body, Long.parseLong(String.valueOf(existing.get(0).get("configId")))));
    }
}
