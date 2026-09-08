package com.yunshu.mes.process.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProRouteProductService {

    private final JdbcTemplate jdbc;

    public ProRouteProductService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "pro_route_product", "record_id", MdDocSchemas.PRO_ROUTE_PRODUCT,
                MdDocSchemas.filter(params, "routeId", "itemId"),
                PageUtil.offset(pn, ps), ps);
    }

    public Map<String, Object> getById(Long id) {
        return WmSqlHelper.getById(jdbc, "pro_route_product", "record_id", "recordId",
                MdDocSchemas.PRO_ROUTE_PRODUCT, id);
    }

    public long count(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "pro_route_product", MdDocSchemas.PRO_ROUTE_PRODUCT,
                MdDocSchemas.filter(params, "routeId", "itemId"));
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "pro_route_product", MdDocSchemas.PRO_ROUTE_PRODUCT, body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("recordId")));
        return WmSqlHelper.update(jdbc, "pro_route_product", "record_id", MdDocSchemas.PRO_ROUTE_PRODUCT, body, id);
    }

    @Transactional
    public int delete(Long id) {
        return WmSqlHelper.delete(jdbc, "pro_route_product", "record_id", id);
    }

    @Transactional
    public int move(Map<String, Object> body) {
        Object recordId = body.get("recordId");
        Object orderNum = body.get("orderNum");
        if (recordId == null || orderNum == null) {
            return 0;
        }
        return jdbc.update("UPDATE pro_route_product SET order_num = ?, update_time = NOW(3) WHERE record_id = ?",
                orderNum, Long.parseLong(String.valueOf(recordId)));
    }
}
