package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmLocationService;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.List;
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
@RequestMapping("/api/mes/wm/location")
public class WmLocationController {

    private final WmLocationService service;

    public WmLocationController(WmLocationService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = service.list(params);
        return MesApiResponse.table(rows, service.count(params));
    }

    @GetMapping("/{locationId}")
    public Map<String, Object> getInfo(@PathVariable Long locationId) {
        return service.getById(locationId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("库区不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.ok(service.create(body));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.toAjax(service.update(body));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{locationIds}")
    public Map<String, Object> remove(@PathVariable String locationIds) {
        try {
            int total = 0;
            for (String part : locationIds.split(",")) {
                total += service.delete(Long.parseLong(part.trim()));
            }
            return MesApiResponse.toAjax(total);
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/setProductMixing")
    public Map<String, Object> setProductMixing(@RequestParam Long locationId, @RequestParam Boolean flag) {
        service.setProductMixing(locationId, flag);
        return MesApiResponse.ok();
    }

    @PostMapping("/setBatchMixing")
    public Map<String, Object> setBatchMixing(@RequestParam Long locationId, @RequestParam Boolean flag) {
        service.setBatchMixing(locationId, flag);
        return MesApiResponse.ok();
    }
}
