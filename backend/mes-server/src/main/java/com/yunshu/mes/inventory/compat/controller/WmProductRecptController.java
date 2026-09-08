package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmProductRecptService;
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
@RequestMapping("/api/mes/wm/productrecpt")
public class WmProductRecptController {

    private final WmProductRecptService service;

    public WmProductRecptController(WmProductRecptService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.listHeader(params), service.countHeader(params));
    }

    @GetMapping("/{recptId}")
    public Map<String, Object> getInfo(@PathVariable Long recptId) {
        Map<String, Object> row = service.getHeader(recptId);
        return row == null ? MesApiResponse.error("产品入库单不存在") : MesApiResponse.ok(row);
    }

    @GetMapping("/checkQuantity/{recptId}")
    public Map<String, Object> checkQuantity(@PathVariable Long recptId) {
        return MesApiResponse.ok(service.checkQuantity(recptId));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.ok(service.createHeader(body));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/fromWorkOrder/{workorderId}")
    public Map<String, Object> createFromWorkOrder(@PathVariable Long workorderId) {
        try {
            return MesApiResponse.ok(service.createFromWorkOrder(workorderId));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.toAjax(service.updateHeader(body));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{recptId}")
    public Map<String, Object> execute(@PathVariable Long recptId) {
        try {
            service.execute(recptId);
            return MesApiResponse.ok();
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{recptIds}")
    public Map<String, Object> remove(@PathVariable String recptIds) {
        try {
            int n = 0;
            for (String p : recptIds.split(",")) {
                n += service.deleteHeader(Long.parseLong(p.trim()));
            }
            return MesApiResponse.toAjax(n);
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }
}
