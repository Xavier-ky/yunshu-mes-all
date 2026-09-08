package com.yunshu.mes.equipment.compat.controller;

import com.yunshu.mes.equipment.compat.repository.DvCheckRecordLineRepository;
import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.Arrays;
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
@RequestMapping("/api/mes/dv/checkrecordline")
public class DvCheckRecordLineController {

    private final DvCheckRecordLineRepository repo;

    public DvCheckRecordLineController(DvCheckRecordLineRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = repo.search(params, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, repo.count(params));
    }

    @GetMapping("/{lineId}")
    public Map<String, Object> getInfo(@PathVariable Long lineId) {
        return repo.findById(lineId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("点检行不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Long id = repo.insert(body);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(repo.update(body));
    }

    @DeleteMapping("/{lineIds}")
    public Map<String, Object> remove(@PathVariable String lineIds) {
        List<Long> ids = Arrays.stream(lineIds.split(",")).map(String::trim).map(Long::parseLong).toList();
        return MesApiResponse.toAjax(repo.deleteByIds(ids));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("待对接");
    }
}
