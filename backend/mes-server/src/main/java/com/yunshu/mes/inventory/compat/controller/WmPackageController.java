package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmPackageService;
import com.yunshu.mes.planning.compat.MesApiResponse;
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
@RequestMapping("/api/mes/wm/package")
public class WmPackageController {

    private final WmPackageService service;

    public WmPackageController(WmPackageService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.list(params), service.count(params));
    }

    @GetMapping("/{packageId}")
    public Map<String, Object> getInfo(@PathVariable Long packageId) {
        Map<String, Object> row = service.getById(packageId);
        return row == null ? MesApiResponse.error("装箱单不存在") : MesApiResponse.ok(row);
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

    @PutMapping("/addsub")
    public Map<String, Object> addSubPackage(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.toAjax(service.addSubPackage(body));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{packageIds}")
    public Map<String, Object> remove(@PathVariable String packageIds) {
        try {
            int n = 0;
            for (String p : packageIds.split(",")) {
                n += service.delete(Long.parseLong(p.trim()));
            }
            return MesApiResponse.toAjax(n);
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }
}
