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
public class ProRouteService {

    private final JdbcTemplate jdbc;

    public ProRouteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "pro_route", "route_id", MdDocSchemas.PRO_ROUTE,
                MdDocSchemas.filter(params, "routeCode", "routeName", "enableFlag"),
                PageUtil.offset(pn, ps), ps);
    }

    public long count(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "pro_route", MdDocSchemas.PRO_ROUTE,
                MdDocSchemas.filter(params, "routeCode", "routeName", "enableFlag"));
    }

    public List<Map<String, Object>> summarizeSteps(List<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(routeIds.size(), "?"));
        String sql = """
                SELECT route_id, process_name, color_code, key_flag, order_num
                FROM pro_route_process
                WHERE route_id IN (%s)
                ORDER BY route_id, order_num
                """.formatted(placeholders);
        return jdbc.query(sql, (rs, n) -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("routeId", rs.getLong("route_id"));
            row.put("processName", rs.getString("process_name"));
            row.put("colorCode", rs.getString("color_code"));
            row.put("keyFlag", rs.getString("key_flag"));
            row.put("orderNum", rs.getInt("order_num"));
            return row;
        }, routeIds.toArray());
    }

    public Map<String, Object> getById(Long id) {
        return WmSqlHelper.getById(jdbc, "pro_route", "route_id", "routeId", MdDocSchemas.PRO_ROUTE, id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "pro_route", MdDocSchemas.PRO_ROUTE, body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("routeId")));
        return WmSqlHelper.update(jdbc, "pro_route", "route_id", MdDocSchemas.PRO_ROUTE, body, id);
    }

    @Transactional
    public int delete(Long id) {
        jdbc.update("DELETE FROM pro_route_process WHERE route_id = ?", id);
        jdbc.update("DELETE FROM pro_route_product WHERE route_id = ?", id);
        jdbc.update("DELETE FROM pro_route_product_bom WHERE route_id = ?", id);
        return WmSqlHelper.delete(jdbc, "pro_route", "route_id", id);
    }
}
