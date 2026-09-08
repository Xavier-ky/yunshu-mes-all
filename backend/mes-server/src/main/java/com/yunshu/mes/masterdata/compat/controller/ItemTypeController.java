package com.yunshu.mes.masterdata.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
@RequestMapping("/api/mes/md/itemtype")
public class ItemTypeController {

    private final JdbcTemplate jdbc;

    public ItemTypeController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "md_item_type", "item_type_id",
                MdDocSchemas.MD_ITEM_TYPE, MdDocSchemas.filter(params, "itemTypeName", "itemOrProduct"),
                PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "md_item_type", MdDocSchemas.MD_ITEM_TYPE,
                MdDocSchemas.filter(params, "itemTypeName", "itemOrProduct")));
    }

    @GetMapping("/{itemTypeId}")
    public Map<String, Object> getInfo(@PathVariable Long itemTypeId) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "md_item_type", "item_type_id", "itemTypeId",
                MdDocSchemas.MD_ITEM_TYPE, itemTypeId);
        return row == null ? MesApiResponse.error("分类不存在") : MesApiResponse.ok(row);
    }

    @GetMapping("/list/exclude/{itemTypeId}")
    public Map<String, Object> listExclude(@PathVariable Long itemTypeId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT item_type_id, item_type_code, item_type_name, parent_type_id, item_or_product, order_num, enable_flag, remark
                FROM md_item_type WHERE item_type_id <> ? ORDER BY order_num, item_type_id
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemTypeId", rs.getLong("item_type_id"));
            m.put("itemTypeCode", rs.getString("item_type_code"));
            m.put("itemTypeName", rs.getString("item_type_name"));
            m.put("parentTypeId", rs.getLong("parent_type_id"));
            m.put("itemOrProduct", rs.getString("item_or_product"));
            m.put("orderNum", rs.getInt("order_num"));
            m.put("enableFlag", rs.getString("enable_flag"));
            m.put("remark", rs.getString("remark"));
            return m;
        }, itemTypeId);
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/treeselect")
    public Map<String, Object> treeselect() {
        List<Map<String, Object>> types = jdbc.query("""
                SELECT item_type_id, item_type_name, parent_type_id, item_or_product
                FROM md_item_type WHERE enable_flag = 'Y' ORDER BY order_num, item_type_id
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getLong("item_type_id"));
            m.put("label", rs.getString("item_type_name"));
            m.put("parentId", rs.getLong("parent_type_id"));
            m.put("itemOrProduct", rs.getString("item_or_product"));
            return m;
        });
        List<Map<String, Object>> tree = buildTree(types, 0L);
        return MesApiResponse.ok(tree);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "md_item_type", MdDocSchemas.MD_ITEM_TYPE, body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("itemTypeId")));
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "md_item_type", "item_type_id",
                MdDocSchemas.MD_ITEM_TYPE, body, id));
    }

    @DeleteMapping("/{itemTypeIds}")
    public Map<String, Object> remove(@PathVariable String itemTypeIds) {
        int n = 0;
        for (String p : itemTypeIds.split(",")) {
            n += WmSqlHelper.delete(jdbc, "md_item_type", "item_type_id", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }

    private List<Map<String, Object>> buildTree(List<Map<String, Object>> nodes, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            Long pid = ((Number) node.get("parentId")).longValue();
            if (pid.equals(parentId)) {
                Map<String, Object> n = new LinkedHashMap<>(node);
                n.remove("parentId");
                n.put("children", buildTree(nodes, ((Number) node.get("id")).longValue()));
                tree.add(n);
            }
        }
        return tree;
    }
}
