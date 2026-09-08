package com.yunshu.mes.planning.compat.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RouteProcessService {

    private final JdbcTemplate jdbc;

    public RouteProcessService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listProductProcess(Long productId) {
        List<Map<String, Object>> routes = jdbc.query("""
                SELECT pr.route_id, pr.route_code, pr.route_name
                FROM product_route pdr
                JOIN process_route pr ON pdr.route_id = pr.route_id
                WHERE pdr.product_id = ?
                ORDER BY pdr.is_default DESC, pdr.route_id
                LIMIT 1
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("routeId", rs.getLong("route_id"));
            m.put("routeCode", rs.getString("route_code"));
            m.put("routeName", rs.getString("route_name"));
            return m;
        }, productId);

        if (routes.isEmpty()) {
            return List.of();
        }
        Long routeId = (Long) routes.get(0).get("routeId");
        return jdbc.query("""
                SELECT prs.route_step_id, prs.step_seq AS step_order, prs.step_id,
                       ps.step_code AS process_code, ps.step_name AS process_name,
                       ps.step_type, 60 AS default_duration
                FROM process_route_step prs
                JOIN process_step ps ON prs.step_id = ps.step_id
                WHERE prs.route_id = ?
                ORDER BY prs.step_seq
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("routeId", routeId);
            m.put("processId", rs.getLong("step_id"));
            m.put("processCode", rs.getString("process_code"));
            m.put("processName", rs.getString("process_name"));
            m.put("processType", rs.getString("step_type"));
            m.put("orderNum", rs.getInt("step_order"));
            m.put("defaultDuration", rs.getObject("default_duration"));
            return m;
        }, routeId);
    }
}
