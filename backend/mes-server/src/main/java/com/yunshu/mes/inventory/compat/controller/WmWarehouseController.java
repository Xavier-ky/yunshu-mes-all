package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmWarehouseService;
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
@RequestMapping("/api/mes/wm/warehouse")
public class WmWarehouseController {

    private final WmWarehouseService service;

    public WmWarehouseController(WmWarehouseService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = service.list(params);
        long total = service.count(params);
        return MesApiResponse.table(rows, total);
    }

    @GetMapping("/getTreeList")
    public Map<String, Object> getTreeList() {
        return MesApiResponse.ok(service.getTreeList());
    }

    @GetMapping("/{warehouseId}")
    public Map<String, Object> getInfo(@PathVariable Long warehouseId) {
        return service.getById(warehouseId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("仓库不存在"));
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

    @DeleteMapping("/{warehouseIds}")
    public Map<String, Object> remove(@PathVariable String warehouseIds) {
        try {
            int total = 0;
            for (String part : warehouseIds.split(",")) {
                total += service.delete(Long.parseLong(part.trim()));
            }
            return MesApiResponse.toAjax(total);
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }
}
