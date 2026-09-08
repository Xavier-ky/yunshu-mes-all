package com.yunshu.mes.planning.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.process.compat.service.ProProcessService;
import java.util.LinkedHashMap;
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
@RequestMapping("/api/mes/pro/process")
public class CompatProcessController {

    private final ProProcessService service;

    public CompatProcessController(ProProcessService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.table(service.list(params), service.count(params));
    }

    @GetMapping("/listAll")
    public Map<String, Object> listAll() {
        return MesApiResponse.ok(service.list(new LinkedHashMap<>(Map.of("pageNum", "1", "pageSize", "1000"))));
    }

    @GetMapping("/{processId}")
    public Map<String, Object> getInfo(@PathVariable Long processId) {
        Map<String, Object> row = service.getById(processId);
        return row == null ? MesApiResponse.error("工序不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(service.create(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(service.update(body));
    }

    @DeleteMapping("/{processIds}")
    public Map<String, Object> remove(@PathVariable String processIds) {
        int n = 0;
        for (String p : processIds.split(",")) {
            n += service.delete(Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
