package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmIssueService;
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
@RequestMapping("/api/mes/wm/issuedetail")
public class WmIssueDetailController {

    private final WmIssueService service;

    public WmIssueDetailController(WmIssueService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.listDetail(params), service.countDetail(params));
    }

    @GetMapping("/{detailId}")
    public Map<String, Object> getInfo(@PathVariable Long detailId) {
        Map<String, Object> row = service.getDetail(detailId);
        return row == null ? MesApiResponse.error("明细不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(service.createDetail(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(service.updateDetail(body));
    }

    @DeleteMapping("/{detailIds}")
    public Map<String, Object> remove(@PathVariable String detailIds) {
        int n = 0;
        for (String p : detailIds.split(",")) {
            n += service.deleteDetail(Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
