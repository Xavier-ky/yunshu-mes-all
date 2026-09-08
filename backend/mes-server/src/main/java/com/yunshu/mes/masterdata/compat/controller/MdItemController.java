package com.yunshu.mes.masterdata.compat.controller;

import com.yunshu.mes.masterdata.compat.service.MdItemService;
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
@RequestMapping("/api/mes/md/mditem")
public class MdItemController {

    private final MdItemService service;

    public MdItemController(MdItemService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.list(params), service.count(params));
    }

    @GetMapping("/{itemId}")
    public Map<String, Object> getInfo(@PathVariable Long itemId,
            @RequestParam(required = false) String itemOrProduct) {
        Map<String, Object> row = service.getById(itemId);
        return row == null ? MesApiResponse.error("物料不存在") : MesApiResponse.ok(row);
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

    @DeleteMapping("/{itemIds}")
    public Map<String, Object> remove(@PathVariable String itemIds) {
        int n = 0;
        for (String p : itemIds.split(",")) {
            n += service.delete(Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
