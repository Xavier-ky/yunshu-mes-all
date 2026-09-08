package com.yunshu.mes.masterdata.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmRowMapper;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MdItemService {

    private final JdbcTemplate jdbc;

    public MdItemService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        StringBuilder sql = new StringBuilder("SELECT i.* FROM md_item i WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendItemFilters(sql, args, params);
        sql.append(" ORDER BY i.item_id DESC LIMIT ? OFFSET ?");
        args.add(ps);
        args.add(PageUtil.offset(pn, ps));
        return jdbc.query(sql.toString(),
                (rs, n) -> WmRowMapper.map(rs, MdDocSchemas.MD_ITEM), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM md_item i WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendItemFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    private void appendItemFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        if (StringUtils.hasText(params.get("itemCode"))) {
            sql.append(" AND i.item_code LIKE ?");
            args.add("%" + params.get("itemCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemName"))) {
            sql.append(" AND i.item_name LIKE ?");
            args.add("%" + params.get("itemName").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemOrProduct"))) {
            sql.append(" AND i.item_or_product = ?");
            args.add(params.get("itemOrProduct").trim());
        }
        if (StringUtils.hasText(params.get("enableFlag"))) {
            sql.append(" AND i.enable_flag = ?");
            args.add(params.get("enableFlag").trim());
        }
        String itemTypeId = params.get("itemTypeId");
        if (StringUtils.hasText(itemTypeId) && !"0".equals(itemTypeId.trim())) {
            long tid = Long.parseLong(itemTypeId.trim());
            sql.append("""
                     AND (i.item_type_id = ?
                      OR i.item_type_id IN (
                          SELECT t.item_type_id FROM md_item_type t
                          WHERE FIND_IN_SET(?, t.ancestors) > 0
                      ))
                    """);
            args.add(tid);
            args.add(String.valueOf(tid));
        }
    }

    public Map<String, Object> getById(Long id) {
        return WmSqlHelper.getById(jdbc, "md_item", "item_id", "itemId", MdDocSchemas.MD_ITEM, id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "md_item", MdDocSchemas.MD_ITEM, body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("itemId")));
        return WmSqlHelper.update(jdbc, "md_item", "item_id", MdDocSchemas.MD_ITEM, body, id);
    }

    @Transactional
    public int delete(Long id) {
        return WmSqlHelper.delete(jdbc, "md_item", "item_id", id);
    }
}
