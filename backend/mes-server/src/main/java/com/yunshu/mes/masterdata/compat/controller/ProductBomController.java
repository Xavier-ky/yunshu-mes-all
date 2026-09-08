package com.yunshu.mes.masterdata.compat.controller;

import com.yunshu.mes.masterdata.compat.service.ProductBomService;
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
@RequestMapping("/api/mes/md/bom")
public class ProductBomController {

    private final ProductBomService service;

    public ProductBomController(ProductBomService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.list(params), service.count(params));
    }

    @GetMapping("/{bomId}")
    public Map<String, Object> getInfo(@PathVariable Long bomId) {
        Map<String, Object> row = service.getById(bomId);
        return row == null ? MesApiResponse.error("BOM不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(service.create(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(service.update(body));
    }

    @DeleteMapping("/{bomIds}")
    public Map<String, Object> remove(@PathVariable String bomIds) {
        int n = 0;
        for (String p : bomIds.split(",")) {
            n += service.delete(Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
