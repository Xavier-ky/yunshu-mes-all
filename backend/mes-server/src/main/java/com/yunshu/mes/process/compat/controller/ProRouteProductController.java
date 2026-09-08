package com.yunshu.mes.process.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.process.compat.service.ProRouteProductService;
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
@RequestMapping("/api/mes/pro/routeproduct")
public class ProRouteProductController {

    private final ProRouteProductService service;

    public ProRouteProductController(ProRouteProductService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.list(params), service.count(params));
    }

    @GetMapping("/{recordId}")
    public Map<String, Object> getInfo(@PathVariable Long recordId) {
        Map<String, Object> row = service.getById(recordId);
        return row == null ? MesApiResponse.error("记录不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(service.create(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(service.update(body));
    }

    @PutMapping("/move")
    public Map<String, Object> move(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(service.move(body));
    }

    @DeleteMapping("/{recordIds}")
    public Map<String, Object> remove(@PathVariable String recordIds) {
        int n = 0;
        for (String p : recordIds.split(",")) {
            n += service.delete(Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
