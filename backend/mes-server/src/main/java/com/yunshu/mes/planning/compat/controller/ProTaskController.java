package com.yunshu.mes.planning.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.planning.compat.service.ProTaskService;
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
@RequestMapping("/api/mes/pro/protask")
public class ProTaskController {

    private final ProTaskService taskService;

    public ProTaskController(ProTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = taskService.list(params);
        return MesApiResponse.table(rows, rows.size());
    }

    @GetMapping("/listGanttTaskList")
    public Map<String, Object> listGanttTaskList(@RequestParam Map<String, String> params) {
        return MesApiResponse.ok(taskService.ganttPayload(params));
    }

    @GetMapping("/listTaskListByWorkorder")
    public Map<String, Object> listTaskListByWorkorder(@RequestParam Long workorderId) {
        List<Map<String, Object>> rows = taskService.listByWorkOrder(workorderId);
        return MesApiResponse.table(rows, rows.size());
    }

    @GetMapping("/{taskId}")
    public Map<String, Object> getInfo(@PathVariable Long taskId) {
        return taskService.getById(taskId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("任务不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Long id = taskService.create(body);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(taskService.update(body));
    }

    @DeleteMapping("/{taskIds}")
    public Map<String, Object> remove(@PathVariable String taskIds) {
        int total = 0;
        for (String part : taskIds.split(",")) {
            total += taskService.delete(Long.parseLong(part.trim()));
        }
        return MesApiResponse.toAjax(total);
    }
}
