package com.yunshu.mes.planning.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.planning.compat.service.WorkorderBomService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/pro/workorderbom")
public class WorkorderBomController {

    private final WorkorderBomService bomService;

    public WorkorderBomController(WorkorderBomService bomService) {
        this.bomService = bomService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Long workorderId) {
        List<Map<String, Object>> rows = bomService.listByWorkOrder(workorderId);
        return MesApiResponse.table(rows, rows.size());
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long lineId = ((Number) body.get("lineId")).longValue();
        bomService.update(lineId, body);
        return MesApiResponse.ok();
    }

    @DeleteMapping("/{lineIds}")
    public Map<String, Object> remove(@PathVariable String lineIds) {
        for (String part : lineIds.split(",")) {
            bomService.delete(Long.parseLong(part.trim()));
        }
        return MesApiResponse.ok();
    }
}
