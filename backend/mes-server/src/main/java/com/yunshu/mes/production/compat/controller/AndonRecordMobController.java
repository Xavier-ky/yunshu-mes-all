package com.yunshu.mes.production.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.production.compat.repository.ProAndonConfigRepository;
import com.yunshu.mes.production.compat.repository.ProAndonRecordRepository;
import com.yunshu.mes.production.compat.service.AndonBridgeService;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/pro/andonrecord")
public class AndonRecordMobController {

    private final ProAndonRecordRepository recordRepo;
    private final ProAndonConfigRepository configRepo;
    private final AndonBridgeService bridge;

    public AndonRecordMobController(ProAndonRecordRepository recordRepo,
            ProAndonConfigRepository configRepo,
            AndonBridgeService bridge) {
        this.recordRepo = recordRepo;
        this.configRepo = configRepo;
        this.bridge = bridge;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = recordRepo.search(params, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, recordRepo.count(params));
    }

    @GetMapping("/listReasons")
    public Map<String, Object> listReasons() {
        return MesApiResponse.ok(configRepo.listAll());
    }

    @GetMapping("/{recordId}")
    public Map<String, Object> getInfo(@PathVariable Long recordId) {
        return recordRepo.findById(recordId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("安灯呼叫记录不存在"));
    }

    @PostMapping
    @Transactional
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Long id = recordRepo.insert(body);
        if (id != null) {
            body.put("recordId", id);
            bridge.afterRecordCreated(id, body);
            return MesApiResponse.ok(recordRepo.findById(id).orElse(body));
        }
        return MesApiResponse.error("创建失败");
    }

    @PutMapping
    @Transactional
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        int rows = recordRepo.update(body);
        if (rows > 0) {
            recordRepo.findById(((Number) body.get("recordId")).longValue()).ifPresent(bridge::afterRecordUpdated);
        }
        return MesApiResponse.toAjax(rows);
    }
}
