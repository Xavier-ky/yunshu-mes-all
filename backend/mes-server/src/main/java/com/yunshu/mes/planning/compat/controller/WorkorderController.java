package com.yunshu.mes.planning.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.planning.compat.service.WorkorderService;
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
@RequestMapping("/api/mes/pro/workorder")
public class WorkorderController {

    private final WorkorderService workorderService;

    public WorkorderController(WorkorderService workorderService) {
        this.workorderService = workorderService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        String status = params.get("status");
        List<Map<String, Object>> rows = workorderService.list(params, status);
        long total = workorderService.count(params, status);
        return MesApiResponse.table(rows, total);
    }

    @GetMapping("/listWithTaskJson")
    public Map<String, Object> listWithTaskJson(@RequestParam Map<String, String> params) {
        String status = params.get("status");
        List<Map<String, Object>> rows = workorderService.listWithTaskJson(params, status);
        long total = workorderService.count(params, status);
        return MesApiResponse.table(rows, total);
    }

    @GetMapping("/{workorderId}")
    public Map<String, Object> getInfo(@PathVariable Long workorderId) {
        return workorderService.getById(workorderId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("工单不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        try {
            Long id = workorderService.create(body);
            return MesApiResponse.ok(id);
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.toAjax(workorderService.update(body));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{workorderIds}")
    public Map<String, Object> remove(@PathVariable String workorderIds) {
        try {
            int total = 0;
            for (String part : workorderIds.split(",")) {
                total += workorderService.delete(Long.parseLong(part.trim()));
            }
            return MesApiResponse.toAjax(total);
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/listItems")
    public Map<String, Object> listItems(@RequestParam Map<String, String> params) {
        Long workorderId = Long.parseLong(params.get("workorderId"));
        List<Map<String, Object>> rows = workorderService.listMaterialRequirements(workorderId);
        return MesApiResponse.table(rows, rows.size());
    }

    @PutMapping("/finish/{workorderId}")
    public Map<String, Object> finish(@PathVariable Long workorderId) {
        return MesApiResponse.toAjax(workorderService.finish(workorderId));
    }

    @PutMapping("/cancel/{workorderId}")
    public Map<String, Object> cancel(@PathVariable Long workorderId) {
        return MesApiResponse.toAjax(workorderService.cancel(workorderId));
    }

    @GetMapping("/getHomeList")
    public Map<String, Object> getHomeList(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = workorderService.getHomeList(params);
        long total = rows.size();
        return MesApiResponse.ok(rows);
    }
}
