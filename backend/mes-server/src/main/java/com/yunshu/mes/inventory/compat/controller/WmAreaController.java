package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmAreaService;
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
@RequestMapping("/api/mes/wm/area")
public class WmAreaController {

    private final WmAreaService service;

    public WmAreaController(WmAreaService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = service.list(params);
        return MesApiResponse.table(rows, service.count(params));
    }

    @GetMapping("/{areaId}")
    public Map<String, Object> getInfo(@PathVariable Long areaId) {
        return service.getById(areaId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("库位不存在"));
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

    @DeleteMapping("/{areaIds}")
    public Map<String, Object> remove(@PathVariable String areaIds) {
        try {
            int total = 0;
            for (String part : areaIds.split(",")) {
                total += service.delete(Long.parseLong(part.trim()));
            }
            return MesApiResponse.toAjax(total);
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/getLocationName")
    public Map<String, Object> getLocationName(@RequestParam(required = false) Long areaId) {
        if (areaId == null) {
            return MesApiResponse.ok(Map.of("locationName", ""));
        }
        return MesApiResponse.ok(service.getLocationName(areaId));
    }
}
