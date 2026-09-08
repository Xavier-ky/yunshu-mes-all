package com.yunshu.mes.production.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.equipment.compat.service.EquipmentBridgeService;
import com.yunshu.mes.production.compat.repository.ProAndonRecordRepository;
import com.yunshu.mes.production.compat.service.AndonBridgeService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/mes/pro/andonrecord")
public class AndonRecordController {

    private final ProAndonRecordRepository repo;
    private final AndonBridgeService bridge;
    private final EquipmentBridgeService equipmentBridge;

    public AndonRecordController(
            ProAndonRecordRepository repo,
            AndonBridgeService bridge,
            EquipmentBridgeService equipmentBridge) {
        this.repo = repo;
        this.bridge = bridge;
        this.equipmentBridge = equipmentBridge;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = repo.search(params, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, repo.count(params));
    }

    @GetMapping("/{recordId}")
    public Map<String, Object> getInfo(@PathVariable Long recordId) {
        return repo.findById(recordId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("安灯呼叫记录不存在"));
    }

    @PostMapping
    @Transactional
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Long id = repo.insert(body);
        if (id != null) {
            body.put("recordId", id);
            bridge.afterRecordCreated(id, body);
            equipmentBridge.afterAndonCreated(id, body);
        }
        return MesApiResponse.ok(id);
    }

    @PutMapping
    @Transactional
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        int rows = repo.update(body);
        if (rows > 0) {
            repo.findById(longVal(body.get("recordId"))).ifPresent(bridge::afterRecordUpdated);
        }
        return MesApiResponse.toAjax(rows);
    }

    @DeleteMapping("/{recordIds}")
    public Map<String, Object> remove(@PathVariable String recordIds) {
        List<Long> ids = Arrays.stream(recordIds.split(",")).map(Long::parseLong).toList();
        return MesApiResponse.toAjax(repo.deleteByIds(ids));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("导出功能待对接");
    }

    private static long longVal(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }
}
