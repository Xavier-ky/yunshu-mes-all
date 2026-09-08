package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmRtIssueService;
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
@RequestMapping("/api/mes/wm/rtissue")
public class WmRtIssueController {

    private final WmRtIssueService service;

    public WmRtIssueController(WmRtIssueService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.listHeader(params), service.countHeader(params));
    }

    @GetMapping("/{rtId}")
    public Map<String, Object> getInfo(@PathVariable Long rtId) {
        Map<String, Object> row = service.getHeader(rtId);
        return row == null ? MesApiResponse.error("退料单不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(service.createHeader(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(service.updateHeader(body));
    }

    @PutMapping("/{rtId}")
    public Map<String, Object> execute(@PathVariable Long rtId) {
        try {
            service.execute(rtId);
            return MesApiResponse.ok();
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{rtIds}")
    public Map<String, Object> remove(@PathVariable String rtIds) {
        int n = 0;
        for (String p : rtIds.split(",")) {
            n += service.deleteHeader(Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
