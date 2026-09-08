package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmIssueService;
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
@RequestMapping("/api/mes/wm/issueline")
public class WmIssueLineController {

    private final WmIssueService service;

    public WmIssueLineController(WmIssueService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.listLine(params), service.countLine(params));
    }

    @GetMapping("/listWithDetail")
    public Map<String, Object> listWithDetail(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = service.listLineWithDetail(params);
        return MesApiResponse.table(rows, rows.size());
    }

    @GetMapping("/{lineId}")
    public Map<String, Object> getInfo(@PathVariable Long lineId) {
        Map<String, Object> row = service.getLine(lineId);
        return row == null ? MesApiResponse.error("行不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(service.createLine(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(service.updateLine(body));
    }

    @DeleteMapping("/{lineIds}")
    public Map<String, Object> remove(@PathVariable String lineIds) {
        int n = 0;
        for (String p : lineIds.split(",")) {
            n += service.deleteLine(Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
