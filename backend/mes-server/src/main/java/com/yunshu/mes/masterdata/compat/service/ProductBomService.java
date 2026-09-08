package com.yunshu.mes.masterdata.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductBomService {

    private final JdbcTemplate jdbc;

    public ProductBomService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "md_product_bom", "bom_id", MdDocSchemas.MD_PRODUCT_BOM,
                MdDocSchemas.filter(params, "itemId", "bomItemCode", "bomItemName"),
                PageUtil.offset(pn, ps), ps);
    }

    public long count(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "md_product_bom", MdDocSchemas.MD_PRODUCT_BOM,
                MdDocSchemas.filter(params, "itemId", "bomItemCode", "bomItemName"));
    }

    public Map<String, Object> getById(Long id) {
        return WmSqlHelper.getById(jdbc, "md_product_bom", "bom_id", "bomId", MdDocSchemas.MD_PRODUCT_BOM, id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "md_product_bom", MdDocSchemas.MD_PRODUCT_BOM, body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("bomId")));
        return WmSqlHelper.update(jdbc, "md_product_bom", "bom_id", MdDocSchemas.MD_PRODUCT_BOM, body, id);
    }

    @Transactional
    public int delete(Long id) {
        return WmSqlHelper.delete(jdbc, "md_product_bom", "bom_id", id);
    }
}
