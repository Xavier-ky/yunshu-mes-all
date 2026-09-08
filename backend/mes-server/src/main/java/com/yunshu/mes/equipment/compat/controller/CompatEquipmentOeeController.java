package com.yunshu.mes.equipment.compat.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Native-style OEE API for yunshu-ui equipment analytics (demo values from dv_machinery).
 */
@RestController
@RequestMapping("/api/equipment")
public class CompatEquipmentOeeController {

    private static final String SQL_MACHINERY =
            "SELECT machinery_code, machinery_name, status FROM dv_machinery ORDER BY machinery_code";

    private final JdbcTemplate jdbc;

    public CompatEquipmentOeeController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/oee")
    public ApiResponse<List<Map<String, Object>>> listOee(
            @RequestParam(required = false) String statDate,
            HttpServletRequest request) {
        String day = StringUtils.hasText(statDate) ? statDate.trim() : LocalDate.now().toString();
        List<Map<String, Object>> rows = new ArrayList<>();
        jdbc.query(SQL_MACHINERY, rs -> {
            String code = rs.getString("machinery_code");
            String name = rs.getString("machinery_name");
            String status = rs.getString("status");
            DemoOee oee = demoOeeFor(code, status);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deviceCode", code);
            row.put("deviceName", name);
            row.put("statDate", day);
            row.put("plannedTimeMin", oee.plannedMin);
            row.put("runTimeMin", oee.runMin);
            row.put("stopTimeMin", oee.stopMin);
            row.put("outputQty", oee.outputQty);
            row.put("goodQty", oee.goodQty);
            row.put("availabilityRate", oee.availability);
            row.put("performanceRate", oee.performance);
            row.put("qualityRate", oee.quality);
            row.put("oeeRate", oee.oee);
            rows.add(row);
        });
        rows.sort(Comparator.comparingDouble(r -> -toDouble(r.get("oeeRate"))));
        if (rows.size() > 20) {
            return ApiResponse.success(rows.subList(0, 20), request);
        }
        return ApiResponse.success(rows, request);
    }

    private static double toDouble(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static DemoOee demoOeeFor(String machineryCode, String status) {
        int hash = Math.abs(machineryCode == null ? 0 : machineryCode.hashCode());
        double jitter = (hash % 7) * 0.008;
        double availability;
        double performance;
        double quality;
        if ("WORKING".equalsIgnoreCase(status)) {
            availability = clamp(0.82 + jitter, 0.78, 0.92);
            performance = clamp(0.86 + (hash % 5) * 0.01, 0.80, 0.95);
            quality = clamp(0.97 + (hash % 3) * 0.005, 0.94, 0.995);
        } else if ("STOP".equalsIgnoreCase(status)) {
            availability = clamp(0.32 + jitter, 0.25, 0.42);
            performance = clamp(0.55 + (hash % 4) * 0.02, 0.45, 0.65);
            quality = clamp(0.92 + (hash % 2) * 0.01, 0.88, 0.96);
        } else if ("REPAIR".equalsIgnoreCase(status)) {
            availability = clamp(0.10 + jitter, 0.06, 0.18);
            performance = clamp(0.35 + (hash % 3) * 0.02, 0.28, 0.45);
            quality = clamp(0.85 + (hash % 2) * 0.01, 0.80, 0.90);
        } else {
            availability = clamp(0.65 + jitter, 0.55, 0.75);
            performance = clamp(0.72 + (hash % 4) * 0.015, 0.65, 0.82);
            quality = clamp(0.94 + (hash % 3) * 0.005, 0.90, 0.98);
        }
        double oee = round3(availability * performance * quality);
        int plannedMin = 480;
        int runMin = (int) Math.round(plannedMin * availability);
        int stopMin = plannedMin - runMin;
        int outputQty = (int) Math.round(runMin * performance * 1.2);
        int goodQty = (int) Math.round(outputQty * quality);
        return new DemoOee(plannedMin, runMin, stopMin, outputQty, goodQty,
                round3(availability), round3(performance), round3(quality), oee);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private record DemoOee(
            int plannedMin,
            int runMin,
            int stopMin,
            int outputQty,
            int goodQty,
            double availability,
            double performance,
            double quality,
            double oee) {
    }
}
