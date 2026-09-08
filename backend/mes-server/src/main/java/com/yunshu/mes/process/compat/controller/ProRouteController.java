package com.yunshu.mes.process.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.process.compat.service.ProRouteService;
import java.util.Map;
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
@RequestMapping("/api/mes/pro/proroute")
public class ProRouteController {

    private final ProRouteService service;

    public ProRouteController(ProRouteService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.list(params), service.count(params));
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam String routeIds) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (String part : routeIds.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                ids.add(Long.parseLong(trimmed));
            }
        }
        java.util.Map<Long, java.util.List<Map<String, Object>>> grouped = new java.util.LinkedHashMap<>();
        for (Map<String, Object> step : service.summarizeSteps(ids)) {
            Long routeId = ((Number) step.get("routeId")).longValue();
            grouped.computeIfAbsent(routeId, k -> new java.util.ArrayList<>()).add(step);
        }
        return MesApiResponse.ok(grouped);
    }

    @GetMapping("/{routeId}")
    public Map<String, Object> getInfo(@PathVariable Long routeId) {
        Map<String, Object> row = service.getById(routeId);
        return row == null ? MesApiResponse.error("工艺路线不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(service.create(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(service.update(body));
    }

    @DeleteMapping("/{routeIds}")
    public Map<String, Object> remove(@PathVariable String routeIds) {
        int n = 0;
        for (String p : routeIds.split(",")) {
            n += service.delete(Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
