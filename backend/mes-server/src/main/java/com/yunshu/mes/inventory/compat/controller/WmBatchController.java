package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmBatchService;
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
@RequestMapping("/api/mes/wm/batch")
public class WmBatchController {

    private final WmBatchService batchService;

    public WmBatchController(WmBatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = batchService.list(params);
        return MesApiResponse.table(rows, batchService.count(params));
    }

    @GetMapping("/listForward")
    public Map<String, Object> listForward(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = batchService.listForward(params.get("batchCode"));
        return MesApiResponse.table(rows, rows.size());
    }

    @GetMapping("/listBackward")
    public Map<String, Object> listBackward(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = batchService.listBackward(params.get("batchCode"));
        return MesApiResponse.table(rows, rows.size());
    }

    @GetMapping("/{batchId}")
    public Map<String, Object> getInfo(@PathVariable Long batchId) {
        return batchService.getById(batchId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("批次不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.ok(batchService.create(body));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return MesApiResponse.error("新增批次失败: " + e.getMessage());
        }
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(batchService.update(body));
    }

    @DeleteMapping("/{batchIds}")
    public Map<String, Object> remove(@PathVariable String batchIds) {
        return MesApiResponse.toAjax(batchService.delete(batchIds));
    }
}
